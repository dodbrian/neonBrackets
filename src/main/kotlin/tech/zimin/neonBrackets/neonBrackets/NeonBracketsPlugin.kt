package tech.zimin.neonBrackets.neonBrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
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
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import com.intellij.psi.TokenType
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.ui.JBColor
import java.awt.Font

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
                highlightBracketsInEditor(editor, true)
            }
        }
    }
}

/**
 * Project listener that registers the highlighting pass factory when a project is opened.
 */
class NeonBracketsInitializer : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {
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
                val docListener = NeonBracketsDocumentListener(editor)
                editor.putUserData(DOCUMENT_LISTENER, docListener)
                editor.document.addDocumentListener(docListener)

                // Add selection listener
                val selectionListener = NeonBracketsSelectionListener(editor)
                editor.putUserData(SELECTION_LISTENER, selectionListener)
                editor.selectionModel.addSelectionListener(selectionListener)

                // Apply highlighting
                highlightBracketsInEditor(editor, true)
            }
        }
    }
}

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
                    highlightBracketsInEditor(editor, true)
                }
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
            highlightBracketsInEditor(editor, true) // Force full rehighlight on document change
        }
    }
}

/**
 * Selection listener that updates bracket highlighting when the cursor moves.
 */
class NeonBracketsSelectionListener(private val editor: Editor) : SelectionListener {
    override fun selectionChanged(e: SelectionEvent) {
        // Only update if cursor moved (not if text was selected)
        if (e.newRange.isEmpty && e.oldRange.isEmpty) {
            ApplicationManager.getApplication().invokeLater {
                // Don't force rehighlight on cursor movement
                highlightBracketsInEditor(editor, false)
            }
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
            highlightBracketsInEditor(editor, true)
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
                    if (!state.enabled || editor.getUserData(SKIP_BRACKET_HIGHLIGHTING) == true) {
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
        highlightBracketsInEditor(editor, false)
    }

    override fun doApplyInformationToEditor() {
        // Nothing to do here as we've already applied the highlighting in doCollectInformation
    }
}

val BRACKET_HIGHLIGHTERS = Key<List<RangeHighlighter>>("NEON_BRACKET_HIGHLIGHTERS")
val SKIP_BRACKET_HIGHLIGHTING = Key<Boolean>("NEON_SKIP_BRACKET_HIGHLIGHTING")
val DOCUMENT_LISTENER = Key<DocumentListener>("NEON_DOCUMENT_LISTENER")
val SELECTION_LISTENER = Key<SelectionListener>("NEON_SELECTION_LISTENER")

// More vibrant and distinct colors for better visibility of neighboring brackets
private val BRACKET_COLORS = listOf(
    JBColor(java.awt.Color(255, 105, 180), java.awt.Color(220, 90, 150)),  // Hot Pink
    JBColor(java.awt.Color(65, 105, 225), java.awt.Color(55, 90, 190)),    // Royal Blue
    JBColor(java.awt.Color(50, 205, 50), java.awt.Color(40, 175, 40)),     // Lime Green
    JBColor(java.awt.Color(255, 165, 0), java.awt.Color(220, 140, 0)),     // Orange
    JBColor(java.awt.Color(138, 43, 226), java.awt.Color(120, 40, 190)),   // Blue Violet
    JBColor(java.awt.Color(30, 144, 255), java.awt.Color(25, 120, 210))    // Dodger Blue
)

private val bracketPairs = listOf(
    Pair('(', ')'),
    Pair('{', '}'),
    Pair('<', '>'),
    Pair('[', ']')
)

// Flag to enable/disable comment and string detection
// Set to false initially to ensure basic colorization works
private var SKIP_COMMENTS_AND_STRINGS = true

/**
 * Get the current IDE product name
 */
fun getIdeProductName(): String {
    return try {
        val appInfo = Class.forName("com.intellij.openapi.application.ApplicationInfo")
        val instance = appInfo.getMethod("getInstance").invoke(null)
        val productName = appInfo.getMethod("getFullProductName").invoke(instance) as String
        productName
    } catch (e: Exception) {
        "Unknown IDE"
    }
}

/**
 * Directly highlight brackets in the editor without using a highlighting pass.
 */
fun highlightBracketsInEditor(editor: Editor, forceRehighlight: Boolean) {
    try {
        println("[NeonBrackets] Directly highlighting brackets in editor")

        // Remove any existing highlighters if we're forcing a rehighlight
        if (forceRehighlight) {
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
            existingHighlighters.forEach {
                try {
                    it.dispose()
                } catch (e: Exception) {
                    println("[NeonBrackets] Error disposing highlighter: ${e.message}")
                }
            }
        } else {
            // If not forcing rehighlight (e.g., just cursor movement), keep existing highlighters
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
            if (existingHighlighters != null && existingHighlighters.isNotEmpty()) {
                // Just return if we already have highlighters and this is just a cursor movement
                return
            }
        }

        val text = editor.document.charsSequence
        val stacks = mutableMapOf<Char, MutableList<Int>>()
        val bracketMatches = mutableListOf<Triple<Int, Int, Int>>() // openPos, closePos, nestingLevel

        // Initialize stacks for each bracket type
        bracketPairs.forEach { (open, _) ->
            stacks[open] = mutableListOf()
        }

        // Try to get comment and string ranges, but only if enabled
        var commentAndStringRanges = emptyList<Pair<Int, Int>>()
        if (SKIP_COMMENTS_AND_STRINGS) {
            try {
                val psiFile = getPsiFile(editor)
                if (psiFile != null) {
                    println("[NeonBrackets] Got PSI file: ${psiFile.name}, language: ${psiFile.language.displayName}")
                    val lexer = getLexerForFile(psiFile)
                    if (lexer != null) {
                        println("[NeonBrackets] Got lexer for language: ${psiFile.language.displayName}")
                        commentAndStringRanges = getCommentAndStringRanges(lexer, text.toString())
                        println("[NeonBrackets] Found ${commentAndStringRanges.size} comment/string ranges")
                    } else {
                        println("[NeonBrackets] Could not get lexer for language: ${psiFile.language.displayName}")
                    }
                } else {
                    println("[NeonBrackets] Could not get PSI file for editor")
                }
            } catch (e: Exception) {
                println("[NeonBrackets] Error getting comment/string ranges: ${e.message}")
                e.printStackTrace()
            }
        }

        // Find matching brackets
        for (i in text.indices) {
            val char = text[i]
            
            // Skip if inside comment or string, but only if enabled and we have ranges
            if (SKIP_COMMENTS_AND_STRINGS && commentAndStringRanges.isNotEmpty() && 
                isInsideCommentOrString(i, commentAndStringRanges)) {
                continue
            }

            for ((open, close) in bracketPairs) {
                if (char == open) {
                    // Found opening bracket
                    stacks[open]?.add(i)
                } else if (char == close && stacks[open]?.isNotEmpty() == true) {
                    // Found closing bracket with matching opening bracket
                    val openPos = stacks[open]?.removeAt(stacks[open]?.size!! - 1) ?: continue
                    
                    // Skip if opening bracket is inside comment or string, but only if enabled and we have ranges
                    if (SKIP_COMMENTS_AND_STRINGS && commentAndStringRanges.isNotEmpty() && 
                        isInsideCommentOrString(openPos, commentAndStringRanges)) {
                        continue
                    }
                    
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

/**
 * Get the PSI file for the editor.
 */
private fun getPsiFile(editor: Editor): PsiFile? {
    val project = editor.project ?: return null
    return PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
}

/**
 * Get a lexer for the given PSI file.
 */
private fun getLexerForFile(file: PsiFile): Lexer? {
    try {
        val language = file.language
        val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language) ?: return null
        return parserDefinition.createLexer(file.project)
    } catch (e: Exception) {
        println("[NeonBrackets] Error creating lexer: ${e.message}")
        return null
    }
}

/**
 * Get ranges of comments and strings in the document.
 */
private fun getCommentAndStringRanges(lexer: Lexer, text: String): List<Pair<Int, Int>> {
    val ranges = mutableListOf<Pair<Int, Int>>()
    
    try {
        lexer.start(text)
        
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            
            // Check if token is a comment or string
            if (isCommentOrString(tokenType)) {
                ranges.add(Pair(lexer.tokenStart, lexer.tokenEnd))
            }
            
            lexer.advance()
        }
    } catch (e: Exception) {
        println("[NeonBrackets] Error processing lexer tokens: ${e.message}")
    }
    
    return ranges
}

/**
 * Check if a token type represents a comment or string.
 */
private fun isCommentOrString(tokenType: IElementType?): Boolean {
    if (tokenType == null) return false
    
    val typeName = tokenType.toString().uppercase()
    return typeName.contains("COMMENT") || 
           typeName.contains("STRING") || 
           typeName.contains("CHAR") ||
           tokenType == TokenType.WHITE_SPACE
}

/**
 * Check if a position is inside a comment or string.
 */
private fun isInsideCommentOrString(position: Int, ranges: List<Pair<Int, Int>>): Boolean {
    for ((start, end) in ranges) {
        if (position in start until end) {
            return true
        }
    }
    return false
}