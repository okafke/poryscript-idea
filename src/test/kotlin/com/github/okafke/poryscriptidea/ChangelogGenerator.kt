package com.github.okafke.poryscriptidea

import org.junit.Test

class ChangelogGenerator {
    fun printCommits(commits: List<String>) {
        if (commits.isEmpty()) {
            println("-")
        }

        for (commit in commits) {
            // remove (#<pull-req id), e.g. (#33) from the end
            val cleanedCommit = commit.replace(Regex("""\s+\(#\d+\)"""),"")
            println("- $cleanedCommit")
        }

        println()
    }

    @Test
    fun generateChangelog() {
        val tag = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()

        val commits = ProcessBuilder("git", "log", "$tag..HEAD", "--pretty=format:%s")
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readLines()

        val feat = arrayListOf<String>()
        val fix  = arrayListOf<String>()
        val deps = arrayListOf<String>()
        for (commit in commits) {
            if (commit.startsWith("fix(deps):")) {
                deps.add(commit)
            } else if (commit.startsWith("feat")) {
                feat.add(commit)
            } else if (commit.startsWith("fix")) {
                fix.add(commit)
            }
        }

        println()
        println("## [Unreleased]")
        println()
        println("### Added")
        printCommits(feat)

        println("### Fixed")
        printCommits(fix)

        println("### Dependencies")
        printCommits(deps)
    }

}