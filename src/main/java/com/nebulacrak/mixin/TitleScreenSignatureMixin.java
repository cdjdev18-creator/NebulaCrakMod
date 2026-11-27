package com.nebulacrak.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para inyectar una firma personalizada en la pantalla de título del juego (TitleScreen).
 */
@Mixin(TitleScreen.class)
public class TitleScreenSignatureMixin {

    // Constantes de texto para la firma de tu mod
    @Unique private static final String NEBULA_NAME = "NebulaCrakMod";
    @Unique private static final String NEBULA_BY = " by ";
    @Unique private static final String NEBULA_DEV = "CDJ dev 18";

    // Códigos de color en formato ARGB (Alfa, Rojo, Verde, Azul)
    @Unique private static final int COLOR_NAME = 0xFFEF4444; // Rojo claro
    @Unique private static final int COLOR_BY = 0xFF9CA3AF;   // Gris claro
    @Unique private static final int COLOR_DEV = 0xFFFFFFFF;  // Blanco

    /**
     * Inyección al final del método 'render' de la pantalla de título.
     * Dibuja los textos de la firma con un fondo parpadeante.
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void nebulacrak$renderSignature(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        
        // Verificación de seguridad para evitar errores si el cliente o las fuentes no están listos
        if (mc == null || mc.font == null || g == null) return;

        var font = mc.font;
        
        //  ancho total que ocupará toda la firma en la pantalla
        int nameW = font.width(NEBULA_NAME);
        int byW = font.width(NEBULA_BY);
        int devW = font.width(NEBULA_DEV);
        int totalW = nameW + byW + devW;

        // Posicionamiento en la esquina superior derecha con un pequeño margen
        int margin = 2;
        int x = Math.max(margin, g.guiWidth() - totalW - margin);
        int y = 3 + font.lineHeight; // Colocado una línea por debajo del borde superior

        // Lógica de parpadeo del fondo: alterna entre verde y negro cada 500L milisegundos
        boolean green = (System.currentTimeMillis() / 500L) % 2L == 0L;
        int bg = green ? 0xFF00AA00 : 0xFF000000;
        
        // Dibujamos el rectángulo de fondo detrás del texto
        g.fill(x - 2, y - 1, x + totalW + 2, y + font.lineHeight + 1, bg);

        // Renderizamos cada sección del texto con su respectivo color de forma consecutiva
        int drawX = x;
        g.drawString(font, NEBULA_NAME, drawX, y, COLOR_NAME, true);
        drawX += nameW;
        
        g.drawString(font, NEBULA_BY, drawX, y, COLOR_BY, true);
        drawX += byW;
        
        g.drawString(font, NEBULA_DEV, drawX, y, COLOR_DEV, true);
    }
}