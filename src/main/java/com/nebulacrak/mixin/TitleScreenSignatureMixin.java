package com.nebulacrak.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenSignatureMixin {
    private static final int COLOR_NAME = 0xFFEF4444;
    private static final int COLOR_BY = 0xFF9CA3AF;
    private static final int COLOR_DEV = 0xFFFFFFFF;

    @Inject(method = "render", at = @At("TAIL"))
    private void nebulacrak$renderSignature(GuiGraphics g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        var font = Minecraft.getInstance().font;
        String name = "NebulaCrakMod";
        String by = " by ";
        String dev = "CDJ dev 18";

        int nameW = font.width(name);
        int byW = font.width(by);
        int devW = font.width(dev);
        int totalW = nameW + byW + devW;

        int margin = 4;
        int x = g.guiWidth() - totalW - margin;
        int y = margin; // top-right, margin keeps clear of edge; avoids fixed low-res coords

        // If another mod draws in the same top-right area, shift down a line to avoid overlap.
        // Keep within screen bounds and do not touch other UI.
        int maxX = g.guiWidth() - margin;
        if (x < margin) x = margin;
        if (y + font.lineHeight > g.guiHeight() - margin) y = Math.max(margin, g.guiHeight() - font.lineHeight - margin);

        g.drawString(font, name, x, y, COLOR_NAME, false);
        x += nameW;
        g.drawString(font, by, x, y, COLOR_BY, false);
        x += byW;
        g.drawString(font, dev, x, y, COLOR_DEV, false);
    }
}
