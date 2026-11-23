package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * Gestor de fuentes de NebulaCrakMod.
 *
 * Estado actual: la selección de fuente personalizada (Arial/Consolas) se conserva
 * en NebulaConfig para uso futuro, pero no hay implementación real de TTF personalizada
 * en esta versión. El fallback efectivo es la fuente predeterminada de Minecraft.
 */
public final class FontManager {
    private static Font minecraftFont;
    private static boolean initialized = false;

    private FontManager() {
    }

    /**
     * Inicializa el gestor solo cuando Minecraft y su fuente están disponibles.
     * No marca initialized=true si no hay fuente válida.
     */
    public static void init() {
        if (initialized && minecraftFont != null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            clearReferences();
            return;
        }

        Font font = mc.font;
        if (font == null) {
            clearReferences();
            return;
        }

        minecraftFont = font;
        initialized = true;
    }

    /**
     * Devuelve la fuente actual. Fallback seguro a Minecraft.getInstance().font cuando existe.
     */
    public static Font getCurrentFont() {
        ensureInitialized();
        if (minecraftFont != null) return minecraftFont;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.font != null) {
            return mc.font;
        }
        return null;
    }

    /**
     * Compatibilidad con NebulaConfig.FontType.
     * No hay implementación real de fuentes personalizadas; retorna fallback de Minecraft.
     */
    public static Font getFont(NebulaConfig.FontType type) {
        // type reservado para selección futura de fuentes personalizadas.
        return getCurrentFont();
    }

    /**
     * Limpia referencias y estado para permitir reinicialización segura.
     */
    public static void clearCache() {
        clearReferences();
    }

    private static void ensureInitialized() {
        if (!initialized || minecraftFont == null) {
            init();
        }
    }

    private static void clearReferences() {
        minecraftFont = null;
        initialized = false;
    }
}
