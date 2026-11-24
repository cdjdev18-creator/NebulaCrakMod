package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.NebulaScreenBackgrounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground("mainMenu");
        if (bg.mode() == NebulaConfig.BackgroundMode.ORIGINAL) return;

        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        switch (bg.mode()) {
            case COLOR -> {
                context.fill(0, 0, width, height, bg.color());
                ci.cancel();
            }
            case TRANSPARENT -> ci.cancel();
            case IMAGE -> {
                String path = bg.imagePath();
                if (path != null && !path.isEmpty() && new File(path).exists()) {
                    Identifier id = com.nebulacrak.gui.NebulaTextureManager.loadTextureFromFile("main_menu_bg", path);
                    if (id != null) {
                        context.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, width, height, width, height);
                        ci.cancel();
                        return;
                    }
                }
                context.fill(0, 0, width, height, bg.color());
                ci.cancel();
            }
            default -> {}
        }
    }
}
