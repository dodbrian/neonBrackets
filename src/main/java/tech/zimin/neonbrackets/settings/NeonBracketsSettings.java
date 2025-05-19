package tech.zimin.neonbrackets.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.ColorPanel;

import tech.zimin.neonbrackets.NeonBracketsFactory;
import tech.zimin.neonbrackets.NeonBracketsPluginKt;
import tech.zimin.neonbrackets.common.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeonBracketsSettings implements Configurable {
    private JPanel panel;
    private JCheckBox enableNeonBracketsCheckBox;
    private JCheckBox roundBracketsCheckBox;
    private JCheckBox curlyBracketsCheckBox;
    private JCheckBox squareBracketsCheckBox;
    private JCheckBox angleBracketsCheckBox;
    private JCheckBox skipCommentsAndStringsCheckBox;
    private JTextField excludedFileTypesTextField;
    private JButton resetToDefaultsButton;
    private ColorPanel light1ColorPanel;
    private ColorPanel light2ColorPanel;
    private ColorPanel light3ColorPanel;
    private ColorPanel light4ColorPanel;
    private ColorPanel light5ColorPanel;
    private ColorPanel light6ColorPanel;
    private ColorPanel dark1ColorPanel;
    private ColorPanel dark2ColorPanel;
    private ColorPanel dark3ColorPanel;
    private ColorPanel dark4ColorPanel;
    private ColorPanel dark5ColorPanel;
    private ColorPanel dark6ColorPanel;

    // Default colors for light theme
    private final List<Color> defaultLightColors = Arrays.asList(
        Color.decode("#FF69B4"), // Hot Pink
        Color.decode("#4169E1"), // Royal Blue
        Color.decode("#32CD32"), // Lime Green
        Color.decode("#FFA500"), // Orange
        Color.decode("#8A2BE2"), // Blue Violet
        Color.decode("#1E90FF")  // Dodger Blue
    );

    // Default colors for dark theme
    private final List<Color> defaultDarkColors = Arrays.asList(
        Color.decode("#DC5A96"), // Dark Hot Pink
        Color.decode("#375ABE"), // Dark Royal Blue
        Color.decode("#28AF28"), // Dark Lime Green
        Color.decode("#DC8C00"), // Dark Orange
        Color.decode("#7828BE"), // Dark Blue Violet
        Color.decode("#1978D2")  // Dark Dodger Blue
    );
    
    private final List<ColorPanel> lightColorPanels = new ArrayList<>();
    private final List<ColorPanel> darkColorPanels = new ArrayList<>();
    @Override
    public String getDisplayName() {
        return "Neon Brackets";
    }

    @Override
    public JComponent createComponent() {
        // Add light and dark color panels to their respective lists
        lightColorPanels.add(light1ColorPanel);
        lightColorPanels.add(light2ColorPanel);
        lightColorPanels.add(light3ColorPanel);
        lightColorPanels.add(light4ColorPanel);
        lightColorPanels.add(light5ColorPanel);
        lightColorPanels.add(light6ColorPanel);
        
        darkColorPanels.add(dark1ColorPanel);
        darkColorPanels.add(dark2ColorPanel);
        darkColorPanels.add(dark3ColorPanel);
        darkColorPanels.add(dark4ColorPanel);
        darkColorPanels.add(dark5ColorPanel);
        darkColorPanels.add(dark6ColorPanel);
        
        // Setup reset button action
        resetToDefaultsButton.addActionListener(e -> resetToDefaults());

        // Load current settings
        reset();
        
        return panel;
    }

    @Override
    public boolean isModified() {
        NeonBracketsState settings = NeonBracketsFactory.Companion.getInstance().getState();

        if (enableNeonBracketsCheckBox.isSelected() != settings.enabled) return true;
        if (roundBracketsCheckBox.isSelected() != settings.enableRoundBrackets) return true;
        if (curlyBracketsCheckBox.isSelected() != settings.enableCurlyBrackets) return true;
        if (angleBracketsCheckBox.isSelected() != settings.enableAngleBrackets) return true;
        if (squareBracketsCheckBox.isSelected() != settings.enableSquareBrackets) return true;

        // Check if any color has been modified
        for (int i = 0; i < lightColorPanels.size(); i++) {
            if (i < settings.bracketColorsLight.size()) {
                Color settingsColor;
                try {
                    settingsColor = Color.decode(settings.bracketColorsLight.get(i));
                } catch (Exception e) {
                    settingsColor = null;
                }

                if (!colorEquals(settingsColor, lightColorPanels.get(i).getSelectedColor())) {
                    return true;
                }
            }
        }

        for (int i = 0; i < darkColorPanels.size(); i++) {
            if (i < settings.bracketColorsDark.size()) {
                Color settingsColor;
                try {
                    settingsColor = Color.decode(settings.bracketColorsDark.get(i));
                } catch (Exception e) {
                    settingsColor = null;
                }

                if (!colorEquals(settingsColor, darkColorPanels.get(i).getSelectedColor())) {
                    return true;
                }
            }
        }

        if (!excludedFileTypesTextField.getText().equals(settings.excludedFileTypes)) return true;
        if (skipCommentsAndStringsCheckBox.isSelected() != settings.skipCommentsAndStrings) return true;

        return false;
    }
    
    private boolean colorEquals(Color c1, Color c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        return c1.equals(c2);
    }

    @Override
    public void apply() {
        NeonBracketsState settings = NeonBracketsFactory.Companion.getInstance().getState();

        settings.enabled = enableNeonBracketsCheckBox.isSelected();
        settings.enableRoundBrackets = roundBracketsCheckBox.isSelected();
        settings.enableCurlyBrackets = curlyBracketsCheckBox.isSelected();
        settings.enableAngleBrackets = angleBracketsCheckBox.isSelected();
        settings.enableSquareBrackets = squareBracketsCheckBox.isSelected();

        // Update color lists
        List<String> lightColors = new ArrayList<>();
        for (ColorPanel panel : lightColorPanels) {
            Color color = panel.getSelectedColor();
            if (color != null) {
                lightColors.add(String.format(Constants.HEX_COLOR_TEMPLATE, color.getRed(), color.getGreen(), color.getBlue()));
            }
        }
        settings.bracketColorsLight = lightColors;

        List<String> darkColors = new ArrayList<>();
        for (ColorPanel panel : darkColorPanels) {
            Color color = panel.getSelectedColor();
            if (color != null) {
                darkColors.add(String.format(Constants.HEX_COLOR_TEMPLATE, color.getRed(), color.getGreen(), color.getBlue()));
            }
        }
        settings.bracketColorsDark = darkColors;

        settings.excludedFileTypes = excludedFileTypesTextField.getText();
        settings.skipCommentsAndStrings = skipCommentsAndStringsCheckBox.isSelected();

        // Refresh all open editors
        ApplicationManager.getApplication().invokeLater(this::refreshAllEditors);
    }

    @Override
    public void reset() {
        NeonBracketsState settings = NeonBracketsFactory.Companion.getInstance().getState();

        enableNeonBracketsCheckBox.setSelected(settings.enabled);
        roundBracketsCheckBox.setSelected(settings.enableRoundBrackets);
        curlyBracketsCheckBox.setSelected(settings.enableCurlyBrackets);
        angleBracketsCheckBox.setSelected(settings.enableAngleBrackets);
        squareBracketsCheckBox.setSelected(settings.enableSquareBrackets);

        // Set colors
        for (int i = 0; i < lightColorPanels.size(); i++) {
            if (i < settings.bracketColorsLight.size()) {
                try {
                    Color color = Color.decode(settings.bracketColorsLight.get(i));
                    lightColorPanels.get(i).setSelectedColor(color);
                } catch (Exception e) {
                    // Use default color if parsing fails
                    lightColorPanels.get(i).setSelectedColor(defaultLightColors.get(i % defaultLightColors.size()));
                }
            } else {
                // Use default color if not enough colors in settings
                lightColorPanels.get(i).setSelectedColor(defaultLightColors.get(i % defaultLightColors.size()));
            }
        }

        for (int i = 0; i < darkColorPanels.size(); i++) {
            if (i < settings.bracketColorsDark.size()) {
                try {
                    Color color = Color.decode(settings.bracketColorsDark.get(i));
                    darkColorPanels.get(i).setSelectedColor(color);
                } catch (Exception e) {
                    // Use default color if parsing fails
                    darkColorPanels.get(i).setSelectedColor(defaultDarkColors.get(i % defaultDarkColors.size()));
                }
            } else {
                // Use default color if not enough colors in settings
                darkColorPanels.get(i).setSelectedColor(defaultDarkColors.get(i % defaultDarkColors.size()));
            }
        }

        excludedFileTypesTextField.setText(settings.excludedFileTypes);
        skipCommentsAndStringsCheckBox.setSelected(settings.skipCommentsAndStrings);
    }

    @Override
    public void disposeUIResources() {
        // No need to clean up any resources
    }

    private void resetToDefaults() {
        // Reset bracket types
        roundBracketsCheckBox.setSelected(true);
        curlyBracketsCheckBox.setSelected(true);
        angleBracketsCheckBox.setSelected(true);
        squareBracketsCheckBox.setSelected(true);

        // Reset colors to defaults
        for (int i = 0; i < lightColorPanels.size(); i++) {
            lightColorPanels.get(i).setSelectedColor(defaultLightColors.get(i % defaultLightColors.size()));
        }

        for (int i = 0; i < darkColorPanels.size(); i++) {
            darkColorPanels.get(i).setSelectedColor(defaultDarkColors.get(i % defaultDarkColors.size()));
        }
    }

    private void refreshAllEditors() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        var editors = editorFactory.getAllEditors();

        for (var editor : editors) {
            // Remove existing highlighters
            var existingHighlighters = editor.getUserData(NeonBracketsPluginKt.getBRACKET_HIGHLIGHTERS());
            if (existingHighlighters != null) {
                existingHighlighters.forEach(it -> {
                    try {
                        it.dispose();
                    } catch (Exception e) {
                        // Silent exception handling
                    }
                });
            }

            // Apply highlighting with current settings
            NeonBracketsPluginKt.highlightBracketsInEditor(editor);
        }
    }
}
