package com.nebulacrak.gui;

import com.nebulacrak.gui.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Botón de entrada de color HEX/RGB.
 * Permite escribir un código de color hexadecimal (#RRGGBB o #RRGGBBAA).
 */
public class ColorInputButton extends AbstractWidget {
    private final Consumer<Integer> onColorChange;
    private String text;
    private boolean focused;
    private final int maxLength = 9; // #RRGGBBAA
    private int cursorPosition = 0;
    private long lastBlinkTime = 0;
    private boolean showCursor = true;

    public ColorInputButton(int x, int y, int width, int height, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, width, height, Component.literal("#" + String.format("%08X", initialColor).substring(2)));
        this.onColorChange = onColorChange;
        this.text = "#" + String.format("%08X", initialColor).substring(2);
        this.cursorPosition = text.length();
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        this.focused = isMouseOver(event.x(), event.y());
        this.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (!focused) return false;

        int keyCode = event.key();
        boolean ctrl = event.hasControlDown();

        // Handle special keys
        if (keyCode == 259) { // BACKSPACE
            if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
                updateColor();
            }
            return true;
        } else if (keyCode == 257) { // ENTER
            focused = false;
            return true;
        } else if (keyCode == 263) { // LEFT
            cursorPosition = Math.max(1, cursorPosition - 1); // Keep # at position 0
            return true;
        } else if (keyCode == 262) { // RIGHT
            cursorPosition = Math.min(text.length(), cursorPosition + 1);
            return true;
        } else if (keyCode == 268) { // HOME
            cursorPosition = 1;
            return true;
        } else if (keyCode == 269) { // END
            cursorPosition = text.length();
            return true;
        } else if (keyCode == 86 && ctrl) { // CTRL+V - Paste
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (!focused) return false;
        int codepoint = event.codepoint();
        char c = (char) codepoint;
        if (isValidHexChar(c) && text.length() < maxLength) {
            text = text.substring(0, cursorPosition) + Character.toUpperCase(c) + text.substring(cursorPosition);
            cursorPosition++;
            updateColor();
            return true;
        }
        return false;
    }

    private char getCharFromKey(int keyCode) {
        // Map GLFW key codes to characters
        if (keyCode >= 48 && keyCode <= 57) return (char) ('0' + (keyCode - 48)); // 0-9
        if (keyCode >= 65 && keyCode <= 90) return (char) ('A' + (keyCode - 65)); // A-Z
        if (keyCode >= 290 && keyCode <= 301) return (char) ('0' + (keyCode - 290)); // Keypad 0-9
        return 0;
    }

    private boolean isValidHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private void updateColor() {
        try {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            // Pad to 8 characters (RRGGBBAA)
            while (hex.length() < 8) hex += "0";
            hex = hex.substring(0, 8);
            int color = (int) Long.parseLong(hex, 16);
            onColorChange.accept(color);
            // Update text to show full 8-char hex
            this.text = "#" + String.format("%08X", color).substring(2);
        } catch (Exception e) {
            // Invalid color, ignore
        }
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        Font font = FontManager.getCurrentFont();
        boolean hovered = isMouseOver(mouseX, mouseY);

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Background
        int bgColor = focused ? 0xFF2A2A3A : (hovered ? 0xFF22222E : 0xFF181822);
        int borderColor = focused ? 0xFF8B5CF6 : (hovered ? 0xFF4A4A60 : 0xFF262634);
        int textColor = 0xFFFFFFFF;

        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);
        context.fill(x, y, x + w, y + 1, borderColor);
        context.fill(x, y + h - 1, x + w, y + h, borderColor);
        context.fill(x, y + 1, x + 1, y + h - 1, borderColor);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor);

        // Color preview swatch on the left
        int swatchSize = h - 4;
        int swatchX = x + 2;
        int swatchY = y + 2;
        try {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            while (hex.length() < 8) hex += "0";
            hex = hex.substring(0, 8);
            int color = (int) Long.parseLong(hex, 16);
            context.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, color);
            // Border around swatch
            context.fill(swatchX - 1, swatchY - 1, swatchX + swatchSize + 1, swatchY, 0xFF555565);
            context.fill(swatchX - 1, swatchY + swatchSize, swatchX + swatchSize + 1, swatchY + swatchSize + 1, 0xFF555565);
            context.fill(swatchX - 1, swatchY, swatchX, swatchY + swatchSize, 0xFF555565);
            context.fill(swatchX + swatchSize, swatchY, swatchX + swatchSize + 1, swatchY + swatchSize, 0xFF555565);
        } catch (Exception ignored) {
        }

        // Text input
        int textX = swatchX + swatchSize + 4;
        int textY = y + (h - 8) / 2;
        String displayText = text;
        
        // Blinking cursor
        long now = System.currentTimeMillis();
        if (now - lastBlinkTime > 500) {
            showCursor = !showCursor;
            lastBlinkTime = now;
        }
        
        if (focused && showCursor) {
            // Calculate cursor position
            String beforeCursor = displayText.substring(0, Math.min(cursorPosition, displayText.length()));
            int cursorX = textX + font.width(beforeCursor);
            context.fill(cursorX, textY, cursorX + 1, textY + 8, 0xFFFFFFFF);
        }

        context.drawString(font, displayText, textX, textY, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.cursorPosition = text.length();
        updateColor();
    }
}