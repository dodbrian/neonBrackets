package tech.zimin.neonbrackets.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import tech.zimin.neonbrackets.highlightBracketsInEditor

/**
 * Document listener that updates bracket highlighting when the document changes.
 */
class NeonBracketsDocumentListener(private val editor: Editor) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        println("[NeonBrackets] Document changed, updating bracket highlighting")

        // Use invokeLater to avoid UI freezes during typing
        ApplicationManager.getApplication().invokeLater {
            highlightBracketsInEditor(editor) // Force full rehighlight on document change
        }
    }
}