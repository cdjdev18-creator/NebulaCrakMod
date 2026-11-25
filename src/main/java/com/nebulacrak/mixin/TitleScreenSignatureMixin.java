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

    @Inject(method = "render", at = @At("RETURN"))
    private void nebulacrak$renderSignature(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || g == null) return;

        var font = mc.font;
        int nameW = font.width(NEBULA_NAME);
        int byW = font.width(NEBULA_BY);
        int devW = font.width(NEBULA_DEV);
        int totalW = nameW + byW + devW;

        int margin = 2;
        int x = Math.max(margin, g.guiWidth() - totalW - margin);
        int y = 3 + font.lineHeight; // one text line below top area

        boolean green = (System.currentTimeMillis() / 700L) % 2L == 0L;
        int bg = green ? 0xFF00AA00 : 0xFF000000;
        g.fill(x - 2, y - 1, x + totalW + 2, y + font.lineHeight + 1, bg);

        int drawX = x;
        g.drawString(font, NEBULA_NAME, drawX, y, COLOR_NAME, true);
        drawX += nameW;
        g.drawString(font, NEBULA_BY, drawX, y, COLOR_BY, true);
        drawX += byW;
        g.drawString(font, NEBULA_DEV, drawX, y, COLOR_DEV, true);
    }
}
