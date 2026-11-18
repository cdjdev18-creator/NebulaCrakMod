package com.nebulacrak.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void nebulacrak$renderSignature(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int margin = 5;
        int width = context.guiWidth();
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        String left = "by ";
        String right = "CDJ dev 18";
        int leftWidth = font.width(left);
        int totalWidth = leftWidth + font.width(right);
        int x = width - totalWidth - margin;
        int y = margin;
        context.drawString(font, left, x, y, 0xFFFFFF, false);
        context.drawString(font, right, x + leftWidth, y, 0x35D16B, false);
    }
}
