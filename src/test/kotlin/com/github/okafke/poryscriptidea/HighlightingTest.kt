package com.github.okafke.poryscriptidea

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil

// we have disabled this test for now
// there seem to be issues with LSP4IJ and virtual files
/*
class HighlightingTest : BasePlatformTestCase() {
    fun testPoryscriptFile() {
        /*val code = """
            script Test {
                end
            }
        """.trimIndent()

        val file = myFixture.configureByText("scripts.pory", code)
        assertFalse(PsiErrorElementUtil.hasErrors(project, file.virtualFile))

        EditorTestUtil.testFileSyntaxHighlighting(file, true, "")
        LSP4IJ crashes on Virtual temp File, cannot run this :(
         */
    }

    fun testProjectService() {
        //val projectService = project.service<PsSettings>()
        //assertTrue(projectService.state.semanticTokenHighlighting)
        // failes with indexing timeout on CI??
    }

}*/
