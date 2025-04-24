package tech.zimin.neonBrackets.neonBrackets

import com.intellij.ui.ColorPicker
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JPanel

/**
 * A custom color panel with rounded corners and centered hex value
 */
class RoundedColorPanel : JPanel() {
    private var selectedColor: Color = JBColor.WHITE
    private val listeners = mutableListOf<ActionListener>()

    init {
        // Set preferred size
        preferredSize = Dimension(120, 28)
        minimumSize = Dimension(100, 28)

        // Make the panel non-opaque for better rendering
        isOpaque = false

        // Add mouse listener to handle clicks
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (isEnabled) {
                    showColorPicker()
                }
            }
        })
    }

    private fun showColorPicker() {
        val color = ColorPicker.showDialog(
            this,
            "Choose Color",
            selectedColor,
            true,
            null,
            false
        )

        if (color != null) {
            setSelectedColor(color)
            // Notify listeners
            val event = ActionEvent(this, ActionEvent.ACTION_PERFORMED, "colorChanged")
            for (listener in listeners) {
                listener.actionPerformed(event)
            }
        }
    }

    fun getSelectedColor(): Color? = selectedColor

    fun setSelectedColor(color: Color?) {
        if (color != null) {
            selectedColor = color
            repaint()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Create rounded rectangle with 8px corner radius
        val roundRect = RoundRectangle2D.Float(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            8f, 8f
        )

        // Fill with selected color
        g2d.color = selectedColor
        g2d.fill(roundRect)

        // Draw border
        g2d.color = JBColor.border()
        g2d.draw(roundRect)

        // Draw hex value in the center
        val hexColor = String.format(hexColorTemplate, selectedColor.red, selectedColor.green, selectedColor.blue)

        // Use a slightly larger font to make the text more visible
        val originalFont = g2d.font
        g2d.font = originalFont.deriveFont(originalFont.size2D * 1.1f)

        val fontMetrics = g2d.fontMetrics

        // Determine text color (white for dark backgrounds, black for light backgrounds)
        val textColor = if (ColorUtil.isDark(selectedColor)) JBColor.WHITE else JBColor.BLACK
        g2d.color = textColor

        // Center the text precisely
        val textWidth = fontMetrics.stringWidth(hexColor)

        // Calculate exact center position
        val x = (width - textWidth) / 2
        val y = height / 2 + (fontMetrics.ascent - fontMetrics.descent) / 2

        g2d.drawString(hexColor, x, y)

        // Restore original font
        g2d.font = originalFont
    }
}