package tech.zimin.neonbrackets

import java.util.concurrent.ConcurrentHashMap

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

val BRACKET_HIGHLIGHTERS = Key<List<RangeHighlighter>>("NEON_BRACKET_HIGHLIGHTERS")
val SKIP_BRACKET_HIGHLIGHTING = Key<Boolean>("NEON_SKIP_BRACKET_HIGHLIGHTING")
val DOCUMENT_LISTENER = Key<DocumentListener>("NEON_DOCUMENT_LISTENER")
val SELECTION_LISTENER = Key<SelectionListener>("NEON_SELECTION_LISTENER")

// Cache structures for performance optimization
val QUOTE_REGIONS_CACHE = ConcurrentHashMap<String, List<IntRange>>()
val COMMENT_STRING_CACHE = ConcurrentHashMap<Int, Boolean>()
val LAST_CACHE_CLEANUP_TIME = Key<Long>("NEON_LAST_CACHE_CLEANUP_TIME")
const val CACHE_CLEANUP_INTERVAL_MS = 30000 // 30 seconds
const val COMMENT_STRING_CACHE_MAX_SIZE = 10000 // Prevent cache from growing too large

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
private fun isFileTypeExcluded(file: VirtualFile): Boolean {
    val settings = NeonBracketsFactory.getInstance().state
    if (settings.excludedFileTypes.isBlank()) return false

    val excludedTypes = settings.excludedFileTypes.split(",").map { it.trim().lowercase() }

    // Get the actual file extension
    val extension = file.extension?.lowercase() ?: ""

    println("[NeonBrackets] Checking if file extension '$extension' is in excluded types: $excludedTypes")

    return extension.isNotEmpty() && excludedTypes.contains(extension)
}

/**
 * Maximum number of brackets to highlight to avoid performance issues
 * Set significantly higher to ensure full file coverage while still preventing memory issues
 */
private const val MAX_BRACKETS_TO_HIGHLIGHT = 25000

/**
 * Directly highlight brackets in the editor without using a highlighting pass.
 * Optimized for performance with bracket count limits and visible area focus.
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
            // Process the document with optimizations
            val document = editor.document
            val bracketColors = getBracketColors()
            val activeBracketPairs = getActiveBracketPairs()

            // Skip if no bracket types are enabled
            if (activeBracketPairs.isEmpty()) {
                return
            }

            val newHighlighters = mutableListOf<RangeHighlighter>()
            
            // Focus on visible area with some margin for better performance
            val scrollingModel = editor.scrollingModel
            val visibleArea = scrollingModel.visibleArea
            val visibleStartLine = editor.xyToLogicalPosition(visibleArea.location).line
            val visibleEndLine = editor.xyToLogicalPosition(
                java.awt.Point(visibleArea.x, visibleArea.y + visibleArea.height)
            ).line
            
            // Process the entire document for small to medium files, use margins for very large files
            // This hybrid approach ensures complete highlighting for most files while maintaining performance
            val totalLineCount = document.lineCount
            val isLargeFile = totalLineCount > 10000 // Only use margin approach for very large files
            
            val startLine: Int
            val endLine: Int
            
            if (isLargeFile) {
                // For very large files, use visible area + large margins
                val marginLines = 1000 // Much larger margin to ensure complete coverage
                startLine = (visibleStartLine - marginLines).coerceAtLeast(0)
                endLine = (visibleEndLine + marginLines).coerceAtMost(totalLineCount - 1)
            } else {
                // For small to medium files, just process the whole document
                startLine = 0
                endLine = totalLineCount - 1
            }
            
            // Get text for the area we're processing
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)
            val text = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
            
            // Get the PsiFile for checking comments and strings (only if needed)
            val psiFile = if (settings.skipCommentsAndStrings) {
                val project = editor.project
                project?.let { PsiDocumentManager.getInstance(it).getPsiFile(document) }
            } else {
                null
            }

            // Map to store bracket pairs for efficient matching
            val bracketStacks = mutableMapOf<Char, MutableList<Int>>()
            activeBracketPairs.forEach { (open, _) -> bracketStacks[open] = mutableListOf() }
            
            // First pass: Determine comment and string regions
            val commentRegions = mutableListOf<IntRange>()
            var inBlockComment = false
            var inLineComment = false
            var inStringLiteral = false
            var escapeNext = false
            var index = 0
            
            while (index < text.length) {
                if (inBlockComment) {
                    // Look for end of block comment
                    if (index < text.length - 1 && text[index] == '*' && text[index + 1] == '/') {
                        inBlockComment = false
                        index += 2
                    } else {
                        index++
                    }
                } else if (inLineComment) {
                    // Look for end of line
                    if (text[index] == '\n') {
                        inLineComment = false
                    }
                    index++
                } else if (inStringLiteral) {
                    // Handle escape sequences in strings
                    if (escapeNext) {
                        escapeNext = false
                    } else if (text[index] == '\\') {
                        escapeNext = true
                    } else if (text[index] == '"') {
                        inStringLiteral = false
                    }
                    index++
                } else {
                    // Check for comment or string start
                    if (index < text.length - 1 && text[index] == '/' && text[index + 1] == '*') {
                        val commentStart = startOffset + index
                        inBlockComment = true
                        index += 2
                        
                        // Find end of block comment
                        var commentEnd = index
                        while (commentEnd < text.length - 1) {
                            if (text[commentEnd] == '*' && text[commentEnd + 1] == '/') {
                                commentEnd += 2
                                break
                            }
                            commentEnd++
                        }
                        
                        if (commentEnd <= text.length) {
                            commentRegions.add(IntRange(commentStart, startOffset + commentEnd - 1))
                        }
                        
                    } else if (index < text.length - 1 && text[index] == '/' && text[index + 1] == '/') {
                        val commentStart = startOffset + index
                        inLineComment = true
                        index += 2
                        
                        // Find end of line comment
                        var commentEnd = index
                        while (commentEnd < text.length) {
                            if (text[commentEnd] == '\n') {
                                break
                            }
                            commentEnd++
                        }
                        
                        if (commentEnd <= text.length) {
                            commentRegions.add(IntRange(commentStart, startOffset + commentEnd - 1))
                        }
                        
                    } else if (text[index] == '"') {
                        inStringLiteral = true
                        index++
                    } else {
                        index++
                    }
                }
            }
            
            // Process each character in the visible range (plus margin)
            var bracketCount = 0
            for (relativePos in text.indices) {
                val absolutePos = startOffset + relativePos
                val char = text[relativePos]
                
                // Stop if we've reached our limit to prevent memory issues
                if (bracketCount >= MAX_BRACKETS_TO_HIGHLIGHT) {
                    break
                }

                // Check if this position is in a comment or string region
                val inCommentOrString = commentRegions.any { absolutePos in it } ||
                    (psiFile != null && isInCommentOrString(psiFile, absolutePos))
                    
                if (inCommentOrString) {
                    continue
                }

                // Skip single quotes
                if (isInSingleQuotes(text, relativePos)) {
                    continue
                }

                // Check for brackets
                for ((openChar, closeChar) in activeBracketPairs) {
                    // For angle brackets, check if it's a generic (optimization: only if angle brackets are enabled)
                    if ((char == '<' || char == '>') && 
                        !isGenericByCharacterContext(text, relativePos)) {
                        continue
                    }

                    if (char == openChar) {
                        bracketStacks[openChar]?.add(absolutePos)
                        break
                    } else if (char == closeChar && bracketStacks[openChar]?.isNotEmpty() == true) {
                        val openPos = bracketStacks[openChar]?.removeAt(bracketStacks[openChar]?.size!! - 1) ?: continue

                        // Calculate nesting level for color
                        val nestingLevel = bracketStacks[openChar]?.size ?: 0
                        val colorIndex = nestingLevel % bracketColors.size
                        val color = bracketColors[colorIndex]

                        // Add highlighters for both brackets
                        addHighlighter(editor, openPos, color, newHighlighters)
                        addHighlighter(editor, absolutePos, color, newHighlighters)
                        bracketCount += 2
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
 * Check if the position is within single quotes. Optimized with caching.
 */
fun isInSingleQuotes(text: String, offset: Int): Boolean {
    // For very small texts, use the direct approach without caching
    if (text.length < 1000) {
        var inSingleQuotes = false
        for (i in 0 until offset) {
            if (text[i] == '\'' && (i == 0 || text[i - 1] != '\\')) {
                inSingleQuotes = !inSingleQuotes
            }
        }
        return inSingleQuotes
    }
    
    // Generate a unique key for this text (use hashCode since we only need equality comparison)
    val textKey = text.hashCode().toString()
    
    // Check if we have cached quote regions for this text
    var quoteRegions = QUOTE_REGIONS_CACHE.get(textKey)
    
    // If not in cache, compute all quoted regions and cache them
    if (quoteRegions == null) {
        val regions = mutableListOf<IntRange>()
        var start = -1
        
        for (i in text.indices) {
            if (text[i] == '\'' && (i == 0 || text[i - 1] != '\\')) {
                if (start == -1) {
                    // Opening quote
                    start = i
                } else {
                    // Closing quote
                    regions.add(IntRange(start, i))
                    start = -1
                }
            }
        }
        
        // Handle unclosed quote
        if (start != -1) {
            regions.add(IntRange(start, text.length - 1))
        }
        
        quoteRegions = regions
        
        // Only cache if not too large (prevent memory issues)
        if (QUOTE_REGIONS_CACHE.size < 100) {
            QUOTE_REGIONS_CACHE.put(textKey, quoteRegions)
        }
    }
    
    // Check if offset falls within any quoted region
    for (range in quoteRegions) {
        if (offset > range.first && offset <= range.last) {
            return true
        }
    }
    
    return false
}

/**
 * Check if the position is within a comment or string with caching for better performance.
 */
private fun isInCommentOrString(psiFile: PsiFile, offset: Int): Boolean {
    // Use the cache key as the hash of the PSI file plus offset
    val cacheKey = (psiFile.hashCode() * 31 + offset)
    
    // Check if we have this result in the cache
    val cachedResult = COMMENT_STRING_CACHE[cacheKey]
    if (cachedResult != null) {
        return cachedResult
    }
    
    // Clean up cache periodically to prevent memory bloat
    cleanupCacheIfNeeded(psiFile)
    
    // If not in cache, do the expensive PSI operation
    val element = psiFile.findElementAt(offset)
    val result = element != null && (PsiTreeUtil.getParentOfType(
        element, PsiComment::class.java
    ) != null || element.node?.elementType.toString().contains("STRING") || element.node?.elementType.toString()
        .contains("COMMENT"))
    
    // Only cache if the cache isn't too large
    if (COMMENT_STRING_CACHE.size < COMMENT_STRING_CACHE_MAX_SIZE) {
        COMMENT_STRING_CACHE[cacheKey] = result
    }
    
    return result
}

/**
 * Clean up the comment/string cache periodically to prevent memory bloat.
 */
private fun cleanupCacheIfNeeded(psiFile: PsiFile) {
    val project = psiFile.project
    val lastCleanup = project.getUserData(LAST_CACHE_CLEANUP_TIME) ?: 0L
    val currentTime = System.currentTimeMillis()
    
    // Only clean up if enough time has passed since last cleanup
    if (currentTime - lastCleanup > CACHE_CLEANUP_INTERVAL_MS) {
        COMMENT_STRING_CACHE.clear()
        project.putUserData(LAST_CACHE_CLEANUP_TIME, currentTime)
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
            position, position + 1, HighlighterLayer.SELECTION - 1,
            TextAttributes(color, null, null, null, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE
        )
        highlighters.add(highlighter)
    } catch (_: Exception) {
        // Silent exception handling
    }
}

/**
 * Check if a color is grayed out (i.e., the red, green, and blue components are similar).
 */
private fun isGrayedOut(color: Color): Boolean {
    val threshold = 10 // Adjust this value as needed
    val red = color.red
    val green = color.green
    val blue = color.blue
    return Math.abs(red - green) <= threshold && Math.abs(red - blue) <= threshold && Math.abs(green - blue) <= threshold
}

/**
 * Cache for angle bracket analysis results
 */
private val ANGLE_BRACKET_CACHE = ConcurrentHashMap<Int, Boolean>()

/**
 * Determines if an angle bracket at the given position is used for generics rather than as an operator.
 * Optimized version that prefers character context analysis over expensive PSI operations.
 */
private fun isGenericAngleBracket(psiFile: PsiFile?, offset: Int): Boolean {
    if (psiFile == null) return true // If we can't determine, default to highlighting
    
    // Check if we have a cached result
    val cacheKey = offset + (psiFile.hashCode() * 31)
    val cachedResult = ANGLE_BRACKET_CACHE[cacheKey]
    if (cachedResult != null) {
        return cachedResult
    }
    
    // Get document text
    val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return true
    if (offset < 0 || offset >= document.textLength) return false
    
    val text = document.text
    val char = text[offset]
    
    // Only process angle brackets
    if (char != '<' && char != '>') return false
    
    // Start with character context analysis which is much faster
    val result = isGenericByCharacterContext(text, offset)
    
    // Only cache if the cache isn't too large
    if (ANGLE_BRACKET_CACHE.size < 10000) {
        ANGLE_BRACKET_CACHE[cacheKey] = result
    }
    
    return result
}

/**
 * Fallback method that determines if an angle bracket is likely a generic based on surrounding characters.
 */
private fun isGenericByCharacterContext(text: String, position: Int): Boolean {
    val char = text[position]
    
    // Check for comparison operators
    if (char == '<') {
        // Check if it's a comparison operator (typically has spaces around it or is part of <=)
        if (position > 0 && position < text.length - 1) {
            // Check for <= operator
            if (position < text.length - 1 && text[position + 1] == '=') {
                return false
            }
            
            // Check for space before and after, which typically indicates a comparison operator
            val spaceBefore = position > 0 && text[position - 1].isWhitespace()
            val spaceAfter = position < text.length - 1 && text[position + 1].isWhitespace()
            
            if (spaceBefore && spaceAfter) {
                return false
            }
            
            // Check for number or boolean literal before, which typically indicates a comparison
            if (position > 0) {
                val prevChar = text[position - 1]
                if (prevChar.isDigit() || 
                    (position > 5 && text.substring(position - 5, position).contains("true")) ||
                    (position > 6 && text.substring(position - 6, position).contains("false"))) {
                    return false
                }
            }
            
            // Check for identifier before and letter after, which typically indicates a generic
            val hasIdentifierBefore = position > 0 && (text[position - 1].isLetterOrDigit() || 
                                                     text[position - 1] == '_' || 
                                                     text[position - 1] == '.')
            
            // If it has an identifier before, it's likely a generic
            if (hasIdentifierBefore) {
                return true
            }
        }
    } else if (char == '>') {
        // Check if it's a comparison operator (typically has spaces around it or is part of >=)
        if (position > 0 && position < text.length - 1) {
            // Check for >= operator
            if (position > 0 && text[position - 1] == '=') {
                return false
            }
            
            // Check for space before and after, which typically indicates a comparison operator
            val spaceBefore = position > 0 && text[position - 1].isWhitespace()
            val spaceAfter = position < text.length - 1 && text[position + 1].isWhitespace()
            
            if (spaceBefore && spaceAfter) {
                return false
            }
            
            // Check for number or boolean literal before, which typically indicates a comparison
            if (position < text.length - 1) {
                val nextChar = text[position + 1]
                if (nextChar.isDigit() || 
                    (position < text.length - 5 && text.substring(position + 1, position + 6).contains("true")) ||
                    (position < text.length - 6 && text.substring(position + 1, position + 7).contains("false"))) {
                    return false
                }
            }
        }
    }
    
    // Default to treating it as a generic if we can't determine it's an operator
    return true
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
 * Clear all existing highlighters from the editor and clean up caches.
 */
fun clearHighlighters(editor: Editor) {
    // Clean up highlighters
    val existingHighlighters = editor.getUserData(BRACKET_HIGHLIGHTERS)
    if (existingHighlighters != null) {
        for (highlighter in existingHighlighters) {
            try {
                highlighter.dispose()
            } catch (_: Exception) {
                // Silent exception handling
            }
        }
    }

    editor.putUserData(BRACKET_HIGHLIGHTERS, null)
    
    // Clean up associated caches
    cleanupCaches(editor)
}

/**
 * Clean up any caches associated with this editor to prevent memory leaks.
 * Should be called when an editor is closed or the plugin is disabled.
 */
private fun cleanupCaches(editor: Editor) {
    val document = editor.document
    val text = document.text
    
    // Clean up quote regions cache for this document
    val textKey = text.hashCode().toString()
    QUOTE_REGIONS_CACHE.remove(textKey)
    
    // Clean up comment/string cache entries related to this file
    val file = FileDocumentManager.getInstance().getFile(document)
    if (file != null && editor.project != null) {
        val psiFile = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(document)
        if (psiFile != null) {
            val fileHash = psiFile.hashCode()
            
            // Remove comment string cache entries for this file
            val keysToRemove = COMMENT_STRING_CACHE.keys().asSequence()
                .filter { it.toString().startsWith(fileHash.toString()) }
                .toList()
                
            for (key in keysToRemove) {
                COMMENT_STRING_CACHE.remove(key)
            }
            
            // Clean angle bracket cache entries
            val angleKeysToRemove = ANGLE_BRACKET_CACHE.keys().asSequence()
                .filter { (it - fileHash * 31) in 0 until document.textLength }
                .toList()
                
            for (key in angleKeysToRemove) {
                ANGLE_BRACKET_CACHE.remove(key)
            }
        }
    }
    
    // If caches are getting too large, trigger a more aggressive cleanup
    if (QUOTE_REGIONS_CACHE.size > 50 || 
        COMMENT_STRING_CACHE.size > COMMENT_STRING_CACHE_MAX_SIZE / 2 || 
        ANGLE_BRACKET_CACHE.size > 5000) {
        purgeAllCaches()
    }
}

/**
 * Purge all caches when memory pressure is high
 */
private fun purgeAllCaches() {
    QUOTE_REGIONS_CACHE.clear()
    COMMENT_STRING_CACHE.clear()
    ANGLE_BRACKET_CACHE.clear()
}
