package com.nebulacrak.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NebulaTextureManager {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/TextureManager");
    private static final Map<String, DynamicTexture> textureCache = new HashMap<>();
    private static final Map<String, Identifier> identifierCache = new HashMap<>();
    private static final Map<String, String> contentHashCache = new HashMap<>();
    private static final Map<String, CompletableFuture<Identifier>> pending = new HashMap<>();

    public static Identifier loadTextureFromFile(String key, String path) {
        if (key == null || path == null || path.isBlank()) return null;
        try {
            Path filePath = Path.of(path);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) return null;
            byte[] bytes = Files.readAllBytes(filePath);
            String hash = Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "_" + bytes.length;
            if (hash.equals(contentHashCache.get(key)) && identifierCache.containsKey(key)) {
                return identifierCache.get(key);
            }
            return loadTextureFromBytesInternal(key, bytes, hash);
        } catch (Exception e) {
            LOG.error("[NebulaTextureManager] Failed to load texture from file key={} path={}", key, path, e);
            return null;
        }
    }

    public static Identifier loadTextureFromBase64(String key, String base64) {
        if (key == null || base64 == null || base64.isBlank()) return null;
        try {
            String hash = Integer.toHexString(base64.hashCode()) + "_" + base64.length();
            if (hash.equals(contentHashCache.get(key)) && identifierCache.containsKey(key)) {
                return identifierCache.get(key);
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            return loadTextureFromBytesInternal(key, bytes, hash);
        } catch (Exception e) {
            LOG.error("[NebulaTextureManager] Failed to load base64 texture key={}", key, e);
            return null;
        }
    }

    private static synchronized Identifier loadTextureFromBytesInternal(String key, byte[] bytes, String hash) {
        try {
            if (hash.equals(contentHashCache.get(key)) && identifierCache.containsKey(key)) {
                return identifierCache.get(key);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                LOG.warn("[NebulaTextureManager] ImageIO returned null for key={}", key);
                return null;
            }
            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= 0 || h <= 0 || w > 4096 || h > 4096) {
                LOG.warn("[NebulaTextureManager] Invalid image dimensions {}x{} for key={}", w, h, key);
                return null;
            }

            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, w, h, true);
            try {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = image.getRGB(x, y);
                        int a = (argb >>> 24) & 0xFF;
                        int r = (argb >>> 16) & 0xFF;
                        int g = (argb >>> 8) & 0xFF;
                        int b = argb & 0xFF;
                        int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                        nativeImage.setPixelABGR(x, y, abgr);
                    }
                }
            } catch (Throwable t) {
                try { nativeImage.close(); } catch (Throwable ignored) {}
                throw t;
            }

            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                nativeImage.close();
                return null;
            }

            Identifier location = Identifier.fromNamespaceAndPath("nebulacrakmod", sanitizeKey(key));
            Runnable register = () -> {
                try {
                    unloadTextureInternal(key);
                    DynamicTexture texture = new DynamicTexture(() -> "nebulacrakmod:" + key, nativeImage);
                    client.getTextureManager().register(location, texture);
                    textureCache.put(key, texture);
                    identifierCache.put(key, location);
                    contentHashCache.put(key, hash);
                } catch (Throwable t) {
                    LOG.error("[NebulaTextureManager] Failed to register texture key={}", key, t);
                    try { nativeImage.close(); } catch (Throwable ignored) {}
                    identifierCache.remove(key);
                    textureCache.remove(key);
                    contentHashCache.remove(key);
                }
            };

            if (client.isSameThread()) {
                register.run();
            } else {
                client.execute(register);
            }
            return location;
        } catch (Exception e) {
            LOG.error("[NebulaTextureManager] Failed to load texture bytes key={}", key, e);
            return null;
        }
    }

    private static String sanitizeKey(String key) {
        String s = key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        if (s.isBlank()) s = "tex";
        return s;
    }

    public static Identifier getTexture(String key) { return identifierCache.get(key); }
    public static boolean hasTexture(String key) { return identifierCache.containsKey(key); }

    public static synchronized void unloadTexture(String key) {
        unloadTextureInternal(key);
        contentHashCache.remove(key);
    }

    private static void unloadTextureInternal(String key) {
        Identifier location = identifierCache.remove(key);
        DynamicTexture texture = textureCache.remove(key);
        Minecraft client = Minecraft.getInstance();
        if (location != null && client != null) {
            try { client.getTextureManager().release(location); } catch (Throwable ignored) {}
        }
        if (texture != null) {
            try { texture.close(); } catch (Throwable ignored) {}
        }
    }

    public static synchronized void clearAll() {
        for (String key : new java.util.ArrayList<>(textureCache.keySet())) unloadTextureInternal(key);
        textureCache.clear();
        identifierCache.clear();
        contentHashCache.clear();
    }

    public static void blit(net.minecraft.client.gui.GuiGraphics context, Identifier texture, int x, int y, float u, float v,
                             int width, int height, int textureWidth, int textureHeight) {
        if (context == null || texture == null) return;
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, (int) u, (int) v, width, height, textureWidth, textureHeight);
    }
}
