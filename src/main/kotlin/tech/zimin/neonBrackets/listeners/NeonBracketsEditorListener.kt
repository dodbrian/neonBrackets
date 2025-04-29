package tech.zimin.neonBrackets.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import tech.zimin.neonBrackets.BRACKET_HIGHLIGHTERS
import tech.zimin.neonBrackets.DOCUMENT_LISTENER
import tech.zimin.neonBrackets.SELECTION_LISTENER
import tech.zimin.neonBrackets.SKIP_BRACKET_HIGHLIGHTING
import tech.zimin.neonBrackets.getIdeProductName
import tech.zimin.neonBrackets.highlightBracketsInEditor

/**
 * Editor factory listener that tracks when editors are created and released.
 */
class NeonBracketsEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor

        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        val fileType = file.fileType.name
        println("[NeonBrackets] Editor created for file: ${file.name}, file type: $fileType")

        if (file.fileType.isBinary) {
            println("[NeonBrackets] Skipping bracket highlighting for binary file: ${file.name}")
            editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, true)
            return
        }

        // Log IDE product name
        val productName = getIdeProductName()
        println("[NeonBrackets] Running in IDE: $productName")

        // Explicitly set to false to ensure they're processed
        editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, false)

        // Store the document listener so we can remove it later
        val docListener = NeonBracketsDocumentListener(editor)
        editor.putUserData(DOCUMENT_LISTENER, docListener)

        // Add document listener to update highlighting when document changes
        editor.document.addDocumentListener(docListener)

        // Store the selection listener so we can remove it later
        val selectionListener = NeonBracketsSelectionListener(editor)
        editor.putUserData(SELECTION_LISTENER, selectionListener)

        // Add selection listener to update highlighting when cursor moves
        editor.selectionModel.addSelectionListener(selectionListener)

        // Apply highlighting immediately
        ApplicationManager.getApplication().invokeLater {
            highlightBracketsInEditor(editor)
        }

        println("[NeonBrackets] Added document listener and applied highlighting for file: ${file.name}")
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor

        // Remove document listener
        val listener = editor.getUserData(DOCUMENT_LISTENER)
        if (listener != null) {
            editor.document.removeDocumentListener(listener)
            editor.putUserData(DOCUMENT_LISTENER, null)
        }

        // Remove selection listener
        val selectionListener = editor.getUserData(SELECTION_LISTENER)
        if (selectionListener != null) {
            editor.selectionModel.removeSelectionListener(selectionListener)
            editor.putUserData(SELECTION_LISTENER, null)
        }

        // Clean up any existing highlighters
        val highlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
        highlighters?.forEach {
            try {
                it.dispose()
            } catch (e: Exception) {
                println("[NeonBrackets] Error disposing highlighter: ${e.message}")
            }
        }

        editor.putUserData(BRACKET_HIGHLIGHTERS, null)
        editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, null)
    }
}