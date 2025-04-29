package tech.zimin.neonBrackets.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.ui.ColorPanel
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import tech.zimin.neonBrackets.common.hexColorTemplate
import tech.zimin.neonBrackets.neonBrackets.BRACKET_HIGHLIGHTERS
import tech.zimin.neonBrackets.neonBrackets.NeonBracketsFactory
import tech.zimin.neonBrackets.neonBrackets.highlightBracketsInEditor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

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
    private var bracketColorsLightPanels = mutableListOf<ColorPanel>()

    // Dark theme colors
    private var bracketColorsDarkPanels = mutableListOf<ColorPanel>()

    private var excludedFileTypesField: JBTextField? = null
    private var skipCommentsAndStringsCheckBox: JBCheckBox? = null

    // Default colors for light theme
    private val defaultLightColors = listOf(
        Color.decode("#FF69B4"), // Hot Pink
        Color.decode("#4169E1"), // Royal Blue
        Color.decode("#32CD32"), // Lime Green
        Color.decode("#FFA500"), // Orange
        Color.decode("#8A2BE2"), // Blue Violet
        Color.decode("#1E90FF")  // Dodger Blue
    )

    // Default colors for dark theme
    private val defaultDarkColors = listOf(
        Color.decode("#DC5A96"), // Dark Hot Pink
        Color.decode("#375ABE"), // Dark Royal Blue
        Color.decode("#28AF28"), // Dark Lime Green
        Color.decode("#DC8C00"), // Dark Orange
        Color.decode("#7828BE"), // Dark Blue Violet
        Color.decode("#1978D2")  // Dark Dodger Blue
    )

    override fun getDisplayName(): String = "Neon Brackets"

    override fun createComponent(): JComponent {
        // Create main panel with vertical box layout
        myPanel = JPanel()
        myPanel!!.layout = BoxLayout(myPanel, BoxLayout.Y_AXIS)
        myPanel!!.border = JBUI.Borders.empty(10)

        // Global enable/disable
        enabledCheckBox = JBCheckBox("Enable Neon Brackets")
        myPanel!!.add(enabledCheckBox)
        myPanel!!.add(Box.createVerticalStrut(10))

        // Bracket types section
        myPanel!!.add(TitledSeparator("Bracket Types"))
        myPanel!!.add(Box.createVerticalStrut(5))

        // Bracket types checkboxes
        roundBracketsCheckBox = JBCheckBox("( ) Round brackets")
        curlyBracketsCheckBox = JBCheckBox("{ } Curly brackets")
        angleBracketsCheckBox = JBCheckBox("< > Angle brackets")
        squareBracketsCheckBox = JBCheckBox("[ ] Square brackets")

        // Create bracket types panel
        val bracketTypesPanel = JPanel(GridLayout(4, 1))
        bracketTypesPanel.add(roundBracketsCheckBox)
        bracketTypesPanel.add(curlyBracketsCheckBox)
        bracketTypesPanel.add(angleBracketsCheckBox)
        bracketTypesPanel.add(squareBracketsCheckBox)
        bracketTypesPanel.alignmentX = Component.LEFT_ALIGNMENT

        myPanel!!.add(bracketTypesPanel)
        myPanel!!.add(Box.createVerticalStrut(10))

        // Colors section
        myPanel!!.add(TitledSeparator("Colors"))
        myPanel!!.add(Box.createVerticalStrut(5))

        // Light theme colors panel
        val lightColorsPanel = createColorPanel("Light Theme Colors", bracketColorsLightPanels)
        lightColorsPanel.alignmentX = Component.LEFT_ALIGNMENT
        myPanel!!.add(lightColorsPanel)
        myPanel!!.add(Box.createVerticalStrut(10))

        // Dark theme colors panel
        val darkColorsPanel = createColorPanel("Dark Theme Colors", bracketColorsDarkPanels)
        darkColorsPanel.alignmentX = Component.LEFT_ALIGNMENT
        myPanel!!.add(darkColorsPanel)
        myPanel!!.add(Box.createVerticalStrut(10))

        // Additional options section
        myPanel!!.add(TitledSeparator("Additional Options"))
        myPanel!!.add(Box.createVerticalStrut(5))

        // Additional options
        skipCommentsAndStringsCheckBox = JBCheckBox("Skip comments and strings")

        excludedFileTypesField = JBTextField()
        excludedFileTypesField!!.toolTipText =
            "Enter file extensions without dots or wildcards (e.g., 'java, xml, kt, cs')"

        val excludedFileTypesPanel = JPanel(BorderLayout())
        val excludedFileTypesLabel = JBLabel("Excluded file types (comma-separated):")
        excludedFileTypesLabel.toolTipText = "Enter file types without wildcards, e.g.: 'java, xml, kt, cs'"
        excludedFileTypesPanel.add(excludedFileTypesLabel, BorderLayout.NORTH)
        excludedFileTypesPanel.add(excludedFileTypesField!!, BorderLayout.CENTER)

        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.add(skipCommentsAndStringsCheckBox)
        optionsPanel.add(Box.createVerticalStrut(10))
        optionsPanel.add(excludedFileTypesPanel)
        optionsPanel.alignmentX = Component.LEFT_ALIGNMENT

        myPanel!!.add(optionsPanel)
        myPanel!!.add(Box.createVerticalStrut(20))

        // Reset to defaults button
        val resetButtonPanel = JPanel(BorderLayout())
        resetButtonPanel.alignmentX = Component.LEFT_ALIGNMENT
        val resetButton = JButton("Reset to defaults")
        resetButton.addActionListener { resetToDefaults() }
        resetButtonPanel.add(resetButton, BorderLayout.EAST)

        myPanel!!.add(resetButtonPanel)

        // Load current settings
        reset()

        return myPanel!!
    }

    private fun createColorPanel(title: String, colorPanels: MutableList<ColorPanel>): JPanel {
        val panel = JPanel(GridLayout(7, 2, 5, 5))
        panel.border = JBUI.Borders.empty(5)

        panel.add(JBLabel("Nesting Level"))
        panel.add(JBLabel("Color"))

        for (i in 0 until 6) {
            panel.add(JBLabel("Level $i"))
            val colorPanel = ColorPanel()
            colorPanels.add(colorPanel)
            panel.add(colorPanel)
        }

        return panel
    }

    override fun isModified(): Boolean {
        val settings = NeonBracketsFactory.Companion.getInstance().state

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

                if (settingsColor != bracketColorsLightPanels[i].selectedColor) {
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

                if (settingsColor != bracketColorsDarkPanels[i].selectedColor) {
                    return true
                }
            }
        }

        if (excludedFileTypesField?.text != settings.excludedFileTypes) return true
        if (skipCommentsAndStringsCheckBox?.isSelected != settings.skipCommentsAndStrings) return true

        return false
    }

    override fun apply() {
        val settings = NeonBracketsFactory.Companion.getInstance().state

        settings.enabled = enabledCheckBox?.isSelected ?: true
        settings.enableRoundBrackets = roundBracketsCheckBox?.isSelected ?: true
        settings.enableCurlyBrackets = curlyBracketsCheckBox?.isSelected ?: true
        settings.enableAngleBrackets = angleBracketsCheckBox?.isSelected ?: true
        settings.enableSquareBrackets = squareBracketsCheckBox?.isSelected ?: true

        // Update color lists
        val lightColors = mutableListOf<String>()
        for (panel in bracketColorsLightPanels) {
            val color = panel.selectedColor
            if (color != null) {
                lightColors.add(String.format(hexColorTemplate, color.red, color.green, color.blue))
            }
        }
        settings.bracketColorsLight = lightColors

        val darkColors = mutableListOf<String>()
        for (panel in bracketColorsDarkPanels) {
            val color = panel.selectedColor
            if (color != null) {
                darkColors.add(String.format(hexColorTemplate, color.red, color.green, color.blue))
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
        val settings = NeonBracketsFactory.Companion.getInstance().state

        enabledCheckBox?.isSelected = settings.enabled
        roundBracketsCheckBox?.isSelected = settings.enableRoundBrackets
        curlyBracketsCheckBox?.isSelected = settings.enableCurlyBrackets
        angleBracketsCheckBox?.isSelected = settings.enableAngleBrackets
        squareBracketsCheckBox?.isSelected = settings.enableSquareBrackets

        // Set colors
        for (i in bracketColorsLightPanels.indices) {
            if (i < settings.bracketColorsLight.size) {
                try {
                    val color = Color.decode(settings.bracketColorsLight[i])
                    bracketColorsLightPanels[i].selectedColor = color
                } catch (_: Exception) {
                    // Use default color if parsing fails
                    bracketColorsLightPanels[i].selectedColor = defaultLightColors[i % defaultLightColors.size]
                }
            } else {
                // Use default color if not enough colors in settings
                bracketColorsLightPanels[i].selectedColor = defaultLightColors[i % defaultLightColors.size]
            }
        }

        for (i in bracketColorsDarkPanels.indices) {
            if (i < settings.bracketColorsDark.size) {
                try {
                    val color = Color.decode(settings.bracketColorsDark[i])
                    bracketColorsDarkPanels[i].selectedColor = color
                } catch (_: Exception) {
                    // Use default color if parsing fails
                    bracketColorsDarkPanels[i].selectedColor = defaultDarkColors[i % defaultDarkColors.size]
                }
            } else {
                // Use default color if not enough colors in settings
                bracketColorsDarkPanels[i].selectedColor = defaultDarkColors[i % defaultDarkColors.size]
            }
        }

        excludedFileTypesField?.text = settings.excludedFileTypes
        skipCommentsAndStringsCheckBox?.isSelected = settings.skipCommentsAndStrings
    }

    override fun disposeUIResources() {
        myPanel = null
    }

    private fun resetToDefaults() {
        // Reset bracket types
        roundBracketsCheckBox?.isSelected = true
        curlyBracketsCheckBox?.isSelected = true
        angleBracketsCheckBox?.isSelected = true
        squareBracketsCheckBox?.isSelected = true

        // Reset colors to defaults
        for (i in bracketColorsLightPanels.indices) {
            bracketColorsLightPanels[i].selectedColor = defaultLightColors[i % defaultLightColors.size]
        }

        for (i in bracketColorsDarkPanels.indices) {
            bracketColorsDarkPanels[i].selectedColor = defaultDarkColors[i % defaultDarkColors.size]
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