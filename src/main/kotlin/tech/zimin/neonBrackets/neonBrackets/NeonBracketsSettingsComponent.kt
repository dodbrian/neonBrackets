package tech.zimin.neonBrackets.neonBrackets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.*
import javax.swing.*
import javax.swing.border.TitledBorder

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
    private var bracketColorsLightPanels = mutableListOf<RoundedColorPanel>()

    // Dark theme colors
    private var bracketColorsDarkPanels = mutableListOf<RoundedColorPanel>()

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
        val lightColorsPanel = JPanel(GridLayout(7, 2))
        lightColorsPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Light Theme Colors",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        lightColorsPanel.add(JBLabel("Nesting Level"))
        lightColorsPanel.add(JBLabel("Color"))

        for (i in 0 until 6) {
            lightColorsPanel.add(JBLabel("Level $i"))
            val colorPanel = RoundedColorPanel()
            bracketColorsLightPanels.add(colorPanel)
            lightColorsPanel.add(colorPanel)
        }

        mainPanel.add(lightColorsPanel)
        mainPanel.add(Box.createVerticalStrut(10))

        // Dark theme colors panel
        val darkColorsPanel = JPanel(GridLayout(7, 2))
        darkColorsPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Dark Theme Colors",
            TitledBorder.LEFT, TitledBorder.TOP
        )

        darkColorsPanel.add(JBLabel("Nesting Level"))
        darkColorsPanel.add(JBLabel("Color"))

        for (i in 0 until 6) {
            darkColorsPanel.add(JBLabel("Level $i"))
            val colorPanel = RoundedColorPanel()
            bracketColorsDarkPanels.add(colorPanel)
            darkColorsPanel.add(colorPanel)
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

    override fun isModified(): Boolean {
        val settings = NeonBracketsFactory.getInstance().state

        if (enabledCheckBox?.isSelected != settings.enabled) return true
        if (roundBracketsCheckBox?.isSelected != settings.enableRoundBrackets) return true
        if (curlyBracketsCheckBox?.isSelected != settings.enableCurlyBrackets) return true
        if (angleBracketsCheckBox?.isSelected != settings.enableAngleBrackets) return true
        if (squareBracketsCheckBox?.isSelected != settings.enableSquareBrackets) return true

        // Check if any color has been modified
        for (i in bracketColorsLightPanels.indices) {
            if (i < settings.bracketColorsLight.size) {
                val settingsColor = try {
                    Color.decode(settings.bracketColorsLight[i])
                } catch (_: Exception) {
                    null
                }

                if (settingsColor != bracketColorsLightPanels[i].getSelectedColor()) {
                    return true
                }
            }
        }

        for (i in bracketColorsDarkPanels.indices) {
            if (i < settings.bracketColorsDark.size) {
                val settingsColor = try {
                    Color.decode(settings.bracketColorsDark[i])
                } catch (_: Exception) {
                    null
                }

                if (settingsColor != bracketColorsDarkPanels[i].getSelectedColor()) {
                    return true
                }
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
        for (panel in bracketColorsLightPanels) {
            val color = panel.getSelectedColor()
            if (color != null) {
                lightColors.add(String.format(hexColorTemplate, color.red, color.green, color.blue))
            } else {
                lightColors.add("#FF69B4") // Default to Hot Pink if no color selected
            }
        }
        settings.bracketColorsLight = lightColors

        val darkColors = mutableListOf<String>()
        for (panel in bracketColorsDarkPanels) {
            val color = panel.getSelectedColor()
            if (color != null) {
                darkColors.add(String.format(hexColorTemplate, color.red, color.green, color.blue))
            } else {
                darkColors.add("#DC5A96") // Default to Dark Hot Pink if no color selected
            }
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

        // Reset color panels
        for (i in bracketColorsLightPanels.indices) {
            if (i < settings.bracketColorsLight.size) {
                try {
                    bracketColorsLightPanels[i].setSelectedColor(Color.decode(settings.bracketColorsLight[i]))
                } catch (_: Exception) {
                    // Use default color if parsing fails
                    bracketColorsLightPanels[i].setSelectedColor(JBColor(0xFF69B4, 0xFF69B4)) // Hot Pink
                }
            }
        }

        for (i in bracketColorsDarkPanels.indices) {
            if (i < settings.bracketColorsDark.size) {
                try {
                    bracketColorsDarkPanels[i].setSelectedColor(Color.decode(settings.bracketColorsDark[i]))
                } catch (_: Exception) {
                    // Use default color if parsing fails
                    bracketColorsDarkPanels[i].setSelectedColor(JBColor(0xDC5A96, 0xDC5A96)) // Dark Hot Pink
                }
            }
        }

        excludedFileTypesField?.text = settings.excludedFileTypes
        skipCommentsAndStringsCheckBox?.isSelected = settings.skipCommentsAndStrings
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

        bracketColorsLightPanels.forEachIndexed { index, panel ->
            panel.setSelectedColor(
                when (index) {
                    0 -> Color.decode("#FF69B4") // Hot Pink
                    1 -> Color.decode("#4169E1") // Royal Blue
                    2 -> Color.decode("#32CD32") // Lime Green
                    3 -> Color.decode("#FFA500") // Orange
                    4 -> Color.decode("#8A2BE2") // Blue Violet
                    5 -> Color.decode("#1E90FF") // Dodger Blue
                    else -> JBColor.WHITE
                }
            )
        }

        bracketColorsDarkPanels.forEachIndexed { index, panel ->
            panel.setSelectedColor(
                when (index) {
                    0 -> Color.decode("#DC5A96") // Dark Hot Pink
                    1 -> Color.decode("#375ABE") // Dark Royal Blue
                    2 -> Color.decode("#28AF28") // Dark Lime Green
                    3 -> Color.decode("#DC8C00") // Dark Orange
                    4 -> Color.decode("#7828BE") // Dark Blue Violet
                    5 -> Color.decode("#1978D2") // Dark Dodger Blue
                    else -> JBColor.BLACK
                }
            )
        }

        excludedFileTypesField?.text = ""
        skipCommentsAndStringsCheckBox?.isSelected = true
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