package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.splash.SplashOverlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    private static final Map<String, DynamicTexture> textureCache = new HashMap<>();
    private static final Map<String, Identifier> identifierCache = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (NebulaConfig.mojangScreenBackgroundMode == NebulaConfig.BackgroundMode.ORIGINAL) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        switch (NebulaConfig.mojangScreenBackgroundMode) {
            case COLOR -> {
                context.fill(0, 0, width, height, NebulaConfig.mojangScreenBackgroundColor);
            }
            case IMAGE -> {
                if (NebulaConfig.mojangScreenImagePath != null && !NebulaConfig.mojangScreenImagePath.isEmpty()) {
                    renderCustomImage(context, width, height, NebulaConfig.mojangScreenImagePath, client, "mojang_bg");
                }
            }
        }
    }

    private void renderCustomImage(DrawContext context, int width, int height, String imagePath, MinecraftClient client, String prefix) {
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
                        nativeImage.setColorArgb(x, y, image.getRGB(x, y));
                    }
                }

                DynamicTexture texture = new DynamicTexture(nativeImage);
                Identifier id = Identifier.of("nebulacrakmod", cacheKey);
                
                TextureManager textureManager = client.getTextureManager();
                textureManager.register(id, texture);
                
                textureCache.put(cacheKey, texture);
                identifierCache.put(cacheKey, id);
            }

            Identifier id = identifierCache.get(cacheKey);
            if (id != null) {
                context.drawTexture(id, 0, 0, 0, 0, width, height, width, height);
            }
        } catch (IOException e) {
            context.fill(0, 0, width, height, NebulaConfig.mojangScreenBackgroundColor);
        } catch (Exception e) {
            // Silently fail, fallback to original
        }
    }
}