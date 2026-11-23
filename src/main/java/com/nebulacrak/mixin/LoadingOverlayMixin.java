package com.nebulacrak.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow
    @Final
    private boolean fadeIn;

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;replaceAlpha(II)I"
        )
    )
    private int nebulacrak$transparentSplashBackground(int color, int alpha) {
        if (this.fadeIn) {
            return 0x00000000;
        }
        return color & 0x00FFFFFF | alpha << 24;
    }
}