package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.font.TextRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.widget.AbstractWidget;
import net.minecraft.client.gui.widget.Button;
import net.minecraft.client.input.mouse.MouseButtonEvent;
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
    public void onClick(MouseButtonEvent event) {
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        if (event.button() == 1 && this.onRightClick != null) {
            this.onRightClick.onPress(this);
        } else if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float partialTick) {
        TextRenderer font = FontManager.getCurrentFont();
        boolean hovered = this.isHoveredOrFocused();

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        int bgColor;
        int borderColor;
        int textColor;

        NebulaConfig.ButtonStyle style = NebulaConfig.buttonStyle;

        if (!this.active) {
            bgColor = getDisabledBg(style);
            borderColor = getDisabledBorder(style);
            textColor = getDisabledText(style);
        } else if (type == Type.TAB) {
            if (selected) {
                bgColor = getTabSelectedBg(style);
                borderColor = getTabSelectedBorder(style);
                textColor = getTabSelectedText(style);
            } else if (hovered) {
                bgColor = getTabHoveredBg(style);
                borderColor = getTabHoveredBorder(style);
                textColor = getTabHoveredText(style);
            } else {
                bgColor = getTabBg(style);
                borderColor = getTabBorder(style);
                textColor = getTabText(style);
            }
        } else if (type == Type.MASTER_HEADER) {
            if (hovered) {
                bgColor = getHeaderHoveredBg(style);
                borderColor = getHeaderHoveredBorder(style);
                textColor = getHeaderHoveredText(style);
            } else {
                bgColor = getHeaderBg(style);
                borderColor = getHeaderBorder(style);
                textColor = getHeaderText(style);
            }
        } else if (type == Type.CATEGORY) {
            if (hovered) {
                bgColor = getCategoryHoveredBg(style);
                borderColor = getCategoryHoveredBorder(style);
                textColor = getCategoryHoveredText(style);
            } else {
                bgColor = getCategoryBg(style);
                borderColor = getCategoryBorder(style);
                textColor = getCategoryText(style);
            }
        } else if (type == Type.MODULE) {
            if (activated) {
                if (hovered) {
                    bgColor = getModuleActiveHoveredBg(style);
                    borderColor = getModuleActiveHoveredBorder(style);
                    textColor = getModuleActiveHoveredText(style);
                } else {
                    bgColor = getModuleActiveBg(style);
                    borderColor = getModuleActiveBorder(style);
                    textColor = getModuleActiveText(style);
                }
            } else {
                if (hovered) {
                    bgColor = getModuleInactiveHoveredBg(style);
                    borderColor = getModuleInactiveHoveredBorder(style);
                    textColor = getModuleInactiveHoveredText(style);
                } else {
                    bgColor = getModuleInactiveBg(style);
                    borderColor = getModuleInactiveBorder(style);
                    textColor = getModuleInactiveText(style);
                }
            }
        } else if (type == Type.SETTINGS || type == Type.SELECTED) {
            if (hovered) {
                bgColor = getSettingsHoveredBg(style);
                borderColor = getSettingsHoveredBorder(style);
                textColor = getSettingsHoveredText(style);
            } else {
                bgColor = getSettingsBg(style);
                borderColor = getSettingsBorder(style);
                textColor = getSettingsText(style);
            }
        } else if (type == Type.DANGER) {
            if (hovered) {
                bgColor = getDangerHoveredBg(style);
                borderColor = getDangerHoveredBorder(style);
                textColor = getDangerHoveredText(style);
            } else {
                bgColor = getDangerBg(style);
                borderColor = getDangerBorder(style);
                textColor = getDangerText(style);
            }
        } else { // LANGUAGE, LABEL, SECTION, or generic
            if (hovered) {
                bgColor = getGenericHoveredBg(style);
                borderColor = getGenericHoveredBorder(style);
                textColor = getGenericHoveredText(style);
            } else {
                bgColor = getGenericBg(style);
                borderColor = getGenericBorder(style);
                textColor = getGenericText(style);
            }
        }

        // Fondo del botón
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgColor);

        // Bordes (1px)
        context.fill(x, y, x + w, y + 1, borderColor);                 // Arriba
        context.fill(x, y + h - 1, x + w, y + h, borderColor);         // Abajo
        context.fill(x, y + 1, x + 1, y + h - 1, borderColor);         // Izquierda
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor); // Derecha

        // Línea de acento inferior en pestañas seleccionadas
        if (type == Type.TAB && selected) {
            context.fill(x, y + h - 2, x + w, y + h, getTabAccentColor(style));
        }

        // Centrado vertical perfecto
        int textY = y + (h - 8) / 2;

        // Renderizado del texto centrado horizontal y verticalmente
        String text = this.getMessage().getString();
        int maxTextWidth = w - 4;
        if (font.getWidth(text) > maxTextWidth) {
            text = font.trimToWidth(text, maxTextWidth - font.getWidth("...")) + "...";
        }

        context.drawCenteredText(font, text, x + w / 2, textY, textColor);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  COLOR SCHEMES PER BUTTON STYLE
    // ═══════════════════════════════════════════════════════════════════════════

    // Disabled
    private int getDisabledBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80141418;
            case METEOR -> 0xFF1A1A2E;
            default -> 0xFF141418;
        };
    }
    private int getDisabledBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x8022222A;
            case METEOR -> 0xFF2A2A3E;
            default -> 0xFF22222A;
        };
    }
    private int getDisabledText(NebulaConfig.ButtonStyle style) {
        return 0xFF555562;
    }

    // TAB
    private int getTabBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80181822;
            case METEOR -> 0xFF1E1E2E;
            default -> 0xFF181822;
        };
    }
    private int getTabBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80262634;
            case METEOR -> 0xFF2A2A3E;
            default -> 0xFF262634;
        };
    }
    private int getTabText(NebulaConfig.ButtonStyle style) {
        return 0xFF94A3B8;
    }
    private int getTabHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA022222E;
            case METEOR -> 0xFF2A2A3E;
            default -> 0xFF22222E;
        };
    }
    private int getTabHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA04A4A60;
            case METEOR -> 0xFF4A4A5E;
            default -> 0xFF4A4A60;
        };
    }
    private int getTabHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFE2E8F0;
    }
    private int getTabSelectedBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA02A203E;
            case METEOR -> 0xFF2A1A3E;
            default -> 0xFF2A203E;
        };
    }
    private int getTabSelectedBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA08B5CF6;
            case METEOR -> 0xFF8B5CF6;
            default -> 0xFF8B5CF6;
        };
    }
    private int getTabSelectedText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }
    private int getTabAccentColor(NebulaConfig.ButtonStyle style) {
        return 0xFF8B5CF6;
    }

    // MASTER_HEADER
    private int getHeaderBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA0261540;
            case METEOR -> 0xFF1A102E;
            default -> 0xFF261540;
        };
    }
    private int getHeaderBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA08B5CF6;
            case METEOR -> 0xFF8B5CF6;
            default -> 0xFF8B5CF6;
        };
    }
    private int getHeaderText(NebulaConfig.ButtonStyle style) {
        return 0xFFEDE9FE;
    }
    private int getHeaderHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xB0352055;
            case METEOR -> 0xFF2A1A4E;
            default -> 0xFF352055;
        };
    }
    private int getHeaderHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xB0A78BFA;
            case METEOR -> 0xFFA78BFA;
            default -> 0xFFA78BFA;
        };
    }
    private int getHeaderHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }

    // CATEGORY
    private int getCategoryBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x801C1C24;
            case METEOR -> 0xFF1A1A2A;
            default -> 0xFF1C1C24;
        };
    }
    private int getCategoryBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x802C2C3A;
            case METEOR -> 0xFF2A2A3A;
            default -> 0xFF2C2C3A;
        };
    }
    private int getCategoryText(NebulaConfig.ButtonStyle style) {
        return 0xFFE2E8F0;
    }
    private int getCategoryHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA0262634;
            case METEOR -> 0xFF2A2A3A;
            default -> 0xFF262634;
        };
    }
    private int getCategoryHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA04E4E64;
            case METEOR -> 0xFF4E4E5E;
            default -> 0xFF4E4E64;
        };
    }
    private int getCategoryHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }

    // MODULE (activated)
    private int getModuleActiveBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x801A2E22;
            case METEOR -> 0xFF1A2E2A;
            default -> 0xFF1A2E22;
        };
    }
    private int getModuleActiveBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x8022C55E;
            case METEOR -> 0xFF22C55E;
            default -> 0xFF22C55E;
        };
    }
    private int getModuleActiveText(NebulaConfig.ButtonStyle style) {
        return 0xFFF0FDF4;
    }
    private int getModuleActiveHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA0223B2C;
            case METEOR -> 0xFF223B2E;
            default -> 0xFF223B2C;
        };
    }
    private int getModuleActiveHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA04ADE80;
            case METEOR -> 0xFF4ADE80;
            default -> 0xFF4ADE80;
        };
    }
    private int getModuleActiveHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }

    // MODULE (inactive)
    private int getModuleInactiveBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x8016161D;
            case METEOR -> 0xFF16161E;
            default -> 0xFF16161D;
        };
    }
    private int getModuleInactiveBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80262630;
            case METEOR -> 0xFF262630;
            default -> 0xFF262630;
        };
    }
    private int getModuleInactiveText(NebulaConfig.ButtonStyle style) {
        return 0xFF94A3B8;
    }
    private int getModuleInactiveHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA022222C;
            case METEOR -> 0xFF22222E;
            default -> 0xFF22222C;
        };
    }
    private int getModuleInactiveHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA0474758;
            case METEOR -> 0xFF474758;
            default -> 0xFF474758;
        };
    }
    private int getModuleInactiveHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFE2E8F0;
    }

    // SETTINGS / SELECTED
    private int getSettingsBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x801A1A22;
            case METEOR -> 0xFF1A1A2A;
            default -> 0xFF1A1A22;
        };
    }
    private int getSettingsBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x802E2E3A;
            case METEOR -> 0xFF2E2E3A;
            default -> 0xFF2E2E3A;
        };
    }
    private int getSettingsText(NebulaConfig.ButtonStyle style) {
        return 0xFF94A3B8;
    }
    private int getSettingsHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA02E2E3E;
            case METEOR -> 0xFF2E2E3E;
            default -> 0xFF2E2E3E;
        };
    }
    private int getSettingsHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA08B5CF6;
            case METEOR -> 0xFF8B5CF6;
            default -> 0xFF8B5CF6;
        };
    }
    private int getSettingsHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }

    // DANGER
    private int getDangerBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x803A1A1A;
            case METEOR -> 0xFF3A1A1A;
            default -> 0xFF3A1A1A;
        };
    }
    private int getDangerBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80EF4444;
            case METEOR -> 0xFFEF4444;
            default -> 0xFFEF4444;
        };
    }
    private int getDangerText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }
    private int getDangerHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA04A1A1A;
            case METEOR -> 0xFF4A1A1A;
            default -> 0xFF4A1A1A;
        };
    }
    private int getDangerHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA0F87171;
            case METEOR -> 0xFFF87171;
            default -> 0xFFF87171;
        };
    }
    private int getDangerHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }

    // GENERIC (LANGUAGE, LABEL, SECTION)
    private int getGenericBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x801C1C26;
            case METEOR -> 0xFF1C1C2A;
            default -> 0xFF1C1C26;
        };
    }
    private int getGenericBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0x80333344;
            case METEOR -> 0xFF333344;
            default -> 0xFF333344;
        };
    }
    private int getGenericText(NebulaConfig.ButtonStyle style) {
        return 0xFFCBD5E1;
    }
    private int getGenericHoveredBg(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA02B2B3A;
            case METEOR -> 0xFF2B2B3A;
            default -> 0xFF2B2B3A;
        };
    }
    private int getGenericHoveredBorder(NebulaConfig.ButtonStyle style) {
        return switch (style) {
            case TRANSPARENT -> 0xA08B5CF6;
            case METEOR -> 0xFF8B5CF6;
            default -> 0xFF8B5CF6;
        };
    }
    private int getGenericHoveredText(NebulaConfig.ButtonStyle style) {
        return 0xFFFFFFFF;
    }
}