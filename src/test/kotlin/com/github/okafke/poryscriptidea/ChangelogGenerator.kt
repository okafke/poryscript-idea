package com.github.okafke.poryscriptidea

import org.junit.Test

class ChangelogGenerator {
    // actually org.jetbrains.intellij.platform is also just a gradle plugin, but I think we can keep that
    private val excludedDependencies = setOf("org.jetbrains.qodana", "org.jetbrains.changelog")
    private val warnings = arrayListOf<String>()

    fun printCommits(commits: List<String>) {
        if (commits.isEmpty()) {
            println("-")
        }

        for (commit in commits) {
            val cleanedCommit = commit.replace(Regex("""\s+\(#\d+\)"""), "")
            println("- $cleanedCommit")
        }

        println()
    }

    fun filterDependencyBumps(deps: List<String>): List<String> {
        val bumpRegex = Regex("""fix\(deps\): bump ([\w.\-]+) from ([\w.\-]+) to ([\w.\-]+)""")
        val considered = mutableSetOf<String>()
        val result = arrayListOf<String>()
        deps.forEach { commit ->
            val match = bumpRegex.find(commit)
            if (match != null) {
                val dependency = match.groupValues[1]
                if (!excludedDependencies.contains(dependency) && considered.add(dependency)) {
                    result.add(commit)
                }
            } else {
                warnings.add(commit)
                result.add(commit)
            }
        }

        return result
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
        val fix = arrayListOf<String>()
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
        printCommits(filterDependencyBumps(deps))

        if (warnings.isNotEmpty()) {
            println("Warnings:")
            println(warnings.joinToString("\n"))
        }
    }

}