package org.goodmath.simplex.lsp

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.services.TextDocumentService

class SimplexDocumentService : TextDocumentService {
    val openDocuments = HashMap<String, TextDocumentItem>()

    override fun didOpen(openParams: DidOpenTextDocumentParams) {
        if (openParams.textDocument.languageId == "Simplex") {
            this.openDocuments[openParams.textDocument.uri] = openParams.textDocument
        }
    }

    override fun didChange(change: DidChangeTextDocumentParams) {
        val changedFile = change.textDocument.uri
        if (openDocuments.containsKey(changedFile)) {
            val doc = openDocuments.get(changedFile)!!
            for (ch in change.contentChanges) {}
        }
    }

    override fun didClose(closeParams: DidCloseTextDocumentParams) {
        if (openDocuments.containsKey(closeParams.textDocument.uri)) {
            openDocuments.remove(closeParams.textDocument.uri)
        }
    }

    override fun didSave(p0: DidSaveTextDocumentParams?) {
        TODO("Not yet implemented")
    }
}
