package com.nebulacrak.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * Gestor de texturas personalizadas para NebulaCrakMod.
 * Usa mapeos oficiales de Mojang.
 */
public class NebulaTextureManager {
    private static final Map<String, DynamicTexture> textureCache = new HashMap<>();
    private static final Map<String, Identifier> identifierCache = new HashMap<>();

    /**
     * Carga una imagen desde un archivo y la registra como textura.
     * @param key Clave única para la textura
     * @param path Ruta al archivo de imagen
     * @return Identifier de la textura, o null si falla
     */
    public static Identifier loadTextureFromFile(String key, String path) {
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
     * @return Identifier de la textura, o null si falla
     */
    public static Identifier loadTextureFromBytes(String key, byte[] bytes) {
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

            // Crear NativeImage
            NativeImage nativeImage = new NativeImage(rgbaImage.getWidth(), rgbaImage.getHeight(), false);
            
            int[] pixels = new int[rgbaImage.getWidth() * rgbaImage.getHeight()];
            rgbaImage.getRGB(0, 0, rgbaImage.getWidth(), rgbaImage.getHeight(), pixels, 0, rgbaImage.getWidth());
            
            for (int y = 0; y < rgbaImage.getHeight(); y++) {
                for (int x = 0; x < rgbaImage.getWidth(); x++) {
                    int argb = pixels[y * rgbaImage.getWidth() + x];
                    nativeImage.setPixel(x, y, argb);
                }
            }

            // Crear DynamicTexture
            DynamicTexture texture = new DynamicTexture(() -> "nebulacrakmod:" + key, nativeImage);
            
            // Registrar en el TextureManager de Minecraft
            Minecraft client = Minecraft.getInstance();
            TextureManager textureManager = client.getTextureManager();
            Identifier location = Identifier.fromNamespaceAndPath("nebulacrakmod", key + "_" + UUID.randomUUID().toString().substring(0, 8));
            textureManager.register(location, texture);
            
            // Guardar en caché
            textureCache.put(key, texture);
            identifierCache.put(key, location);
            
            return location;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Carga una imagen desde base64 y la registra como textura.
     * @param key Clave única para la textura
     * @param base64 String base64 de la imagen
     * @return Identifier de la textura, o null si falla
     */
    public static Identifier loadTextureFromBase64(String key, String base64) {
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
     * Obtiene el Identifier de una textura cargada.
     * @param key Clave de la textura
     * @return Identifier o null si no existe
     */
    public static Identifier getTexture(String key) {
        return identifierCache.get(key);
    }

    /**
     * Verifica si una textura está cargada.
     * @param key Clave de la textura
     * @return true si la textura existe
     */
    public static boolean hasTexture(String key) {
        return identifierCache.containsKey(key);
    }

    /**
     * Elimina una textura de la caché y del TextureManager.
     * @param key Clave de la textura
     */
    public static void unloadTexture(String key) {
        Identifier location = identifierCache.remove(key);
        DynamicTexture texture = textureCache.remove(key);
        if (location != null && texture != null) {
            try {
                Minecraft client = Minecraft.getInstance();
                TextureManager textureManager = client.getTextureManager();
                textureManager.release(location);
                texture.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Limpia todas las texturas personalizadas.
     */
    public static void clearAll() {
        Minecraft client = Minecraft.getInstance();
        TextureManager textureManager = client.getTextureManager();
        for (DynamicTexture texture : textureCache.values()) {
            try {
                texture.close();
            } catch (Exception ignored) {
            }
        }
        for (Identifier location : identifierCache.values()) {
            try {
                textureManager.release(location);
            } catch (Exception ignored) {
            }
        }
        textureCache.clear();
        identifierCache.clear();
    }

    /**
     * Renderiza una textura usando GuiGraphics.blit.
     * @param context Instancia de GuiGraphics
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
    public static void blit(net.minecraft.client.gui.GuiGraphics context, Identifier texture, int x, int y, int u, int v, 
                             int width, int height, int textureWidth, int textureHeight) {
        context.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}