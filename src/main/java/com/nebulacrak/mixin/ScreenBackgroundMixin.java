package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.NebulaScreenBackgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenBackgroundMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        String id = NebulaScreenBackgrounds.screenIdFor(self);
        if (id == null) return;
        if (NebulaScreenBackgrounds.render(graphics, graphics.guiWidth(), graphics.guiHeight(), id)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$skipCustomTitlePanorama(GuiGraphics graphics, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        NebulaConfig.BackgroundMode mode = NebulaConfig.getScreenBackground("mainMenu").mode();
        if (self instanceof TitleScreen && mode != null && mode != NebulaConfig.BackgroundMode.ORIGINAL) {
            ci.cancel();
        }
    }
}
