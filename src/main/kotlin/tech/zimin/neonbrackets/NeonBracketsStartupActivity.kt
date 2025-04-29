package tech.zimin.neonbrackets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import tech.zimin.neonbrackets.listeners.NeonBracketsDocumentListener
import tech.zimin.neonbrackets.listeners.NeonBracketsSelectionListener

/**
 * Startup activity that runs when the IDE starts.
 * This ensures that the plugin is initialized immediately after installation.
 */
class NeonBracketsStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        println("[NeonBrackets] Startup activity running for project: ${project.name}")

        // Process all open editors immediately
        ApplicationManager.getApplication().invokeLater {
            processAllOpenEditors()
        }
    }

    private fun processAllOpenEditors() {
        // Get all open editors and apply highlighting
        val editorFactory = EditorFactory.getInstance()
        val editors = editorFactory.allEditors

        println("[NeonBrackets] Startup activity processing ${editors.size} existing editors")

        for (editor in editors) {
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: continue

            if (!file.fileType.isBinary) {
                println("[NeonBrackets] Startup activity processing editor for file: ${file.name}")

                // Remove any existing highlighting
                val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
                existingHighlighters?.forEach {
                    try {
                        it.dispose()
                    } catch (e: Exception) {
                        println("[NeonBrackets] Error disposing highlighter: ${e.message}")
                    }
                }

                // Add document listener if not already added
                if (editor.getUserData(DOCUMENT_LISTENER) == null) {
                    val docListener = NeonBracketsDocumentListener(editor)
                    editor.putUserData(DOCUMENT_LISTENER, docListener)
                    editor.document.addDocumentListener(docListener)
                }

                // Add selection listener if not already added
                if (editor.getUserData(SELECTION_LISTENER) == null) {
                    val selectionListener = NeonBracketsSelectionListener(editor)
                    editor.putUserData(SELECTION_LISTENER, selectionListener)
                    editor.selectionModel.addSelectionListener(selectionListener)
                }

                // Apply highlighting
                highlightBracketsInEditor(editor)
            }
        }
    }
}