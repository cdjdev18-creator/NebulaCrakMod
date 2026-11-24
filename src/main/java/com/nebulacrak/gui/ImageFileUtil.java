package com.nebulacrak.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.function.Consumer;

public final class ImageFileUtil {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/ImageFileUtil");
    private ImageFileUtil() {}

    public static void openPngPicker(String title, Consumer<String> onSelected, Consumer<String> onError) {
        openImagePicker(title, false, onSelected, onError);
    }

    public static void openImagePicker(String title, boolean allowJpg, Consumer<String> onSelected, Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                String selected = openImageDialog(title, allowJpg);
                if (selected == null || selected.isBlank()) return;
                Path path = Path.of(selected.trim().replace("\"", ""));
                String validationError = validateImage(path, allowJpg);
                if (validationError != null) {
                    LOG.warn("[ImageFileUtil] Invalid image selected {}: {}", path, validationError);
                    mcExec(() -> onError.accept(validationError));
                    return;
                }
                String saved = copyToModImages(path);
                if (saved == null) {
                    mcExec(() -> onError.accept("No se pudo copiar la imagen."));
                    return;
                }
                mcExec(() -> onSelected.accept(saved));
            } catch (Throwable e) {
                LOG.error("[ImageFileUtil] Image picker failed", e);
                mcExec(() -> onError.accept("Error al abrir/leer imagen: " + e.getClass().getSimpleName()));
            }
        }, "Nebula-ImagePicker");
        t.setDaemon(true);
        t.start();
    }

    private static void mcExec(Runnable r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.execute(r);
    }

    private static String openImageDialog(String title, boolean allowJpg) {
        String defaultPath = System.getProperty("user.home") + File.separator + "Pictures" + File.separator;
        String filterDesc = allowJpg ? "Images (*.png;*.jpg;*.jpeg)" : "PNG Images (*.png)";
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(allowJpg ? 3 : 1);
            patterns.put(stack.UTF8("*.png"));
            if (allowJpg) {
                patterns.put(stack.UTF8("*.jpg"));
                patterns.put(stack.UTF8("*.jpeg"));
            }
            patterns.flip();
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    title == null ? filterDesc : title,
                    defaultPath,
                    patterns,
                    filterDesc,
                    false
            );
            if (result != null && !result.isBlank()) return result;
        } catch (Throwable t) {
            LOG.warn("[ImageFileUtil] TinyFileDialogs failed, trying PowerShell fallback", t);
        }

        try {
            String safeTitle = (title == null ? filterDesc : title).replace("'", "''");
            String filter = allowJpg ? "Image Files (*.png;*.jpg;*.jpeg)|*.png;*.jpg;*.jpeg" : "PNG Images (*.png)|*.png";
            String cmd = "powershell.exe -NoProfile -STA -Command \"Add-Type -AssemblyName System.Windows.Forms; $f = New-Object System.Windows.Forms.OpenFileDialog; $f.Filter = '" + filter + "'; $f.InitialDirectory = '" + defaultPath.replace("\\", "\\\\") + "'; $f.Title = '" + safeTitle + "'; if ($f.ShowDialog() -eq 'OK') { Write-Output $f.FileName }\"";
            Process process = Runtime.getRuntime().exec(cmd);
            String out = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            if (!out.isBlank()) return out.lines().findFirst().orElse("").trim();
        } catch (Throwable t) {
            LOG.warn("[ImageFileUtil] PowerShell file dialog failed", t);
        }
        return null;
    }

    public static String validatePng(Path path) { return validateImage(path, false); }

    public static String validateImage(Path path, boolean allowJpg) {
        if (path == null) return "Ruta inválida.";
        if (!Files.exists(path)) return "El archivo no existe.";
        if (!Files.isRegularFile(path)) return "La ruta no es un archivo.";
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean png = name.endsWith(".png");
        boolean jpg = name.endsWith(".jpg") || name.endsWith(".jpeg");
        if (!png && !(allowJpg && jpg)) return allowJpg ? "Solo se permiten imágenes PNG/JPG." : "Solo se permiten imágenes PNG.";
        try {
            if (Files.size(path) > 10L * 1024L * 1024L) return "Imagen demasiado grande (máx 10MB).";
            BufferedImage img = ImageIO.read(path.toFile());
            if (img == null) return "La imagen no es válida o no se pudo leer.";
            if (img.getWidth() <= 0 || img.getHeight() <= 0) return "Dimensiones de imagen inválidas.";
            if (img.getWidth() > 4096 || img.getHeight() > 4096) return "Imagen demasiado grande (máx 4096x4096).";
        } catch (Throwable e) {
            LOG.warn("[ImageFileUtil] validateImage failed for {}", path, e);
            return "No se pudo validar la imagen.";
        }
        return null;
    }

    public static String validateSkinPng(Path path) {
        String base = validatePng(path);
        if (base != null) return base;
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            if (img == null) return "No se pudo leer la skin PNG.";
            int w = img.getWidth();
            int h = img.getHeight();
            boolean valid = (w == 64 && h == 64) || (w == 64 && h == 32) || (w == 128 && h == 128);
            if (!valid) return "Skin inválida. Dimensiones admitidas: 64x64, 64x32, 128x128.";
        } catch (Throwable e) {
            LOG.warn("[ImageFileUtil] validateSkinPng failed for {}", path, e);
            return "No se pudo validar la skin PNG.";
        }
        return null;
    }

    public static String copyToModImages(Path source) {
        try {
            Path modDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                    .resolve("nebulacrakmod").resolve("images");
            Files.createDirectories(modDir);
            String fileName = source.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
            String ext = dot > 0 ? fileName.substring(dot).toLowerCase(Locale.ROOT) : ".png";
            if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg")) ext = ".png";
            baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (baseName.isBlank()) baseName = "image";
            Path dest = modDir.resolve(baseName + "_" + System.currentTimeMillis() + ext);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (Throwable e) {
            LOG.error("[ImageFileUtil] copyToModImages failed", e);
            return null;
        }
    }

    public static void notifyClient(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("[NebulaCrakMod] " + message), false);
            } else {
                LOG.info("[NebulaCrakMod] {}", message);
            }
        });
    }
}
