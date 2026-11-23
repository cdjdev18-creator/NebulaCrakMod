package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    private static final Map<String, DynamicTexture> textureCache = new HashMap<>();
    private static final Map<String, Identifier> identifierCache = new HashMap<>();

    @Shadow
    public abstract boolean fadeIn();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean isInitialSplash = fadeIn();
        NebulaConfig.BackgroundMode mode;
        int color;
        String imagePath;
        String cachePrefix;

        if (isInitialSplash) {
            mode = NebulaConfig.mojangScreenBackgroundMode;
            color = NebulaConfig.mojangScreenBackgroundColor;
            imagePath = NebulaConfig.mojangScreenImagePath;
            cachePrefix = "mojang_bg";
        } else {
            mode = NebulaConfig.loadingScreenBackgroundMode;
            color = NebulaConfig.loadingScreenBackgroundColor;
            imagePath = NebulaConfig.loadingScreenImagePath;
            cachePrefix = "loading_bg";
        }

        if (mode == NebulaConfig.BackgroundMode.ORIGINAL) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        switch (mode) {
            case COLOR -> {
                context.fill(0, 0, width, height, color);
                ci.cancel();
            }
            case TRANSPARENT -> {
                ci.cancel();
            }
            case IMAGE -> {
                if (imagePath != null && !imagePath.isEmpty()) {
                    renderCustomImage(context, width, height, imagePath, client, cachePrefix);
                    ci.cancel();
                }
            }
        }
    }

    private void renderCustomImage(GuiGraphics context, int width, int height, String imagePath, Minecraft client, String prefix) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) return;

            String cacheKey = prefix + "_" + imagePath.hashCode();
            
            if (!textureCache.containsKey(cacheKey)) {
                BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image == null) return;

                NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        nativeImage.setPixel(x, y, image.getRGB(x, y));
                    }
                }

                DynamicTexture texture = new DynamicTexture(() -> "nebulacrakmod:" + cacheKey, nativeImage);
                Identifier id = Identifier.fromNamespaceAndPath("nebulacrakmod", cacheKey);
                
                TextureManager textureManager = client.getTextureManager();
                textureManager.register(id, texture);
                
                textureCache.put(cacheKey, texture);
                identifierCache.put(cacheKey, id);
            }

            Identifier id = identifierCache.get(cacheKey);
            if (id != null) {
                context.blit(id, 0, 0, 0, 0, width, height, width, height);
            }
        } catch (IOException e) {
            context.fill(0, 0, width, height, NebulaConfig.loadingScreenBackgroundColor);
        } catch (Exception e) {
            // Silently fail, fallback to original
        }
    }
}