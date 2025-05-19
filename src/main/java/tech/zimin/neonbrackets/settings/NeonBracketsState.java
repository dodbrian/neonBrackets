package tech.zimin.neonbrackets.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings state for the plugin.
 */
public class NeonBracketsState {
    // Public fields to match Kotlin's property access pattern
    public boolean enabled = true;

    // Bracket type toggles
    public boolean enableRoundBrackets = true;
    public boolean enableCurlyBrackets = true;
    public boolean enableAngleBrackets = true;
    public boolean enableSquareBrackets = true;

    // Colors (stored as hex strings)
    public List<String> bracketColorsLight = new ArrayList<>(Arrays.asList(
        "#FF69B4", // Hot Pink
        "#4169E1", // Royal Blue
        "#32CD32", // Lime Green
        "#FFA500", // Orange
        "#8A2BE2", // Blue Violet
        "#1E90FF"  // Dodger Blue
    ));

    public List<String> bracketColorsDark = new ArrayList<>(Arrays.asList(
        "#DC5A96", // Dark Hot Pink
        "#375ABE", // Dark Royal Blue
        "#28AF28", // Dark Lime Green
        "#DC8C00", // Dark Orange
        "#7828BE", // Dark Blue Violet
        "#1978D2"  // Dark Dodger Blue
    ));

    // Excluded file types (comma-separated)
    public String excludedFileTypes = "";

    // Skip comments and strings
    public boolean skipCommentsAndStrings = true;

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableRoundBrackets() {
        return enableRoundBrackets;
    }

    public void setEnableRoundBrackets(boolean enableRoundBrackets) {
        this.enableRoundBrackets = enableRoundBrackets;
    }

    public boolean isEnableCurlyBrackets() {
        return enableCurlyBrackets;
    }

    public void setEnableCurlyBrackets(boolean enableCurlyBrackets) {
        this.enableCurlyBrackets = enableCurlyBrackets;
    }

    public boolean isEnableAngleBrackets() {
        return enableAngleBrackets;
    }

    public void setEnableAngleBrackets(boolean enableAngleBrackets) {
        this.enableAngleBrackets = enableAngleBrackets;
    }

    public boolean isEnableSquareBrackets() {
        return enableSquareBrackets;
    }

    public void setEnableSquareBrackets(boolean enableSquareBrackets) {
        this.enableSquareBrackets = enableSquareBrackets;
    }

    public List<String> getBracketColorsLight() {
        return bracketColorsLight;
    }

    public void setBracketColorsLight(List<String> bracketColorsLight) {
        this.bracketColorsLight = bracketColorsLight;
    }

    public List<String> getBracketColorsDark() {
        return bracketColorsDark;
    }

    public void setBracketColorsDark(List<String> bracketColorsDark) {
        this.bracketColorsDark = bracketColorsDark;
    }

    public String getExcludedFileTypes() {
        return excludedFileTypes;
    }

    public void setExcludedFileTypes(String excludedFileTypes) {
        this.excludedFileTypes = excludedFileTypes;
    }

    public boolean isSkipCommentsAndStrings() {
        return skipCommentsAndStrings;
    }

    public void setSkipCommentsAndStrings(boolean skipCommentsAndStrings) {
        this.skipCommentsAndStrings = skipCommentsAndStrings;
    }
}
