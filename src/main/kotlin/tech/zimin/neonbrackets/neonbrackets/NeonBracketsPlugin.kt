package tech.zimin.neonbrackets.neonbrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import java.awt.Color
import java.awt.Font

/**
 * Project listener that registers the highlighting pass factory when a project is opened.
 */
class NeonBracketsInitializer : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        println("[NeonBrackets] Project opened: ${project.name}")
        
        // Get the highlighting pass registrar for the project
        val registrar = TextEditorHighlightingPassRegistrar.getInstance(project)
        
        // Get or create the factory
        val factory = NeonBracketsFactory.getInstance()
        
        // Register the highlighting pass factory
        factory.registerHighlightingPassFactory(registrar, project)
        
        println("[NeonBrackets] Highlighting pass factory registered for project: ${project.name}")
        
        // Process existing editors
        processExistingEditors()
    }
    
    private fun processExistingEditors() {
        // Get all open editors and apply highlighting
        val editorFactory = EditorFactory.getInstance()
        val editors = editorFactory.allEditors
        
        println("[NeonBrackets] Processing ${editors.size} existing editors")
        
        for (editor in editors) {
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: continue
            
            if (!file.fileType.isBinary) {
                println("[NeonBrackets] Processing existing editor for file: ${file.name}")
                
                // Add document listener
                editor.document.addDocumentListener(NeonBracketsDocumentListener(editor))
                
                // Apply highlighting
                NeonBracketsEditorListener.highlightBracketsInEditor(editor)
            }
        }
    }
}

/**
 * Document listener that updates bracket highlighting when the document changes.
 */
class NeonBracketsDocumentListener(private val editor: Editor) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        println("[NeonBrackets] Document changed, updating bracket highlighting")
        
        // Use invokeLater to avoid UI freezes during typing
        ApplicationManager.getApplication().invokeLater {
            NeonBracketsEditorListener.highlightBracketsInEditor(editor)
        }
    }
}

/**
 * Editor factory listener that tracks when editors are created and released.
 */
class NeonBracketsEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        println("[NeonBrackets] Editor created for file: ${file.name}, file type: ${file.fileType.name}")
        
        if (file.fileType.isBinary) {
            println("[NeonBrackets] Skipping bracket highlighting for binary file: ${file.name}")
            editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, true)
            return
        }
        
        // Check if we're in Rider
        val isRider = isRiderIde()
        if (isRider) {
            println("[NeonBrackets] Running in Rider IDE")
        }
        
        // Explicitly set to false to ensure they're processed
        editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, false)
        
        // Store the document listener so we can remove it later
        val docListener = NeonBracketsDocumentListener(editor)
        editor.putUserData(DOCUMENT_LISTENER, docListener)
        
        // Add document listener to update highlighting when document changes
        editor.document.addDocumentListener(docListener)
        
        // Directly highlight brackets when editor is created
        highlightBracketsInEditor(editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        
        // Remove document listener
        val listener = editor.getUserData(DOCUMENT_LISTENER)
        if (listener != null) {
            editor.document.removeDocumentListener(listener)
            editor.putUserData(DOCUMENT_LISTENER, null)
        }
        
        // Clean up any existing highlighters
        val highlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
        highlighters?.forEach { it.dispose() }
        
        editor.putUserData(BRACKET_HIGHLIGHTERS, null)
        editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, null)
    }

    companion object {
        val BRACKET_HIGHLIGHTERS = Key<List<RangeHighlighter>>("NEON_BRACKET_HIGHLIGHTERS")
        val SKIP_BRACKET_HIGHLIGHTING = Key<Boolean>("NEON_SKIP_BRACKET_HIGHLIGHTING")
        val DOCUMENT_LISTENER = Key<DocumentListener>("NEON_DOCUMENT_LISTENER")
        
        private val BRACKET_COLORS = listOf(
            Color(255, 0, 0),      // Red
            Color(0, 200, 0),      // Green
            Color(0, 0, 255),      // Blue
            Color(255, 165, 0),    // Orange
            Color(255, 0, 255),    // Magenta
            Color(0, 255, 255)     // Cyan
        )
        
        private val bracketPairs = listOf(
            Pair('(', ')'),
            Pair('{', '}'),
            Pair('[', ']')
        )
        
        /**
         * Check if we're running in Rider IDE
         */
        fun isRiderIde(): Boolean {
            return PluginManagerCore.isPluginInstalled(PluginId.getId("com.intellij.modules.rider"))
        }
        
        /**
         * Directly highlight brackets in the editor without using a highlighting pass.
         */
        fun highlightBracketsInEditor(editor: Editor) {
            try {
                println("[NeonBrackets] Directly highlighting brackets in editor")
                
                // Remove any existing highlighters
                val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
                existingHighlighters.forEach { it.dispose() }
                
                val text = editor.document.charsSequence
                val stacks = mutableMapOf<Char, MutableList<Int>>()
                val bracketMatches = mutableListOf<Triple<Int, Int, Int>>() // openPos, closePos, nestingLevel
                
                // Initialize stacks for each bracket type
                bracketPairs.forEach { (open, _) -> 
                    stacks[open] = mutableListOf() 
                }
                
                // Find matching brackets
                for (i in text.indices) {
                    val char = text[i]
                    
                    for ((open, close) in bracketPairs) {
                        if (char == open) {
                            // Found opening bracket
                            stacks[open]?.add(i)
                        } else if (char == close && stacks[open]?.isNotEmpty() == true) {
                            // Found closing bracket with matching opening bracket
                            val openPos = stacks[open]?.removeAt(stacks[open]?.size!! - 1) ?: continue
                            val level = stacks[open]?.size ?: 0
                            bracketMatches.add(Triple(openPos, i, level))
                        }
                    }
                }
                
                println("[NeonBrackets] Found ${bracketMatches.size} bracket pairs")
                
                // Apply highlighters
                val newHighlighters = mutableListOf<RangeHighlighter>()
                val markupModel = editor.markupModel
                
                for ((openPos, closePos, level) in bracketMatches) {
                    val colorIndex = level % BRACKET_COLORS.size
                    val color = BRACKET_COLORS[colorIndex]
                    
                    val attributes = TextAttributes().apply {
                        foregroundColor = color
                        fontType = Font.BOLD
                    }
                    
                    try {
                        // Highlight opening bracket
                        val openHighlighter = markupModel.addRangeHighlighter(
                            openPos, openPos + 1,
                            HighlighterLayer.SELECTION + 100, // Use a very high layer
                            attributes,
                            HighlighterTargetArea.EXACT_RANGE
                        )
                        
                        // Highlight closing bracket
                        val closeHighlighter = markupModel.addRangeHighlighter(
                            closePos, closePos + 1,
                            HighlighterLayer.SELECTION + 100,
                            attributes,
                            HighlighterTargetArea.EXACT_RANGE
                        )
                        
                        newHighlighters.add(openHighlighter)
                        newHighlighters.add(closeHighlighter)
                    } catch (e: Exception) {
                        println("[NeonBrackets] Error adding highlighter: ${e.message}")
                    }
                }
                
                editor.putUserData(BRACKET_HIGHLIGHTERS, newHighlighters)
                println("[NeonBrackets] Applied ${newHighlighters.size} highlighters")
            } catch (e: Exception) {
                println("[NeonBrackets] Error highlighting brackets: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

/**
 * Settings state for the plugin.
 */
data class NeonBracketsState(
    var enabled: Boolean = true
)

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
            object : TextEditorHighlightingPassFactory {
                override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
                    if (!state.enabled || editor.getUserData(NeonBracketsEditorListener.SKIP_BRACKET_HIGHLIGHTING) == true) {
                        return null
                    }
                    
                    println("[NeonBrackets] Creating highlighting pass for file: ${file.name}")
                    return NeonBracketsPass(file, editor)
                }
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

/**
 * Highlighting pass that colorizes matching brackets.
 */
class NeonBracketsPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, true) {
    override fun doCollectInformation(progress: ProgressIndicator) {
        // Directly highlight brackets instead of collecting information
        NeonBracketsEditorListener.highlightBracketsInEditor(editor)
    }

    override fun doApplyInformationToEditor() {
        // Nothing to do here as we've already applied the highlighting in doCollectInformation
    }
}
