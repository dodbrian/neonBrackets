package tech.zimin.neonBrackets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import tech.zimin.neonBrackets.listeners.NeonBracketsDocumentListener
import tech.zimin.neonBrackets.listeners.NeonBracketsSelectionListener

/**
 * Application component that initializes the plugin when it's loaded.
 */
class NeonBracketsApplicationInitializer {
    companion object {
        /**
         * Initialize the plugin when it's loaded.
         */
        fun initialize() {
            println("[NeonBrackets] Plugin initialized")

            // Process existing editors when the plugin is loaded
            ApplicationManager.getApplication().invokeLater {
                processAllOpenEditors()
            }
        }

        private fun processAllOpenEditors() {
            // Get all open editors and apply highlighting
            val editorFactory = EditorFactory.getInstance()
            val editors = editorFactory.allEditors

            println("[NeonBrackets] Processing ${editors.size} existing editors on plugin load")

            for (editor in editors) {
                val file = FileDocumentManager.getInstance().getFile(editor.document) ?: continue

                if (!file.fileType.isBinary) {
                    println("[NeonBrackets] Processing existing editor for file: ${file.name}")

                    // Add document listener
                    val docListener = NeonBracketsDocumentListener(editor)
                    editor.putUserData(DOCUMENT_LISTENER, docListener)
                    editor.document.addDocumentListener(docListener)

                    // Add selection listener
                    val selectionListener = NeonBracketsSelectionListener(editor)
                    editor.putUserData(SELECTION_LISTENER, selectionListener)
                    editor.selectionModel.addSelectionListener(selectionListener)

                    // Apply highlighting
                    highlightBracketsInEditor(editor)
                }
            }
        }
    }
}