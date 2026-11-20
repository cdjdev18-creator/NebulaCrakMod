package com.nebulacrak.mixin;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.client.gui.screen.class_310", remap = false)
public class TitleScreenMixin {

    private static Object customBackgroundTexture;
    private static Object customBackgroundId;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(Object guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (NebulaConfig.mainMenuBackgroundMode == NebulaConfig.BackgroundMode.ORIGINAL) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int width = getScaledWidth(client);
        int height = getScaledHeight(client);

        switch (NebulaConfig.mainMenuBackgroundMode) {
            case COLOR -> {
                invokeFill(guiGraphics, 0, 0, width, height, NebulaConfig.mainMenuBackgroundColor);
                ci.cancel();
            }
            case IMAGE -> {
                if (NebulaConfig.mainMenuImagePath != null && !NebulaConfig.mainMenuImagePath.isEmpty()) {
                    renderCustomImage(guiGraphics, width, height, NebulaConfig.mainMenuImagePath, client);
                    ci.cancel();
                }
            }
        }
    }

    private void renderCustomImage(Object guiGraphics, int width, int height, String imagePath, Minecraft client) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) return;

            if (customBackgroundTexture == null || !getIdentifierPath(customBackgroundId).equals(imagePath)) {
                if (customBackgroundTexture != null) {
                    invokeClose(customBackgroundTexture);
                }
                BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image == null) return;

                Object nativeImage = createNativeImage(image.getWidth(), image.getHeight());
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        setNativeImagePixel(nativeImage, x, y, image.getRGB(x, y));
                    }
                }

                customBackgroundTexture = createDynamicTexture(nativeImage);
                customBackgroundId = createIdentifier("nebulacrakmod", "main_menu_bg_" + imagePath.hashCode());
                registerTexture(client.getTextureManager(), customBackgroundId, customBackgroundTexture);
            }

            blit(guiGraphics, customBackgroundId, 0, 0, 0, 0, width, height, width, height);
        } catch (IOException e) {
            invokeFill(guiGraphics, 0, 0, width, height, NebulaConfig.mainMenuBackgroundColor);
        } catch (Exception e) {
        }
    }

    private static int getScaledWidth(Minecraft client) {
        try {
            return (int) client.getWindow().getClass().getMethod("getScaledWidth").invoke(client.getWindow());
        } catch (Exception e) {
            try {
                return (int) client.getWindow().getClass().getMethod("getWidth").invoke(client.getWindow());
            } catch (Exception ex) {
                return 854;
            }
        }
    }

    private static int getScaledHeight(Minecraft client) {
        try {
            return (int) client.getWindow().getClass().getMethod("getScaledHeight").invoke(client.getWindow());
        } catch (Exception e) {
            try {
                return (int) client.getWindow().getClass().getMethod("getHeight").invoke(client.getWindow());
            } catch (Exception ex) {
                return 480;
            }
        }
    }

    private static Object createNativeImage(int width, int height) throws Exception {
        Class<?> nativeImageClass = Class.forName("com.mojang.blaze3d.platform.class_4708");
        return nativeImageClass.getConstructor(int.class, int.class, boolean.class).newInstance(width, height, true);
    }

    private static void setNativeImagePixel(Object nativeImage, int x, int y, int argb) throws Exception {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = (argb >> 24) & 0xFF;
        Class<?> nativeImageClass = Class.forName("com.mojang.blaze3d.platform.class_4708");
        Method setPixel = nativeImageClass.getMethod("method_22922", int.class, int.class, int.class, int.class, int.class, int.class);
        setPixel.invoke(nativeImage, x, y, r, g, b, a);
    }

    private static Object createDynamicTexture(Object nativeImage) throws Exception {
        Class<?> dynamicTextureClass = Class.forName("com.mojang.blaze3d.textures.class_4707");
        return dynamicTextureClass.getConstructor(nativeImage.getClass()).newInstance(nativeImage);
    }

    private static Object createIdentifier(String namespace, String path) throws Exception {
        Class<?> identifierClass = Class.forName("net.minecraft.class_2960");
        return identifierClass.getMethod("method_13543", String.class, String.class).invoke(null, namespace, path);
    }

    private static String getIdentifierPath(Object identifier) throws Exception {
        Class<?> identifierClass = Class.forName("net.minecraft.class_2960");
        return (String) identifierClass.getMethod("method_13542").invoke(identifier);
    }

    private static void registerTexture(Object textureManager, Object identifier, Object texture) throws Exception {
        Class<?> textureManagerClass = Class.forName("net.minecraft.client.renderer.texture.class_3867");
        Method register = textureManagerClass.getMethod("register", Class.forName("net.minecraft.class_2960"), Class.forName("net.minecraft.client.renderer.texture.class_4706"));
        register.invoke(textureManager, identifier, texture);
    }

    private static void invokeFill(Object guiGraphics, int x1, int y1, int x2, int y2, int color) {
        try {
            Class<?> guiGraphicsClass = Class.forName("net.minecraft.client.gui.class_4709");
            Method fill = guiGraphicsClass.getMethod("method_22724", int.class, int.class, int.class, int.class, int.class);
            fill.invoke(guiGraphics, x1, y1, x2, y2, color);
        } catch (Exception ignored) {}
    }

    private static void blit(Object guiGraphics, Object texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        try {
            Class<?> guiGraphicsClass = Class.forName("net.minecraft.client.gui.class_4709");
            Method blit = guiGraphicsClass.getMethod("method_22723", Class.forName("net.minecraft.class_2960"), int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
            blit.invoke(guiGraphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } catch (Exception ignored) {}
    }

    private static void invokeClose(Object texture) {
        try {
            Class<?> dynamicTextureClass = Class.forName("com.mojang.blaze3d.textures.class_4707");
            Method close = dynamicTextureClass.getMethod("close");
            close.invoke(texture);
        } catch (Exception ignored) {}
    }
}