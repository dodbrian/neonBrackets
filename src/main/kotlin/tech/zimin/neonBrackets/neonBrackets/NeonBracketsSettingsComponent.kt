package tech.zimin.neonBrackets.neonBrackets

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.TitledBorder
import kotlin.collections.iterator

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