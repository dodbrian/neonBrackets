package tech.zimin.neonBrackets.neonBrackets

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

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
                    // For angle brackets, only process them if they're used for generics
                    if ((openChar == '<' || closeChar == '>') && !isGenericAngleBracket(text, i)) {
                        continue
                    }
                    
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
 * Add a highlighter for a bracket.
 */
private fun addHighlighter(
    editor: Editor, position: Int, color: JBColor, highlighters: MutableList<RangeHighlighter>
) {
    try {
        val highlighter = editor.markupModel.addRangeHighlighter(
            position, position + 1, HighlighterLayer.SELECTION - 1, // Just below selection layer
            TextAttributes(color, null, null, null, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE
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
    return element != null && (PsiTreeUtil.getParentOfType(
        element, PsiComment::class.java
    ) != null || element.node?.elementType.toString().contains("STRING") || element.node?.elementType.toString()
        .contains("COMMENT"))
}

/**
 * Determines if an angle bracket at the given position is likely used for generics rather than as an operator.
 * This function checks surrounding characters to make the determination.
 */
private fun isGenericAngleBracket(text: String, position: Int): Boolean {
    if (position < 0 || position >= text.length || (text[position] != '<' && text[position] != '>')) {
        return false
    }
    
    val char = text[position]
    
    // For opening angle bracket '<'
    if (char == '<') {
        // Check if it's preceded by an identifier (letter, digit, underscore, or dot)
        // and followed by a letter, which would indicate a generic type
        val hasIdentifierBefore = position > 0 && (text[position - 1].isLetterOrDigit() || 
                                                  text[position - 1] == '_' || 
                                                  text[position - 1] == '.')
        val hasIdentifierAfter = position < text.length - 1 && 
                               (text[position + 1].isLetter() || 
                                text[position + 1] == '?' ||  // For nullable types
                                text[position + 1] == '_' ||  // For type parameters
                                text[position + 1] == ' ' ||  // Space after opening bracket
                                text[position + 1] == '\t')   // Tab after opening bracket
        
        // Check for space in generic declarations like "List< T>"
        if (hasIdentifierBefore && position < text.length - 2 && 
            text[position + 1].isWhitespace() && text[position + 2].isLetter()) {
            return true
        }
        
        return hasIdentifierBefore && hasIdentifierAfter
    } 
    // For closing angle bracket '>'
    else {
        // Check if it's preceded by an identifier or another closing bracket
        // and followed by appropriate characters for end of a generic declaration
        val hasValidBefore = position > 0 && (text[position - 1].isLetterOrDigit() || 
                                            text[position - 1] == '_' || 
                                            text[position - 1] == '>' ||  // Nested generics
                                            text[position - 1] == ' ' ||  // Space before closing bracket
                                            text[position - 1] == '\t' || // Tab before closing bracket
                                            text[position - 1] == '?')    // For nullable types
        
        // Valid characters after a closing generic bracket
        val hasValidAfter = position == text.length - 1 || // End of text
                          (position < text.length - 1 && (
                           text[position + 1] == '(' ||    // Method call after generic
                           text[position + 1] == ')' ||    // End of parameter
                           text[position + 1] == ',' ||    // Parameter separator
                           text[position + 1] == '.' ||    // Method call on generic
                           text[position + 1] == ' ' ||    // Space after generic
                           text[position + 1] == '\t' ||   // Tab after generic
                           text[position + 1] == ';' ||    // End of statement
                           text[position + 1] == '{' ||    // Start of block
                           text[position + 1] == '>' ||    // Nested generics
                           text[position + 1] == '[' ||    // Array access
                           text[position + 1].isWhitespace()))
        
        return hasValidBefore && hasValidAfter
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

/**
 * Clear all existing highlighters from the editor.
 */
fun clearHighlighters(editor: Editor) {
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
