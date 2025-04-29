package tech.zimin.neonBrackets.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import tech.zimin.neonBrackets.highlightBracketsInEditor

/**
 * Selection listener that updates bracket highlighting when the cursor moves.
 */
class NeonBracketsSelectionListener(private val editor: Editor) : SelectionListener {
    override fun selectionChanged(e: SelectionEvent) {
        // Only update if cursor moved (not if text was selected)
        if (e.newRange.isEmpty && e.oldRange.isEmpty) {
            ApplicationManager.getApplication().invokeLater {
                // Don't force rehighlight on cursor movement
                highlightBracketsInEditor(editor)
            }
        }
    }
}