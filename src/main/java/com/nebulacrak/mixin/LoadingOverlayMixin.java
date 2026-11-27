package com.nebulacrak.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
// este es el encargado del splash del error a largo plazo
//repara antes de mas actializaciones pe no se te olvide
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Unique
    private static final Map<String, DynamicTexture> nebulacrak$textureCache = new HashMap<>();
    @Unique
    private static final Map<String, Identifier> nebulacrak$identifierCache = new HashMap<>();

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;replaceAlpha(II)I"))
    private int nebulacrak$replaceAlpha(int color, int alpha) {
        NebulaConfig.BackgroundMode mode = nebulacrak$getActiveMode();
        if (mode == NebulaConfig.BackgroundMode.ORIGINAL) {
            return color & 16777215 | alpha << 24;
        }
        if (mode == NebulaConfig.BackgroundMode.TRANSPARENT) {
            return 0;
        }
        int base = nebulacrak$getActiveColor(mode);
        return (base & 0x00FFFFFF) | (alpha << 24);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void nebulacrak$renderBackgroundFill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        NebulaConfig.BackgroundMode mode = nebulacrak$getActiveMode();
        if (mode == NebulaConfig.BackgroundMode.IMAGE) {
            String path = nebulacrak$getActiveImagePath();
            if (path != null && !path.isEmpty()
                    && nebulacrak$drawImage(graphics, graphics.guiWidth(), graphics.guiHeight(), path, color)) {
                return;
            }
        }
        if (mode != NebulaConfig.BackgroundMode.TRANSPARENT) {
            graphics.fill(x1, y1, x2, y2, color);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;I)V"))
    private void nebulacrak$clearInitialBackground(CommandEncoder encoder, GpuTexture texture, int color) {
        NebulaConfig.BackgroundMode mode = nebulacrak$getActiveMode();
        if (mode == NebulaConfig.BackgroundMode.COLOR || mode == NebulaConfig.BackgroundMode.IMAGE) {
            encoder.clearColorTexture(texture, nebulacrak$getActiveColor(mode));
            return;
        }
        encoder.clearColorTexture(texture, mode == NebulaConfig.BackgroundMode.TRANSPARENT ? 0 : color);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;I)V", shift = At.Shift.AFTER))
    private void nebulacrak$drawInitialImage(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (nebulacrak$getActiveMode() == NebulaConfig.BackgroundMode.IMAGE) {
            nebulacrak$drawImage(graphics, graphics.guiWidth(), graphics.guiHeight(), nebulacrak$getActiveImagePath(), 0xFFFFFFFF);
        }
    }

    @Redirect(method = "drawProgressBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void nebulacrak$fillProgressBar(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (NebulaConfig.loadingBarMode == NebulaConfig.LoadingBarMode.TRANSPARENT) {
            int barLeft = (Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2) - 120;
            int barRight = barLeft + 240;
            boolean innerFill = x1 > barLeft && x2 < barRight && y1 > 0 && y2 > y1;
            if (innerFill) {
                return;
            }
        }
        graphics.fill(x1, y1, x2, y2, color);
    }

    @Unique
    private NebulaConfig.BackgroundMode nebulacrak$getActiveMode() {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground("mojang");
        NebulaConfig.BackgroundMode mode = bg.mode();
        if (mode == null) mode = NebulaConfig.BackgroundMode.ORIGINAL;
        if (mode == NebulaConfig.BackgroundMode.IMAGE) {
            String path = bg.imagePath();
            if (path == null || path.isEmpty()) return NebulaConfig.BackgroundMode.ORIGINAL;
        }
        return mode;
    }

    @Unique
    private int nebulacrak$getActiveColor(NebulaConfig.BackgroundMode mode) {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground("mojang");
        return bg.color();
    }

    @Unique
    private String nebulacrak$getActiveImagePath() {
        return NebulaConfig.getScreenBackground("mojang").imagePath();
    }

    @Unique
    private int nebulacrak$withAlpha(int baseColor, int originalColor) {
        int alpha = (originalColor >>> 24) & 0xFF;
        return (baseColor & 0x00FFFFFF) | (alpha << 24);
    }

    @Unique
    private boolean nebulacrak$drawImage(GuiGraphics graphics, int width, int height, String imagePath, int originalColor) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) return false;
            String cacheKey = "mojang_" + imagePath.hashCode() + "_" + file.lastModified();

            if (!nebulacrak$textureCache.containsKey(cacheKey)) {
                BufferedImage image = ImageIO.read(file);
                if (image == null) return false;
                NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        nativeImage.setPixel(x, y, image.getRGB(x, y));
                    }
                }
                DynamicTexture texture = new DynamicTexture(() -> "nebulacrakmod:mojang_loading", nativeImage);
                Identifier id = Identifier.fromNamespaceAndPath("nebulacrakmod", cacheKey.toLowerCase().replace('-', '_'));
                Minecraft.getInstance().getTextureManager().register(id, texture);
                nebulacrak$textureCache.put(cacheKey, texture);
                nebulacrak$identifierCache.put(cacheKey, id);
            }

            Identifier id = nebulacrak$identifierCache.get(cacheKey);
            if (id == null) return false;
            int alpha = (originalColor >>> 24) & 0xFF;
            int tint = (alpha << 24) | 0xFFFFFF;
            graphics.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, width, height, width, height, tint);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
