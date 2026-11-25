package com.nebulacrak.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenSignatureMixin {
    @Unique private static final String NEBULA_NAME = "NebulaCrakMod";
    @Unique private static final String NEBULA_BY = " by ";
    @Unique private static final String NEBULA_DEV = "CDJ dev 18";
    @Unique private static final int COLOR_NAME = 0xFFEF4444;
    @Unique private static final int COLOR_BY = 0xFF9CA3AF;
    @Unique private static final int COLOR_DEV = 0xFFFFFFFF;

    @Inject(method = "render", at = @At("TAIL"))
    private void nebulacrak$renderSignature(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || g == null) return;

        var font = mc.font;
        int nameW = font.width(NEBULA_NAME);
        int byW = font.width(NEBULA_BY);
        int devW = font.width(NEBULA_DEV);
        int totalW = nameW + byW + devW;
        int lineH = font.lineHeight;

        int margin = 4;
        int x = g.guiWidth() - totalW - margin;
        int y = margin;

        // Adapt to common top-right overlays (e.g. mod/version labels) by stepping down if occupied area is likely crowded.
        // We keep it non-intrusive: top-right preferred, then shift down in line increments within screen bounds.
        int maxY = g.guiHeight() - lineH - margin;
        if (x < margin) x = margin;
        if (y < margin) y = margin;

        // Heuristic: avoid overlapping Minecraft's top-right debug-like area and keep clear of screen edge.
        // If another element occupies the exact corner, move below it while staying in top-right zone.
        int safetyStep = lineH + 2;
        int attempts = 0;
        while (attempts < 4 && y + lineH > g.guiHeight() - margin) {
            y = Math.max(margin, maxY);
            attempts++;
        }
        // Default to top-right; if UI scale/resolution is tight, nudge down one line to reduce collision chance.
        if (g.guiWidth() < 320 || g.guiHeight() < 240) y = Math.min(maxY, y + safetyStep);

        int drawX = x;
        g.drawString(font, NEBULA_NAME, drawX, y, COLOR_NAME, false);
        drawX += nameW;
        g.drawString(font, NEBULA_BY, drawX, y, COLOR_BY, false);
        drawX += byW;
        g.drawString(font, NEBULA_DEV, drawX, y, COLOR_DEV, false);
    }
}
