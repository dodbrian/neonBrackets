package tech.zimin.neonBrackets.neonBrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lexer.Lexer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.*
import javax.swing.border.TitledBorder

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
    var enabled: Boolean = true,

    // Bracket type toggles
    var enableRoundBrackets: Boolean = true,
    var enableCurlyBrackets: Boolean = true,
    var enableAngleBrackets: Boolean = true,
    var enableSquareBrackets: Boolean = true,

    // Colors (stored as hex strings)
    var bracketColorsLight: List<String> = listOf(
        "#FF69B4", // Hot Pink
        "#4169E1", // Royal Blue
        "#32CD32", // Lime Green
        "#FFA500", // Orange
        "#8A2BE2", // Blue Violet
        "#1E90FF"  // Dodger Blue
    ),

    var bracketColorsDark: List<String> = listOf(
        "#DC5A96", // Dark Hot Pink
        "#375ABE", // Dark Royal Blue
        "#28AF28", // Dark Lime Green
        "#DC8C00", // Dark Orange
        "#7828BE", // Dark Blue Violet
        "#1978D2"  // Dark Dodger Blue
    ),

    // Excluded file types (comma-separated)
    var excludedFileTypes: String = "",

    // Skip comments and strings
    var skipCommentsAndStrings: Boolean = true
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

/**
 * Highlighting pass that colorizes matching brackets.
 */
class NeonBracketsPass(
    file: PsiFile,
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

// Update the global BRACKET_COLORS to use dynamic colors from settings
private fun getBracketColors(): List<JBColor> {
    val settings = NeonBracketsFactory.getInstance().state

    return settings.bracketColorsLight.mapIndexed { index, lightColor ->
        JBColor(
            parseColor(lightColor, JBColor(0xFF69B4, 0xDC5A96)), // Hot Pink
            parseColor(settings.bracketColorsDark[index], JBColor(0xDC5A96, 0xFF69B4)) // Dark Hot Pink
        )
    }
}

private fun parseColor(colorStr: String, defaultColor: JBColor): Color {
    return try {
        Color.decode(colorStr)
    } catch (_: Exception) {
        defaultColor
    }
}

// Get the active bracket pairs based on settings
private fun getActiveBracketPairs(): List<Pair<Char, Char>> {
    val settings = NeonBracketsFactory.getInstance().state
    val activePairs = mutableListOf<Pair<Char, Char>>()

    if (settings.enableRoundBrackets) activePairs.add(Pair('(', ')'))
    if (settings.enableCurlyBrackets) activePairs.add(Pair('{', '}'))
    if (settings.enableAngleBrackets) activePairs.add(Pair('<', '>'))
    if (settings.enableSquareBrackets) activePairs.add(Pair('[', ']'))

    return activePairs
}

// Check if a file type is excluded
private fun isFileTypeExcluded(fileType: String): Boolean {
    val settings = NeonBracketsFactory.getInstance().state
    if (settings.excludedFileTypes.isBlank()) return false

    val excludedTypes = settings.excludedFileTypes.split(",").map { it.trim().lowercase() }
    return fileType.lowercase() in excludedTypes
}

/**
 * Directly highlight brackets in the editor without using a highlighting pass.
 */
fun highlightBracketsInEditor(editor: Editor, forceRehighlight: Boolean) {
    try {
        val settings = NeonBracketsFactory.getInstance().state

        // Skip if plugin is disabled
        if (!settings.enabled) {
            // Remove any existing highlighters if we're disabling
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
            existingHighlighters.forEach {
                try {
                    it.dispose()
                } catch (_: Exception) {
                    // Silent exception handling
                }
            }
            editor.putUserData(BRACKET_HIGHLIGHTERS, emptyList())
            return
        }

        // Check if file type is excluded
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file != null) {
            val fileType = file.fileType.name
            if (isFileTypeExcluded(fileType)) {
                // Remove any existing highlighters for excluded file types
                val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
                existingHighlighters.forEach {
                    try {
                        it.dispose()
                    } catch (_: Exception) {
                        // Silent exception handling
                    }
                }
                editor.putUserData(BRACKET_HIGHLIGHTERS, emptyList())
                return
            }
        }

        // Remove any existing highlighters if we're forcing a rehighlight
        if (forceRehighlight) {
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
            existingHighlighters.forEach {
                try {
                    it.dispose()
                } catch (_: Exception) {
                    // Silent exception handling
                }
            }
        } else {
            // If not forcing rehighlight (e.g., just cursor movement), keep existing highlighters
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
            if (!existingHighlighters.isNullOrEmpty()) {
                // Just return if we already have highlighters and this is just a cursor movement
                return
            }
        }

        val text = editor.document.charsSequence
        val stacks = mutableMapOf<Char, MutableList<Int>>()
        val bracketMatches = mutableListOf<Triple<Int, Int, Int>>() // openPos, closePos, nestingLevel

        // Get active bracket pairs based on settings
        val activeBracketPairs = getActiveBracketPairs()

        // Initialize stacks for each bracket type
        activeBracketPairs.forEach { (open, _) ->
            stacks[open] = mutableListOf()
        }

        // Try to get comment and string ranges, but only if enabled
        var commentAndStringRanges = emptyList<Pair<Int, Int>>()
        if (settings.skipCommentsAndStrings) {
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
            if (settings.skipCommentsAndStrings && commentAndStringRanges.isNotEmpty() &&
                isInsideCommentOrString(i, commentAndStringRanges)
            ) {
                continue
            }

            for ((_, pair) in activeBracketPairs.withIndex()) {
                val (open, close) = pair
                if (char == open) {
                    // Found opening bracket
                    stacks[open]?.add(i)
                } else if (char == close && stacks[open]?.isNotEmpty() == true) {
                    // Found closing bracket with matching opening bracket
                    val openPos = stacks[open]?.removeAt(stacks[open]?.size!! - 1) ?: continue

                    // Skip if opening bracket is inside comment or string, but only if enabled and we have ranges
                    if (settings.skipCommentsAndStrings && commentAndStringRanges.isNotEmpty() &&
                        isInsideCommentOrString(openPos, commentAndStringRanges)
                    ) {
                        continue
                    }

                    // Use the nesting level for coloring
                    val level = stacks[open]?.size ?: 0
                    bracketMatches.add(Triple(openPos, i, level))
                }
            }
        }

        println("[NeonBrackets] Found ${bracketMatches.size} bracket pairs")

        // Apply highlighters
        val newHighlighters = mutableListOf<RangeHighlighter>()
        val markupModel = editor.markupModel

        // Get bracket colors from settings
        val bracketColors = getBracketColors()

        for ((openPos, closePos, level) in bracketMatches) {
            // Use the nesting level to determine color
            val colorIndex = level % bracketColors.size
            val color = bracketColors[colorIndex]

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

/**
 * Get the current IDE product name
 */
fun getIdeProductName(): String {
    return try {
        val appInfo = Class.forName("com.intellij.openapi.application.ApplicationInfo")
        val instance = appInfo.getMethod("getInstance").invoke(null)
        val productName = appInfo.getMethod("getFullProductName").invoke(instance) as String
        productName
    } catch (_: Exception) {
        "Unknown IDE"
    }
}

/**
 * Settings component for the plugin.
 */
class NeonBracketsSettingsComponent : Configurable {
    private var myPanel: JPanel? = null
    private var enabledCheckBox: JBCheckBox? = null
    private var roundBracketsCheckBox: JBCheckBox? = null
    private var curlyBracketsCheckBox: JBCheckBox? = null
    private var angleBracketsCheckBox: JBCheckBox? = null
    private var squareBracketsCheckBox: JBCheckBox? = null

    // Light theme colors
    private var bracketColorsLightFields = mutableListOf<JBTextField>()
    private var bracketColorsLightButtons = mutableMapOf<Int, JButton>()

    // Dark theme colors
    private var bracketColorsDarkFields = mutableListOf<JBTextField>()
    private var bracketColorsDarkButtons = mutableMapOf<Int, JButton>()

    private var excludedFileTypesField: JBTextField? = null
    private var skipCommentsAndStringsCheckBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Neon Brackets"

    override fun createComponent(): JComponent {
        myPanel = JPanel(BorderLayout())

        // Main panel with all settings
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        // Global enable/disable
        enabledCheckBox = JBCheckBox("Enable Neon Brackets")
        mainPanel.add(enabledCheckBox)
        mainPanel.add(Box.createVerticalStrut(10))

        // Bracket types panel
        val bracketTypesPanel = JPanel(GridLayout(4, 1))
        bracketTypesPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Bracket Types",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        roundBracketsCheckBox = JBCheckBox("Round Brackets ( )")
        curlyBracketsCheckBox = JBCheckBox("Curly Brackets { }")
        angleBracketsCheckBox = JBCheckBox("Angle Brackets < >")
        squareBracketsCheckBox = JBCheckBox("Square Brackets [ ]")

        bracketTypesPanel.add(roundBracketsCheckBox)
        bracketTypesPanel.add(curlyBracketsCheckBox)
        bracketTypesPanel.add(angleBracketsCheckBox)
        bracketTypesPanel.add(squareBracketsCheckBox)

        mainPanel.add(bracketTypesPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Light theme colors panel
        val lightColorsPanel = JPanel(GridLayout(7, 3))
        lightColorsPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Light Theme Colors",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        lightColorsPanel.add(JBLabel("Nesting Level"))
        lightColorsPanel.add(JBLabel("Hex Color"))
        lightColorsPanel.add(JBLabel("Pick"))

        for (i in 0 until 6) {
            lightColorsPanel.add(JBLabel("Level $i"))
            val field = JBTextField()
            bracketColorsLightFields.add(field)
            lightColorsPanel.add(field)
            val button = JButton("...")
            bracketColorsLightButtons[i] = button
            button.addActionListener { pickColor(field, button) }
            lightColorsPanel.add(button)
        }

        mainPanel.add(lightColorsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Dark theme colors panel
        val darkColorsPanel = JPanel(GridLayout(7, 3))
        darkColorsPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Dark Theme Colors",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        darkColorsPanel.add(JBLabel("Nesting Level"))
        darkColorsPanel.add(JBLabel("Hex Color"))
        darkColorsPanel.add(JBLabel("Pick"))

        for (i in 0 until 6) {
            darkColorsPanel.add(JBLabel("Level $i"))
            val field = JBTextField()
            bracketColorsDarkFields.add(field)
            darkColorsPanel.add(field)
            val button = JButton("...")
            bracketColorsDarkButtons[i] = button
            button.addActionListener { pickColor(field, button) }
            darkColorsPanel.add(button)
        }

        mainPanel.add(darkColorsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Additional options panel
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Additional Options",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        skipCommentsAndStringsCheckBox = JBCheckBox("Skip Comments and Strings")
        optionsPanel.add(skipCommentsAndStringsCheckBox)

        val excludedFileTypesPanel = JPanel(BorderLayout())
        excludedFileTypesPanel.add(JBLabel("Excluded File Types (comma-separated):"), BorderLayout.NORTH)
        excludedFileTypesField = JBTextField()
        excludedFileTypesPanel.add(excludedFileTypesField!!, BorderLayout.CENTER)
        optionsPanel.add(excludedFileTypesPanel)

        mainPanel.add(optionsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Reset to defaults button
        val resetPanel = JPanel(BorderLayout())
        val resetButton = JButton("Reset to Defaults")
        resetButton.addActionListener { resetToDefaults() }
        resetPanel.add(resetButton, BorderLayout.EAST)
        mainPanel.add(resetPanel)

        myPanel!!.add(mainPanel, BorderLayout.CENTER)

        // Load current settings
        reset()

        return myPanel!!
    }

    private fun pickColor(textField: JBTextField, button: JButton) {
        val initialColor = try {
            Color.decode(textField.text)
        } catch (_: Exception) {
            JBColor.WHITE
        }

        val color = JColorChooser.showDialog(myPanel, "Choose Color", initialColor)
        if (color != null) {
            val hexColor = String.format("#%02X%02X%02X", color.red, color.green, color.blue)
            textField.text = hexColor
            button.background = color
        }
    }

    override fun isModified(): Boolean {
        val settings = NeonBracketsFactory.getInstance().state

        if (enabledCheckBox?.isSelected != settings.enabled) return true
        if (roundBracketsCheckBox?.isSelected != settings.enableRoundBrackets) return true
        if (curlyBracketsCheckBox?.isSelected != settings.enableCurlyBrackets) return true
        if (angleBracketsCheckBox?.isSelected != settings.enableAngleBrackets) return true
        if (squareBracketsCheckBox?.isSelected != settings.enableSquareBrackets) return true

        // Check if any color has been modified
        for (i in bracketColorsLightFields.indices) {
            if (i < settings.bracketColorsLight.size &&
                bracketColorsLightFields[i].text != settings.bracketColorsLight[i]
            ) {
                return true
            }
        }

        for (i in bracketColorsDarkFields.indices) {
            if (i < settings.bracketColorsDark.size &&
                bracketColorsDarkFields[i].text != settings.bracketColorsDark[i]
            ) {
                return true
            }
        }

        if (excludedFileTypesField?.text != settings.excludedFileTypes) return true
        if (skipCommentsAndStringsCheckBox?.isSelected != settings.skipCommentsAndStrings) return true

        return false
    }

    override fun apply() {
        val settings = NeonBracketsFactory.getInstance().state

        settings.enabled = enabledCheckBox?.isSelected ?: true
        settings.enableRoundBrackets = roundBracketsCheckBox?.isSelected ?: true
        settings.enableCurlyBrackets = curlyBracketsCheckBox?.isSelected ?: true
        settings.enableAngleBrackets = angleBracketsCheckBox?.isSelected ?: true
        settings.enableSquareBrackets = squareBracketsCheckBox?.isSelected ?: true

        // Update color lists
        val lightColors = mutableListOf<String>()
        for (field in bracketColorsLightFields) {
            lightColors.add(field.text)
        }
        settings.bracketColorsLight = lightColors

        val darkColors = mutableListOf<String>()
        for (field in bracketColorsDarkFields) {
            darkColors.add(field.text)
        }
        settings.bracketColorsDark = darkColors

        settings.excludedFileTypes = excludedFileTypesField?.text ?: ""
        settings.skipCommentsAndStrings = skipCommentsAndStringsCheckBox?.isSelected ?: true

        // Refresh all open editors
        ApplicationManager.getApplication().invokeLater {
            refreshAllEditors()
        }
    }

    override fun reset() {
        val settings = NeonBracketsFactory.getInstance().state

        enabledCheckBox?.isSelected = settings.enabled
        roundBracketsCheckBox?.isSelected = settings.enableRoundBrackets
        curlyBracketsCheckBox?.isSelected = settings.enableCurlyBrackets
        angleBracketsCheckBox?.isSelected = settings.enableAngleBrackets
        squareBracketsCheckBox?.isSelected = settings.enableSquareBrackets

        // Reset color fields
        for (i in bracketColorsLightFields.indices) {
            if (i < settings.bracketColorsLight.size) {
                bracketColorsLightFields[i].text = settings.bracketColorsLight[i]
            }
        }

        for (i in bracketColorsDarkFields.indices) {
            if (i < settings.bracketColorsDark.size) {
                bracketColorsDarkFields[i].text = settings.bracketColorsDark[i]
            }
        }

        excludedFileTypesField?.text = settings.excludedFileTypes
        skipCommentsAndStringsCheckBox?.isSelected = settings.skipCommentsAndStrings

        // Update button colors
        updateButtonColors()
    }

    override fun disposeUIResources() {
        myPanel = null
    }

    private fun resetToDefaults() {
        // Reset to default values
        enabledCheckBox?.isSelected = true

        roundBracketsCheckBox?.isSelected = true
        curlyBracketsCheckBox?.isSelected = true
        angleBracketsCheckBox?.isSelected = true
        squareBracketsCheckBox?.isSelected = true

        bracketColorsLightFields.forEachIndexed { index, field ->
            field.text = when (index) {
                0 -> "#FF69B4" // Hot Pink
                1 -> "#4169E1" // Royal Blue
                2 -> "#32CD32" // Lime Green
                3 -> "#FFA500" // Orange
                4 -> "#8A2BE2" // Blue Violet
                5 -> "#1E90FF" // Dodger Blue
                else -> "#FFFFFF"
            }
        }

        bracketColorsDarkFields.forEachIndexed { index, field ->
            field.text = when (index) {
                0 -> "#DC5A96" // Dark Hot Pink
                1 -> "#375ABE" // Dark Royal Blue
                2 -> "#28AF28" // Dark Lime Green
                3 -> "#DC8C00" // Dark Orange
                4 -> "#7828BE" // Dark Blue Violet
                5 -> "#1978D2" // Dark Dodger Blue
                else -> "#000000"
            }
        }

        excludedFileTypesField?.text = ""
        skipCommentsAndStringsCheckBox?.isSelected = true

        // Update button colors
        updateButtonColors()
    }

    private fun updateButtonColors() {
        try {
            for ((index, button) in bracketColorsLightButtons) {
                button.background = Color.decode(bracketColorsLightFields[index].text)
            }

            for ((index, button) in bracketColorsDarkButtons) {
                button.background = Color.decode(bracketColorsDarkFields[index].text)
            }
        } catch (_: Exception) {
            // Ignore color parsing errors
        }
    }

    private fun refreshAllEditors() {
        val editorFactory = EditorFactory.getInstance()
        val editors = editorFactory.allEditors

        for (editor in editors) {
            // Remove existing highlighters
            val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
            existingHighlighters?.forEach {
                try {
                    it.dispose()
                } catch (_: Exception) {
                    // Silent exception handling
                }
            }

            // Apply highlighting with current settings
            highlightBracketsInEditor(editor, true)
        }
    }
}

/**
 * Action to toggle Neon Brackets highlighting.
 */
class ToggleNeonBracketsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val settings = NeonBracketsFactory.getInstance().state
        settings.enabled = !settings.enabled

        // Refresh all open editors
        ApplicationManager.getApplication().invokeLater {
            refreshAllEditors()
        }
    }

    override fun update(e: AnActionEvent) {
        val settings = NeonBracketsFactory.getInstance().state
        e.presentation.text = if (settings.enabled) "Disable Neon Brackets" else "Enable Neon Brackets"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

/**
 * Refresh all open editors with current settings.
 */
fun refreshAllEditors() {
    val editorFactory = EditorFactory.getInstance()
    val editors = editorFactory.allEditors

    for (editor in editors) {
        // Remove existing highlighters
        val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
        existingHighlighters?.forEach {
            try {
                it.dispose()
            } catch (_: Exception) {
                // Silent exception handling
            }
        }

        // Apply highlighting with current settings
        highlightBracketsInEditor(editor, true)
    }
}