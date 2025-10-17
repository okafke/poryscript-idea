package com.github.okafke.poryscriptidea.lsp

import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LSPFileSupport
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.client.ExecuteLSPFeatureStatus
import com.redhat.devtools.lsp4ij.client.indexing.ProjectIndexingManager
import com.redhat.devtools.lsp4ij.features.semanticTokens.LazyHighlightInfo
import com.redhat.devtools.lsp4ij.features.semanticTokens.SemanticTokensData
import com.redhat.devtools.lsp4ij.internal.CompletableFutures
import com.redhat.devtools.lsp4ij.internal.PsiFileChangedException
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class PsHighlightVisitor: HighlightVisitor {
    val LOGGER: Logger = LoggerFactory.getLogger(PsHighlightVisitor::class.java)
    private var semanticTokens: SemanticTokensData? = null
    private var holder: HighlightInfoHolder? = null
    private var lazyInfos: Array<LazyHighlightInfo?>? = null

    override fun suitableForFile(file: PsiFile): Boolean {
        return LanguageServersRegistry.getInstance().isFileSupported(file)
                && file.virtualFile.extension.equals("pory", true)
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        if (ProjectIndexingManager.canExecuteLSPFeature(file) != ExecuteLSPFeatureStatus.NOW) {
            return true
        } else {
            try {
                this.lazyInfos = null
                this.holder = null
                this.semanticTokens = getSemanticTokens(file)
                if (this.semanticTokens != null) {
                    if (!this.semanticTokens!!.shouldVisitPsiElement(file)) {
                        highlightSemanticTokens(file, this.semanticTokens!!, holder)
                        this.lazyInfos = null
                        this.holder = null
                    } else {
                        this.lazyInfos = highlightSemanticTokens(file, this.semanticTokens!!, null as HighlightInfoHolder?)
                        this.holder = holder
                    }
                }

                action.run()
            } finally {
                this.holder = null
                this.lazyInfos = null
            }

            return true
        }
    }

    override fun visit(element: PsiElement) {
        if (this.semanticTokens != null
            && this.lazyInfos != null
            && this.semanticTokens!!.isEligibleForSemanticHighlighting(element)
        ) {
            val start = element.textOffset
            if (start >= 0) {
                val end = start + element.textLength

                var i = start
                while (i < end && i < this.lazyInfos!!.size) {
                    val info = this.lazyInfos!![i]
                    if (info != null) {
                        this.holder!!.add(info.resolve(i))
                        this.lazyInfos!![i] = null
                    }
                    ++i
                }
            }
        }
    }

    override fun clone(): HighlightVisitor {
        return PsHighlightVisitor()
    }

    companion object {
        public val THIS_CONTEXT: ThreadLocal<Boolean> = ThreadLocal()
    }

    private fun getSemanticTokens(file: PsiFile): SemanticTokensData? {
        val semanticTokensSupport = LSPFileSupport.getSupport(file).getSemanticTokensSupport()
        val params = SemanticTokensParams(TextDocumentIdentifier())
        val semanticTokensFuture = semanticTokensSupport.getSemanticTokens(params)

        try {
            CompletableFutures.waitUntilDone(semanticTokensFuture, file)
        } catch (_: PsiFileChangedException) {
            semanticTokensSupport.cancel()
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExecutionException) {
            LOGGER.error("Error while consuming LSP 'textDocument/semanticTokens/full' request", e)
            return null
        }

        return if (CompletableFutures.isDoneNormally(semanticTokensFuture)) semanticTokensFuture!!.getNow(null) else null
    }

    private fun highlightSemanticTokens(
        file: PsiFile,
        semanticTokens: SemanticTokensData,
        holder: HighlightInfoHolder?
    ): Array<LazyHighlightInfo?>? {
        val document = LSPIJUtils.getDocument(file.virtualFile)
        if (document == null) {
            return null
        } else if (holder != null) {
            try {
                THIS_CONTEXT.set(true)
                semanticTokens.highlight(
                    file,
                    document
                ) { start: Int, end: Int, colorKey: TextAttributesKey? ->
                    holder.add(LazyHighlightInfo.resolve(start, end, colorKey!!))
                }
            } finally {
                THIS_CONTEXT.set(null)
            }

            return null
        } else {
            val infos = arrayOfNulls<LazyHighlightInfo>(document.textLength)
            semanticTokens.highlight(
                file,
                document
            ) { start: Int, end: Int, colorKey: TextAttributesKey? ->
                infos[start] = LazyHighlightInfo(end, colorKey)
            }
            return infos
        }
    }

}