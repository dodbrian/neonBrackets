package tech.zimin.neonbrackets.settings

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