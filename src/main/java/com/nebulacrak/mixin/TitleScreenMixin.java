package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.NebulaTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    /**
     * Cancela el renderizado del panorama vanilla para evitar que se dibuje
     * el fondo original. Nuestro fondo personalizado se dibuja en renderBackground.
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V"), cancellable = true)
    private void nebulacrak$skipPanorama(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground("mainMenu");
        // Solo salteamos el panorama si hay un fondo personalizado (no ORIGINAL)
        if (bg.mode() != NebulaConfig.BackgroundMode.ORIGINAL) {
            ci.cancel(); // Cancela la llamada al método original renderPanorama
        }
    }

    /**
     * Dibuja el fondo personalizado en renderBackground, que se ejecuta ANTES
     * de los botones/widgets (en Screen.renderWithTooltipAndSubtitles).
     * Esto asegura el orden correcto: Fondo -> Botones -> Logo -> Splash -> Firma.
     */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$renderCustomBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground("mainMenu");
        
        // Si está en modo ORIGINAL, no hacemos nada (deja que el mixin genérico maneje el caso)
        if (bg.mode() == NebulaConfig.BackgroundMode.ORIGINAL) {
            return;
        }

        Minecraft client = Minecraft.getInstance(); 
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        // Dibujamos el fondo personalizado
        switch (bg.mode()) {
            case COLOR -> graphics.fill(0, 0, width, height, bg.color());
            case TRANSPARENT -> {
                // No dibuja nada, fondo transparente - cancelamos para evitar fondo vanilla
                ci.cancel();
            }
            case IMAGE -> {
                String path = bg.imagePath();
                if (path != null && !path.isBlank() && new File(path).exists()) {
                    Identifier id = NebulaTextureManager.loadTextureFromFile("main_menu_bg", path);
                    if (id != null) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, width, height, width, height);
                    } else {
                        graphics.fill(0, 0, width, height, bg.color());
                    }
                } else {
                    graphics.fill(0, 0, width, height, bg.color());
                }
            }
            default -> {}
        }
        // Cancelamos para evitar que se dibuje algo más en esta capa
        ci.cancel();
    }
}