package org.goodmath.simplex.lsp

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class SimplexLS:  LanguageServer {
    override fun initialize(p0: InitializeParams?): CompletableFuture<InitializeResult?>? {
        TODO("Not yet implemented")
    }

    override fun shutdown(): CompletableFuture<in Any>? {
        TODO("Not yet implemented")
    }

    override fun exit() {
        TODO("Not yet implemented")
    }

    override fun getTextDocumentService(): TextDocumentService? {
        TODO("Not yet implemented")
    }

    override fun getWorkspaceService(): WorkspaceService? {
        TODO("Not yet implemented")
    }
}
