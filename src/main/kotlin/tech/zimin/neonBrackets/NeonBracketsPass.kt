package tech.zimin.neonBrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiFile

/**
 * Highlighting pass that colorizes matching brackets.
 */
class NeonBracketsPass(
    file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, true) {
    override fun doCollectInformation(progress: ProgressIndicator) {
        // Directly highlight brackets instead of collecting information
        highlightBracketsInEditor(editor)
    }

    override fun doApplyInformationToEditor() {
        // Nothing to do here as we've already applied the highlighting in doCollectInformation
    }
}