package com.github.okafke.poryscriptidea.lsp

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

interface PsLanguageClientApi: LanguageClient {
    @JsonRequest("poryscript/readfile")
    fun readfile(file: String): CompletableFuture<String>

    @JsonRequest("poryscript/readfs")
    fun readfs(file: String): CompletableFuture<String>

    @JsonRequest("poryscript/getPoryscriptFiles")
    fun getPoryscriptFiles(): CompletableFuture<List<String>>

    @JsonRequest("poryscript/getfileuri")
    fun getFileUri(file: String): CompletableFuture<String>

}