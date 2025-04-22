package tech.zimin.neonBrackets.neonBrackets

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
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
                highlightBracketsInEditor(editor)
            }
        }
    }
}

/**
 * Project listener that registers the highlighting pass factory when a project is opened.
 */
class NeonBracketsInitializer : StartupActivity, DumbAware {
    private var editorFactoryListener: EditorFactoryListener? = null

    override fun runActivity(project: Project) {
        println("[NeonBrackets] Initializing NeonBrackets for project: ${project.name}")

        // Create a dedicated disposable for our plugin's resources
        val pluginDisposable = Disposer.newDisposable("NeonBracketsPluginDisposable")
        
        // Get a connection from the project's message bus
        val connection = project.messageBus.connect(pluginDisposable)
        
        // Listen for project closing events to clean up our resources
        connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectClosing(closingProject: Project) {
                if (project == closingProject) {
                    println("[NeonBrackets] Project closing, disposing resources: ${project.name}")
                    Disposer.dispose(pluginDisposable)
                }
            }
        })
        
        // Register editor factory listener to catch new editors
        val listener = object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                val editor = event.editor
                if (editor.project == project) {
                    setupEditorListeners(editor)
                    highlightBracketsInEditor(editor)
                }
            }

            override fun editorReleased(event: EditorFactoryEvent) {
                val editor = event.editor
                clearHighlighters(editor)
                removeEditorListeners(editor)
            }
        }

        // Register the listener with EditorFactory using our disposable
        // This is the non-deprecated way to register the listener
        EditorFactory.getInstance().addEditorFactoryListener(listener, pluginDisposable)
        editorFactoryListener = listener

        // Process all open editors immediately
        ApplicationManager.getApplication().invokeLater {
            processAllOpenEditors(project)
        }
    }

    private fun processAllOpenEditors(project: Project) {
        // Get all open editors and apply highlighting
        val editors = EditorFactory.getInstance().allEditors
        println("[NeonBrackets] Processing ${editors.size} existing editors")

        for (editor in editors) {
            if (editor.project == project) {
                setupEditorListeners(editor)
                highlightBracketsInEditor(editor)
            }
        }
    }
}

/**
 * Set up document and selection listeners for an editor.
 */
private fun setupEditorListeners(editor: Editor) {
    // Add document listener if not already added
    if (editor.getUserData(DOCUMENT_LISTENER) == null) {
        val docListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                highlightBracketsInEditor(editor)
            }
        }
        editor.document.addDocumentListener(docListener)
        editor.putUserData(DOCUMENT_LISTENER, docListener)
    }

    // Add selection listener if not already added
    if (editor.getUserData(SELECTION_LISTENER) == null) {
        val selectionListener = object : SelectionListener {
            override fun selectionChanged(event: SelectionEvent) {
                highlightBracketsInEditor(editor)
            }
        }
        editor.selectionModel.addSelectionListener(selectionListener)
        editor.putUserData(SELECTION_LISTENER, selectionListener)
    }
}

/**
 * Remove document and selection listeners from an editor.
 */
private fun removeEditorListeners(editor: Editor) {
    // Remove document listener
    val documentListener = editor.getUserData(DOCUMENT_LISTENER)
    if (documentListener != null) {
        editor.document.removeDocumentListener(documentListener)
        editor.putUserData(DOCUMENT_LISTENER, null)
    }

    // Remove selection listener
    val selectionListener = editor.getUserData(SELECTION_LISTENER)
    if (selectionListener != null) {
        editor.selectionModel.removeSelectionListener(selectionListener)
        editor.putUserData(SELECTION_LISTENER, null)
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
                    highlightBracketsInEditor(editor)
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
            highlightBracketsInEditor(editor) // Force full rehighlight on document change
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
                highlightBracketsInEditor(editor)
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
            highlightBracketsInEditor(editor)
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
        highlightBracketsInEditor(editor)
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
private fun isFileTypeExcluded(file: com.intellij.openapi.vfs.VirtualFile): Boolean {
    val settings = NeonBracketsFactory.getInstance().state
    if (settings.excludedFileTypes.isBlank()) return false

    val excludedTypes = settings.excludedFileTypes.split(",").map { it.trim().lowercase() }

    // Get the actual file extension
    val extension = file.extension?.lowercase() ?: ""

    println("[NeonBrackets] Checking if file extension '$extension' is in excluded types: $excludedTypes")

    return extension.isNotEmpty() && excludedTypes.contains(extension)
}

/**
 * Directly highlight brackets in the editor without using a highlighting pass.
 */
fun highlightBracketsInEditor(editor: Editor) {
    try {
        val settings = NeonBracketsFactory.getInstance().state

        // Always clear existing highlighters first
        clearHighlighters(editor)

        // If plugin is disabled, just return after clearing
        if (!settings.enabled) {
            return
        }

        // Check if file type is excluded
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file != null && isFileTypeExcluded(file)) {
            return
        }

        // Skip if we're already highlighting this document (prevents recursion)
        if (editor.getUserData(SKIP_BRACKET_HIGHLIGHTING) == true) {
            return
        }

        // Set flag to prevent recursive highlighting
        editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, true)

        try {
            // Process the entire document
            val document = editor.document
            val text = document.text
            val bracketColors = getBracketColors()
            val activeBracketPairs = getActiveBracketPairs()

            // Skip if no bracket types are enabled
            if (activeBracketPairs.isEmpty()) {
                return
            }

            val newHighlighters = mutableListOf<RangeHighlighter>()
            val bracketStacks = mutableMapOf<Char, MutableList<Int>>()

            // Initialize stacks for each bracket type
            activeBracketPairs.forEach { (open, _) ->
                bracketStacks[open] = mutableListOf()
            }

            // Get the PsiFile for checking comments and strings
            val psiFile = if (settings.skipCommentsAndStrings) {
                val project = editor.project
                if (project != null) {
                    PsiDocumentManager.getInstance(project).getPsiFile(document)
                } else {
                    null
                }
            } else {
                null
            }

            // Process each character in the document
            for (i in text.indices) {
                val char = text[i]

                // Skip comments and strings if enabled
                if (psiFile != null && isInCommentOrString(psiFile, i)) {
                    continue
                }

                // Check for opening brackets
                for ((openChar, closeChar) in activeBracketPairs) {
                    if (char == openChar) {
                        bracketStacks[openChar]?.add(i)
                        break
                    } else if (char == closeChar && bracketStacks[openChar]?.isNotEmpty() == true) {
                        val openPos = bracketStacks[openChar]?.removeAt(bracketStacks[openChar]?.size!! - 1) ?: continue

                        // Calculate nesting level for color
                        val nestingLevel = bracketStacks[openChar]?.size ?: 0
                        val colorIndex = nestingLevel % bracketColors.size
                        val color = bracketColors[colorIndex]

                        // Add highlighters for both brackets
                        addHighlighter(editor, openPos, color, newHighlighters)
                        addHighlighter(editor, i, color, newHighlighters)
                        break
                    }
                }
            }

            // Store the new highlighters
            editor.putUserData(BRACKET_HIGHLIGHTERS, newHighlighters)
        } finally {
            // Clear flag when done
            editor.putUserData(SKIP_BRACKET_HIGHLIGHTING, false)
        }
    } catch (_: Exception) {
        // Silent exception handling
    }
}

/**
 * Clear all existing highlighters from the editor.
 */
private fun clearHighlighters(editor: Editor) {
    val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS) ?: emptyList()
    existingHighlighters.forEach {
        try {
            it.dispose()
        } catch (_: Exception) {
            // Silent exception handling
        }
    }
    editor.putUserData(BRACKET_HIGHLIGHTERS, emptyList())
}

/**
 * Add a highlighter for a bracket.
 */
private fun addHighlighter(
    editor: Editor,
    position: Int,
    color: JBColor,
    highlighters: MutableList<RangeHighlighter>
) {
    try {
        val highlighter = editor.markupModel.addRangeHighlighter(
            position, position + 1,
            HighlighterLayer.SELECTION - 1, // Just below selection layer
            TextAttributes(color, null, null, null, Font.PLAIN),
            HighlighterTargetArea.EXACT_RANGE
        )
        highlighters.add(highlighter)
    } catch (_: Exception) {
        // Silent exception handling
    }
}

/**
 * Check if the position is within a comment or string.
 */
private fun isInCommentOrString(psiFile: PsiFile, offset: Int): Boolean {
    val element = psiFile.findElementAt(offset)
    return element != null && (
            PsiTreeUtil.getParentOfType(element, PsiComment::class.java) != null ||
                    element.node?.elementType.toString().contains("STRING") ||
                    element.node?.elementType.toString().contains("COMMENT")
            )
}

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

        roundBracketsCheckBox = JBCheckBox("( ) Round brackets")
        curlyBracketsCheckBox = JBCheckBox("{ } Curly brackets")
        angleBracketsCheckBox = JBCheckBox("< > Angle brackets")
        squareBracketsCheckBox = JBCheckBox("[ ] Square brackets")

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

        skipCommentsAndStringsCheckBox = JBCheckBox("Skip comments and strings")
        optionsPanel.add(skipCommentsAndStringsCheckBox)

        val excludedFileTypesPanel = JPanel(BorderLayout())
        val excludedFileTypesLabel = JBLabel("Excluded file types (comma-separated):")
        excludedFileTypesLabel.toolTipText = "Enter file types without wildcards, e.g.: 'java, xml, kt, cs'"
        excludedFileTypesPanel.add(excludedFileTypesLabel, BorderLayout.NORTH)
        excludedFileTypesField = JBTextField()
        excludedFileTypesField!!.toolTipText =
            "Enter file extensions without dots or wildcards (e.g., 'java, xml, kt, cs')"
        excludedFileTypesPanel.add(excludedFileTypesField!!, BorderLayout.CENTER)
        optionsPanel.add(excludedFileTypesPanel)

        mainPanel.add(optionsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Reset to defaults button
        val resetPanel = JPanel(BorderLayout())
        val resetButton = JButton("Reset to defaults")
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
            highlightBracketsInEditor(editor)
        }
    }
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
