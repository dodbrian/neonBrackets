package tech.zimin.neonbrackets.settings;

import com.intellij.application.options.colors.ColorAndFontSettingsListener;
import com.intellij.application.options.colors.OptionsPanel;
import com.intellij.ui.ColorPanel;

import javax.swing.*;
import java.util.Set;

public class NeonBracketsSettings implements OptionsPanel {
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

    @Override
    public void addListener(ColorAndFontSettingsListener colorAndFontSettingsListener) {
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void updateOptionsList() {

    }

    @Override
    public Runnable showOption(String s) {
        return null;
    }

    @Override
    public void applyChangesToScheme() {
    }

    @Override
    public void selectOption(String s) {
    }

    @Override
    public Set<String> processListOptions() {
        return Set.of();
    }
}
