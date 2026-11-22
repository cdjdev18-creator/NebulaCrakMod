package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.font.TextRenderer;

/**
 * Gestor de fuentes simplificado para NebulaCrakMod.
 * Nota: La implementación completa de fuentes TTF personalizadas requeriría
 * registrar un FontProvider en el data pack del mod.
 * Por ahora, usa la fuente predeterminada de Minecraft pero permite
 * rastrear la preferencia del usuario.
 */
public class FontManager {
    private static TextRenderer minecraftFont;
    private static boolean initialized = false;

    /**
     * Inicializa el gestor de fuentes.
     * Debe llamarse después de que Minecraft esté inicializado.
     */
    public static void init() {
        if (initialized) return;
        
        minecraftFont = MinecraftClient.getInstance().textRenderer;
        initialized = true;
    }

    /**
     * Obtiene la fuente actual según la configuración.
     * Por ahora siempre retorna la fuente de Minecraft.
     */
    public static TextRenderer getCurrentFont() {
        if (!initialized) init();
        return minecraftFont;
    }

    /**
     * Obtiene una fuente específica por tipo.
     * Por ahora siempre retorna la fuente de Minecraft.
     */
    public static TextRenderer getFont(NebulaConfig.FontType type) {
        if (!initialized) init();
        return minecraftFont;
    }

    /**
     * Limpia la caché (reinicializa en próximo uso).
     */
    public static void clearCache() {
        initialized = false;
    }
}