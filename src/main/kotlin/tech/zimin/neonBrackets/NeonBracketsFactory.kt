package tech.zimin.neonBrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import tech.zimin.neonBrackets.settings.NeonBracketsState

/**
 * Factory for creating bracket highlighting passes.
 */
@State(
    name = "NeonBracketsSettings",
    storages = [Storage("neonBrackets.xml")]
)
class NeonBracketsFactory : TextEditorHighlightingPassFactoryRegistrar, PersistentStateComponent<NeonBracketsState> {
    private var state = NeonBracketsState()

    override fun getState(): NeonBracketsState = state

    override fun loadState(state: NeonBracketsState) {
        this.state = state
    }

    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        println("[NeonBrackets] Registering highlighting pass factory for project: ${project.name}")

        registrar.registerTextEditorHighlightingPass(
            TextEditorHighlightingPassFactory { file, editor ->
                if (!state.enabled || editor.getUserData(SKIP_BRACKET_HIGHLIGHTING) == true) {
                    return@TextEditorHighlightingPassFactory null
                }

                println("[NeonBrackets] Creating highlighting pass for file: ${file.name}")
                NeonBracketsPass(file, editor)
            },
            null,
            null,
            false,
            -1
        )
    }

    companion object {
        private val instance = NeonBracketsFactory()

        fun getInstance(): NeonBracketsFactory {
            return instance
        }
    }
}