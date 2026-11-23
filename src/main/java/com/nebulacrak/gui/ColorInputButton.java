package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Campo de entrada de color HEX para Nebula.
 * Acepta #RRGGBB y #RRGGBBAA. Interno: ARGB 0xAARRGGBB.
 */
public class ColorInputButton extends AbstractWidget {
    private static final int MAX_HEX_DIGITS = 8;
    private static final int BORDER_NORMAL = 0xFF262634;
    private static final int BORDER_HOVER = 0xFF4A4A60;
    private static final int BORDER_FOCUSED = 0xFF8B5CF6;
    private static final int BG_NORMAL = 0xFF181822;
    private static final int BG_HOVER = 0xFF22222E;
    private static final int BG_FOCUSED = 0xFF2A2A3A;

    private final Consumer<Integer> onColorChange;
    private String hexDigits; // only hex chars, no '#'
    private int cursorPos;
    private int highlightPos;
    private int lastValidColor;
    private long lastBlinkTime = 0;
    private boolean showCursor = true;

    public ColorInputButton(int x, int y, int width, int height, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, width, height, Component.literal(colorToText(initialColor)));
        this.onColorChange = onColorChange;
        this.lastValidColor = initialColor;
        this.hexDigits = colorToHexDigits(initialColor);
        this.cursorPos = this.hexDigits.length();
        this.highlightPos = this.cursorPos;
        syncMessage();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (doubleClick) {
            selectAll();
        } else {
            int local = getTextIndexAt(event.x());
            setCursorPosition(local, hasShiftDown());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible) return false;
        boolean inside = this.isMouseOver(event.x(), event.y());
        if (this.isFocused() && !inside) {
            this.setFocused(false);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isFocused()) return false;

        if (event.isSelectAll()) {
            selectAll();
            return true;
        }
        if (event.isCopy()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(getHighlighted());
            return true;
        }
        if (event.isPaste()) {
            pasteFromClipboard();
            return true;
        }
        if (event.isCut()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(getHighlighted());
            deleteSelection();
            return true;
        }

        boolean shift = event.hasShiftDown();
        switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) deleteSelection();
                else deleteChars(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) deleteSelection();
                else deleteChars(1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveCursorBy(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCursorBy(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                setCursorPosition(0, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                setCursorPosition(hexDigits.length(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.setFocused(false);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.setFocused(false);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.isFocused()) return false;
        if (!event.isAllowedChatCharacter()) return false;

        String input = event.codepointAsString();
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '#') continue;
            if (isHex(c) && filtered.length() + selectionReplacedLength() < MAX_HEX_DIGITS) {
                filtered.append(Character.toUpperCase(c));
            }
        }
        if (filtered.isEmpty()) return true; // consumed but no valid hex
        insertHex(filtered.toString());
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        Font font = getFont();
        boolean hovered = this.isHovered();
        boolean focused = this.isFocused();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bg = focused ? BG_FOCUSED : (hovered ? BG_HOVER : BG_NORMAL);
        int border = focused ? BORDER_FOCUSED : (hovered ? BORDER_HOVER : BORDER_NORMAL);

        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y + 1, x + 1, y + h - 1, border);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, border);

        int swatchSize = h - 4;
        int swatchX = x + 2;
        int swatchY = y + 2;
        renderSwatch(context, swatchX, swatchY, swatchSize, lastValidColor);

        int textX = swatchX + swatchSize + 5;
        int textY = y + (h - font.lineHeight) / 2;
        int textMaxW = (x + w - 3) - textX;
        if (textMaxW <= 0) return;

        String fullText = "#" + hexDigits;
        String visible = trimToWidth(font, fullText, textMaxW);

        // Selection highlight
        if (focused && hasSelection()) {
            int selStart = Math.min(cursorPos, highlightPos) + 1; // +1 for '#'
            int selEnd = Math.max(cursorPos, highlightPos) + 1;
            selStart = Mth.clamp(selStart, 0, visible.length());
            selEnd = Mth.clamp(selEnd, 0, visible.length());
            if (selEnd > selStart) {
                int sx1 = textX + font.width(visible.substring(0, selStart));
                int sx2 = textX + font.width(visible.substring(0, selEnd));
                context.fill(sx1, textY - 1, sx2, textY + font.lineHeight, 0xFF3B82F6);
            }
        }

        context.drawString(font, visible, textX, textY, 0xFFFFFFFF, false);

        // Cursor
        long now = System.currentTimeMillis();
        if (now - lastBlinkTime > 500) {
            showCursor = !showCursor;
            lastBlinkTime = now;
        }
        if (focused && showCursor && !hasSelection()) {
            int cursorIndex = Mth.clamp(cursorPos + 1, 0, visible.length()); // +1 for '#'
            int cx = textX + font.width(visible.substring(0, cursorIndex));
            if (cx < x + w - 2) {
                context.fill(cx, textY - 1, cx + 1, textY + font.lineHeight, 0xFFFFFFFF);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void setFocused(boolean focused) {
        boolean was = this.isFocused();
        super.setFocused(focused);
        if (focused && !was) {
            lastBlinkTime = System.currentTimeMillis();
            showCursor = true;
        }
        if (!focused && was) {
            // Normalize display to last valid color when leaving
            hexDigits = colorToHexDigits(lastValidColor);
            cursorPos = Mth.clamp(cursorPos, 0, hexDigits.length());
            highlightPos = cursorPos;
            syncMessage();
        }
    }

    public String getText() {
        return "#" + hexDigits;
    }

    public void setText(String text) {
        Integer parsed = parseColor(text);
        if (parsed != null) {
            lastValidColor = parsed;
            hexDigits = colorToHexDigits(parsed);
            cursorPos = hexDigits.length();
            highlightPos = cursorPos;
            syncMessage();
            if (onColorChange != null) onColorChange.accept(parsed);
        }
    }

    public int getColor() {
        return lastValidColor;
    }

    private void insertHex(String hex) {
        if (hex.isEmpty()) return;
        int start = Math.min(cursorPos, highlightPos);
        int end = Math.max(cursorPos, highlightPos);
        int available = MAX_HEX_DIGITS - (hexDigits.length() - (end - start));
        if (available <= 0) return;
        String toInsert = hex.substring(0, Math.min(hex.length(), available));
        hexDigits = hexDigits.substring(0, start) + toInsert + hexDigits.substring(end);
        cursorPos = start + toInsert.length();
        highlightPos = cursorPos;
        tryApplyCurrent();
    }

    private void deleteChars(int dir) {
        if (dir == 0) return;
        int pos = cursorPos + dir;
        pos = Mth.clamp(pos, 0, hexDigits.length());
        int start = Math.min(cursorPos, pos);
        int end = Math.max(cursorPos, pos);
        if (start == end) return;
        hexDigits = hexDigits.substring(0, start) + hexDigits.substring(end);
        cursorPos = start;
        highlightPos = start;
        tryApplyCurrent();
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(cursorPos, highlightPos);
        int end = Math.max(cursorPos, highlightPos);
        hexDigits = hexDigits.substring(0, start) + hexDigits.substring(end);
        cursorPos = start;
        highlightPos = start;
        tryApplyCurrent();
    }

    private void pasteFromClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < clip.length() && filtered.length() < MAX_HEX_DIGITS; i++) {
            char c = clip.charAt(i);
            if (c == '#') continue;
            if (isHex(c)) filtered.append(Character.toUpperCase(c));
        }
        if (!filtered.isEmpty()) {
            insertHex(filtered.toString());
        }
    }

    private void tryApplyCurrent() {
        Integer parsed = parseColor("#" + hexDigits);
        if (parsed != null) {
            lastValidColor = parsed;
            if (onColorChange != null) onColorChange.accept(parsed);
        }
        // Invalid/incomplete: keep lastValidColor, do not mutate text except user edit
        syncMessage();
        resetBlink();
    }

    private void moveCursorBy(int dir, boolean extendSelection) {
        int next = Mth.clamp(cursorPos + dir, 0, hexDigits.length());
        setCursorPosition(next, extendSelection);
    }

    private void setCursorPosition(int pos, boolean extendSelection) {
        cursorPos = Mth.clamp(pos, 0, hexDigits.length());
        if (!extendSelection) {
            highlightPos = cursorPos;
        }
        resetBlink();
    }

    private void selectAll() {
        highlightPos = 0;
        cursorPos = hexDigits.length();
        resetBlink();
    }

    private boolean hasSelection() {
        return cursorPos != highlightPos;
    }

    private String getHighlighted() {
        int start = Math.min(cursorPos, highlightPos);
        int end = Math.max(cursorPos, highlightPos);
        if (start == end) return "";
        return hexDigits.substring(start, end);
    }

    private int selectionReplacedLength() {
        return hexDigits.length() - Math.abs(cursorPos - highlightPos);
    }

    private void resetBlink() {
        showCursor = true;
        lastBlinkTime = System.currentTimeMillis();
    }

    private boolean hasShiftDown() {
        Minecraft mc = Minecraft.getInstance();
        long handle = mc.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private int getTextIndexAt(double mouseX) {
        Font font = getFont();
        int swatchSize = getHeight() - 4;
        int textX = getX() + 2 + swatchSize + 5;
        String fullText = "#" + hexDigits;
        int rel = Mth.floor(mouseX) - textX;
        if (rel <= 0) return 0;
        int idx = fullText.length();
        for (int i = 1; i <= fullText.length(); i++) { // skip '#'
            int w = font.width(fullText.substring(0, i));
            if (w > rel) {
                int prevW = font.width(fullText.substring(0, i - 1));
                idx = (rel - prevW < w - rel) ? i - 1 : i;
                break;
            }
        }
        return Mth.clamp(idx - 1, 0, hexDigits.length()); // map to hexDigits index
    }

    private String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = sb.toString() + text.charAt(i);
            if (font.width(next) > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private void renderSwatch(GuiGraphics context, int x, int y, int size, int argb) {
        // Checkerboard for transparency preview
        int half = Math.max(1, size / 2);
        int light = 0xFFBDBDBD;
        int dark = 0xFF7A7A7A;
        context.fill(x, y, x + size, y + size, light);
        context.fill(x, y, x + half, y + half, dark);
        context.fill(x + half, y + half, x + size, y + size, dark);
        // Color overlay
        context.fill(x, y, x + size, y + size, argb);
        // Border
        int bc = 0xFF555565;
        context.fill(x - 1, y - 1, x + size + 1, y, bc);
        context.fill(x - 1, y + size, x + size + 1, y + size + 1, bc);
        context.fill(x - 1, y, x, y + size, bc);
        context.fill(x + size, y, x + size + 1, y + size, bc);
    }

    private Font getFont() {
        Font f = FontManager.getCurrentFont();
        if (f != null) return f;
        return Minecraft.getInstance().font;
    }

    private void syncMessage() {
        this.setMessage(Component.literal("#" + hexDigits));
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String colorToHexDigits(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0xFF) {
            return String.format("%02X%02X%02X", r, g, b);
        }
        return String.format("%02X%02X%02X%02X", r, g, b, a); // RRGGBBAA
    }

    private static String colorToText(int argb) {
        return "#" + colorToHexDigits(argb);
    }

    /**
     * Parses #RRGGBB or #RRGGBBAA to ARGB int. Returns null if invalid/incomplete.
     * Does not mutate input or auto-pad.
     */
    private static Integer parseColor(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6 && t.length() != 8) return null;
        for (int i = 0; i < t.length(); i++) {
            if (!isHex(t.charAt(i))) return null;
        }
        try {
            if (t.length() == 6) {
                int rgb = (int) Long.parseLong(t, 16);
                return 0xFF000000 | rgb; // ARGB, opaque
            }
            // RRGGBBAA -> AARRGGBB
            int r = Integer.parseInt(t.substring(0, 2), 16);
            int g = Integer.parseInt(t.substring(2, 4), 16);
            int b = Integer.parseInt(t.substring(4, 6), 16);
            int a = Integer.parseInt(t.substring(6, 8), 16);
            return (a << 24) | (r << 16) | (g << 8) | b;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
