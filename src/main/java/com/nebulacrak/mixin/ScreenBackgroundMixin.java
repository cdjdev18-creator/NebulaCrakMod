package com.nebulacrak.mixin;

import com.nebulacrak.gui.NebulaScreenBackgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenBackgroundMixin {
    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$renderMenuBackground(GuiGraphics graphics, int x, int y, int width, int height, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        String id = NebulaScreenBackgrounds.screenIdFor(self);
        if (id == null) return;
        if (NebulaScreenBackgrounds.render(graphics, graphics.guiWidth(), graphics.guiHeight(), id)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$renderPanorama(GuiGraphics graphics, float a, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        String id = NebulaScreenBackgrounds.screenIdFor(self);
        if (id == null) return;
        if (NebulaScreenBackgrounds.render(graphics, graphics.guiWidth(), graphics.guiHeight(), id)) {
            ci.cancel();
        }
    }
}
