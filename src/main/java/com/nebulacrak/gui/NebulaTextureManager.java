package com.nebulacrak.gui;

import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * Gestor de texturas personalizadas para NebulaCrakMod.
 * Usa reflection para compatibilidad con mapeos oficiales de Mojang.
 */
public class NebulaTextureManager {
    private static final Map<String, Object> textureCache = new HashMap<>();
    private static final Map<String, Object> dynamicTextures = new HashMap<>();
    
    // Clases reflejadas (nombres en mapeos oficiales)
    private static Class<?> resourceLocationClass;
    private static Class<?> dynamicTextureClass;
    private static Class<?> nativeImageClass;
    private static Class<?> textureManagerClass;
    private static Class<?> identifierClass;
    private static Method textureManagerRegister;
    private static Method textureManagerRelease;
    private static Method nativeImageSetPixelRGBA;
    private static Method dynamicTextureClose;
    private static Constructor<?> resourceLocationConstructor;
    private static Method resourceLocationFromNamespaceAndPath;
    private static Method guiGraphicsBlit;

    static {
        try {
            // Cargar clases usando nombres oficiales de Mojang
            resourceLocationClass = Class.forName("net.minecraft.class_2960");
            identifierClass = resourceLocationClass; // Identifier = ResourceLocation en mapeos oficiales
            dynamicTextureClass = Class.forName("com.mojang.blaze3d.textures.class_4707"); // DynamicTexture
            nativeImageClass = Class.forName("com.mojang.blaze3d.platform.class_4708"); // NativeImage
            
            // Métodos de TextureManager
            textureManagerClass = Class.forName("net.minecraft.client.renderer.texture.class_3867"); // TextureManager
            textureManagerRegister = textureManagerClass.getMethod("register", resourceLocationClass, Class.forName("net.minecraft.client.renderer.texture.class_4706")); // TextureManager.register(Identifier, Texture)
            textureManagerRelease = textureManagerClass.getMethod("release", resourceLocationClass);
            
            // Métodos de NativeImage
            nativeImageSetPixelRGBA = nativeImageClass.getMethod("method_22922", int.class, int.class, int.class, int.class, int.class, int.class); // setPixelRGBA
            
            // Métodos de DynamicTexture
            dynamicTextureClose = dynamicTextureClass.getMethod("close");
            
            // Constructor de ResourceLocation
            resourceLocationConstructor = resourceLocationClass.getConstructor(String.class, String.class);
            
            // Método estático fromNamespaceAndPath
            resourceLocationFromNamespaceAndPath = resourceLocationClass.getMethod("method_13543", String.class, String.class); // fromNamespaceAndPath
            
            // Método blit de GuiGraphics
            Class<?> guiGraphicsClass = Class.forName("net.minecraft.client.gui.class_4709"); // GuiGraphics
            guiGraphicsBlit = guiGraphicsClass.getMethod("method_22723", identifierClass, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class); // blit(Identifier, x, y, u, v, width, height, textureWidth, textureHeight)
            
        } catch (Exception e) {
            // Fallback silencioso - las texturas no funcionarán pero no crashea
        }
    }

    /**
     * Carga una imagen desde un archivo y la registra como textura.
     * @param key Clave única para la textura
     * @param path Ruta al archivo de imagen
     * @return ResourceLocation (Identifier) de la textura, o null si falla
     */
    public static Object loadTextureFromFile(String key, String path) {
        try {
            Path filePath = Path.of(path);
            if (!Files.exists(filePath)) {
                return null;
            }
            byte[] bytes = Files.readAllBytes(filePath);
            return loadTextureFromBytes(key, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Carga una imagen desde bytes y la registra como textura.
     * @param key Clave única para la textura
     * @param bytes Datos de la imagen
     * @return ResourceLocation (Identifier) de la textura, o null si falla
     */
    public static Object loadTextureFromBytes(String key, byte[] bytes) {
        try {
            // Validar que es una imagen válida
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return null;
            }

            // Convertir a formato RGBA
            BufferedImage rgbaImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            rgbaImage.getGraphics().drawImage(image, 0, 0, null);

            // Crear NativeImage usando reflection
            Object nativeImage = nativeImageClass.getConstructor(int.class, int.class, boolean.class)
                .newInstance(rgbaImage.getWidth(), rgbaImage.getHeight(), false);
            
            int[] pixels = new int[rgbaImage.getWidth() * rgbaImage.getHeight()];
            rgbaImage.getRGB(0, 0, rgbaImage.getWidth(), rgbaImage.getHeight(), pixels, 0, rgbaImage.getWidth());
            
            for (int y = 0; y < rgbaImage.getHeight(); y++) {
                for (int x = 0; x < rgbaImage.getWidth(); x++) {
                    int argb = pixels[y * rgbaImage.getWidth() + x];
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int a = (argb >> 24) & 0xFF;
                    nativeImageSetPixelRGBA.invoke(nativeImage, x, y, r, g, b, a);
                }
            }

            // Crear DynamicTexture
            Object texture = dynamicTextureClass.getConstructor(nativeImageClass)
                .newInstance(nativeImage);
            
            // Registrar en el TextureManager de Minecraft
            Object textureManager = Minecraft.getInstance().getClass().getMethod("getTextureManager").invoke(Minecraft.getInstance());
            Object location = resourceLocationFromNamespaceAndPath.invoke(null, "nebulacrakmod", key + "_" + UUID.randomUUID().toString().substring(0, 8));
            textureManagerRegister.invoke(textureManager, location, texture);
            
            // Guardar en caché
            textureCache.put(key, location);
            dynamicTextures.put(key, texture);
            
            return location;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Carga una imagen desde base64 y la registra como textura.
     * @param key Clave única para la textura
     * @param base64 String base64 de la imagen
     * @return ResourceLocation (Identifier) de la textura, o null si falla
     */
    public static Object loadTextureFromBase64(String key, String base64) {
        try {
            if (base64 == null || base64.isEmpty()) {
                return null;
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            return loadTextureFromBytes(key, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene la ResourceLocation (Identifier) de una textura cargada.
     * @param key Clave de la textura
     * @return Identifier o null si no existe
     */
    public static Object getTexture(String key) {
        return textureCache.get(key);
    }

    /**
     * Verifica si una textura está cargada.
     * @param key Clave de la textura
     * @return true si la textura existe
     */
    public static boolean hasTexture(String key) {
        return textureCache.containsKey(key);
    }

    /**
     * Elimina una textura de la caché y del TextureManager.
     * @param key Clave de la textura
     */
    public static void unloadTexture(String key) {
        Object location = textureCache.remove(key);
        Object texture = dynamicTextures.remove(key);
        if (location != null && texture != null) {
            try {
                Object textureManager = Minecraft.getInstance().getClass().getMethod("getTextureManager").invoke(Minecraft.getInstance());
                textureManagerRelease.invoke(textureManager, location);
                dynamicTextureClose.invoke(texture);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Limpia todas las texturas personalizadas.
     */
    public static void clearAll() {
        Object textureManager = null;
        try {
            textureManager = Minecraft.getInstance().getClass().getMethod("getTextureManager").invoke(Minecraft.getInstance());
        } catch (Exception ignored) {}
        for (Object texture : dynamicTextures.values()) {
            try {
                dynamicTextureClose.invoke(texture);
            } catch (Exception ignored) {
            }
        }
        for (Object location : textureCache.values()) {
            try {
                if (textureManager != null) {
                    textureManagerRelease.invoke(textureManager, location);
                }
            } catch (Exception ignored) {
            }
        }
        textureCache.clear();
        dynamicTextures.clear();
    }

    /**
     * Renderiza una textura usando reflection para llamar a blit.
     * @param guiGraphics Instancia de GuiGraphics
     * @param texture Identifier de la textura
     * @param x Posición X
     * @param y Posición Y
     * @param u Coordenada U en la textura
     * @param v Coordenada V en la textura
     * @param width Ancho
     * @param height Alto
     * @param textureWidth Ancho de la textura
     * @param textureHeight Alto de la textura
     */
    public static void blit(Object guiGraphics, Object texture, int x, int y, float u, float v, 
                             int width, int height, int textureWidth, int textureHeight) {
        try {
            guiGraphicsBlit.invoke(guiGraphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } catch (Exception ignored) {
        }
    }
}