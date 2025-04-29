package tech.zimin.neonbrackets

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.DumbAware

/**
 * Action to toggle Neon Brackets highlighting.
 */
class ToggleNeonBracketsAction : ToggleAction("Toggle Neon Brackets"), DumbAware {
    override fun isSelected(e: AnActionEvent): Boolean {
        return NeonBracketsFactory.getInstance().state.enabled
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        // Update the state
        val settings = NeonBracketsFactory.getInstance().state
        settings.enabled = state

        // Force rehighlight all open editors when toggling
        ApplicationManager.getApplication().invokeLater {
            val editors = EditorFactory.getInstance().allEditors

            for (editor in editors) {
                try {
                    // Always force rehighlight when toggling
                    highlightBracketsInEditor(editor)
                } catch (_: Exception) {
                    // Silent exception handling
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}