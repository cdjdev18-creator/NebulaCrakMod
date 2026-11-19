package com.nebulacrak.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CompactButton extends AbstractWidget {
    public enum Type {
        TAB,
        MASTER_HEADER,
        CATEGORY,
        MODULE,
        LANGUAGE,
        SETTINGS,
        SECTION,
        LABEL,
        SELECTED,
        DISABLED,
        DANGER
    }

    private final Type type;
    private final Button.OnPress onPress;
    private Button.OnPress onRightClick;
    private boolean activated;
    private boolean selected;

    public CompactButton(int x, int y, int width, int height, Component message, Button.OnPress onPress, Type type) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.type = type;
    }

    public CompactButton setActivated(boolean activated) {
        this.activated = activated;
        return this;
    }

    public CompactButton setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public CompactButton setOnRightClick(Button.OnPress onRightClick) {
        this.onRightClick = onRightClick;
        return this;
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isFocused) {
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        if (event.button() == 1 && this.onRightClick != null) {
            this.onRightClick.onPress(null);
        } else if (this.onPress != null) {
            this.onPress.onPress(null);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        boolean hovered = this.isHoveredOrFocused();

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        int bgColor;
        int borderColor;
        int textColor;

        if (!this.active) {
            bgColor = 0xFF141418;
            borderColor = 0xFF22222A;
            textColor = 0xFF555562;
        } else if (type == Type.TAB) {
            if (selected) {
                bgColor = 0xFF2A203E;
                borderColor = 0xFF8B5CF6;
                textColor = 0xFFFFFFFF;
            } else if (hovered) {
                bgColor = 0xFF22222E;
                borderColor = 0xFF4A4A60;
                textColor = 0xFFE2E8F0;
            } else {
                bgColor = 0xFF181822;
                borderColor = 0xFF262634;
                textColor = 0xFF94A3B8;
            }
        } else if (type == Type.MASTER_HEADER) {
            if (hovered) {
                bgColor = 0xFF352055;
                borderColor = 0xFFA78BFA;
                textColor = 0xFFFFFFFF;
            } else {
                bgColor = 0xFF261540;
                borderColor = 0xFF8B5CF6;
                textColor = 0xFFEDE9FE;
            }
        } else if (type == Type.CATEGORY) {
            if (hovered) {
                bgColor = 0xFF262634;
                borderColor = 0xFF4E4E64;
                textColor = 0xFFFFFFFF;
            } else {
                bgColor = 0xFF1C1C24;
                borderColor = 0xFF2C2C3A;
                textColor = 0xFFE2E8F0;
            }
        } else if (type == Type.MODULE) {
            if (activated) {
                if (hovered) {
                    bgColor = 0xFF223B2C;
                    borderColor = 0xFF4ADE80;
                    textColor = 0xFFFFFFFF;
                } else {
                    bgColor = 0xFF1A2E22;
                    borderColor = 0xFF22C55E;
                    textColor = 0xFFF0FDF4;
                }
            } else {
                if (hovered) {
                    bgColor = 0xFF22222C;
                    borderColor = 0xFF474758;
                    textColor = 0xFFE2E8F0;
                } else {
                    bgColor = 0xFF16161D;
                    borderColor = 0xFF262630;
                    textColor = 0xFF94A3B8;
                }
            }
        } else if (type == Type.SETTINGS) {
            if (hovered) {
                bgColor = 0xFF2E2E3E;
                borderColor = 0xFF8B5CF6;
                textColor = 0xFFFFFFFF;
            } else {
                bgColor = 0xFF1A1A22;
                borderColor = 0xFF2E2E3A;
                textColor = 0xFF94A3B8;
            }
        } else { // LANGUAGE or generic
            if (hovered) {
                bgColor = 0xFF2B2B3A;
                borderColor = 0xFF8B5CF6;
                textColor = 0xFFFFFFFF;
            } else {
                bgColor = 0xFF1C1C26;
                borderColor = 0xFF333344;
                textColor = 0xFFCBD5E1;
            }
        }

        // Fondo del botón
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);

        // Bordes (1px)
        guiGraphics.fill(x, y, x + w, y + 1, borderColor);                 // Arriba
        guiGraphics.fill(x, y + h - 1, x + w, y + h, borderColor);         // Abajo
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, borderColor);         // Izquierda
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor); // Derecha

        // Línea de acento inferior en pestañas seleccionadas
        if (type == Type.TAB && selected) {
            guiGraphics.fill(x, y + h - 2, x + w, y + h, 0xFF8B5CF6);
        }

        // Centrado vertical perfecto
        int textY = y + (h - 8) / 2;

        // Renderizado del texto centrado horizontal y verticalmente
        String text = this.getMessage().getString();
        int maxTextWidth = w - 4;
        if (font.width(text) > maxTextWidth) {
            text = font.plainSubstrByWidth(text, maxTextWidth - font.width("...")) + "...";
        }

        guiGraphics.drawCenteredString(font, text, x + w / 2, textY, textColor);
    }
}
