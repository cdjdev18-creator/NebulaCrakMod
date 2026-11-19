package com.nebulacrak.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Configuración general de NebulaCrakMod (sección CONFI).
 * Se guarda en .minecraft/config/nebulacrakmod/nebula.json
 */
public class NebulaConfig {

    // ── Tecla de apertura ──────────────────────────────────────────────────────
    public static int openKeyCode = 192; // GLFW_KEY_GRAVE_ACCENT (tecla ` / º por defecto)

    // ── Cerrar con tecla de apertura ──────────────────────────────────────────
    public static boolean closeWithOpenKey = true;

    // ── Tipo de letra ─────────────────────────────────────────────────────────
    public enum FontType { ARIAL, CONSOLAS }
    public static FontType fontType = FontType.ARIAL;

    // ── Estilo de botones ─────────────────────────────────────────────────────
    public enum ButtonStyle { DEFAULT, TRANSPARENT, METEOR }
    public static ButtonStyle buttonStyle = ButtonStyle.DEFAULT;

    // ── Rutas de imágenes personalizadas (para EDIT,MC) ───────────────────────
    public static String mainMenuImagePath = "";
    public static String mojangScreenImagePath = "";
    public static String loadingScreenImagePath = "";

    // ── Modos de fondo para EDIT,MC ───────────────────────────────────────────
    public enum BackgroundMode { ORIGINAL, COLOR, IMAGE, TRANSPARENT }
    public static BackgroundMode mainMenuBackgroundMode = BackgroundMode.ORIGINAL;
    public static int mainMenuBackgroundColor = 0xFF121218;
    public static BackgroundMode mojangScreenBackgroundMode = BackgroundMode.ORIGINAL;
    public static int mojangScreenBackgroundColor = 0xFF121218;
    public static BackgroundMode loadingScreenBackgroundMode = BackgroundMode.ORIGINAL;
    public static int loadingScreenBackgroundColor = 0xFF121218;

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("nebulacrakmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("nebula.json");

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) return;

            JsonObject root = JsonParser.parseString(Files.readString(CONFIG_FILE, StandardCharsets.UTF_8)).getAsJsonObject();

            // Tecla de apertura
            openKeyCode = getInt(root, "openKeyCode", openKeyCode);

            // Cerrar con tecla de apertura
            closeWithOpenKey = getBool(root, "closeWithOpenKey", closeWithOpenKey);

            // Fuente
            fontType = parseEnum(FontType.class, root, "fontType", FontType.ARIAL);

            // Estilo de botones
            buttonStyle = parseEnum(ButtonStyle.class, root, "buttonStyle", ButtonStyle.DEFAULT);

            // Rutas de imágenes
            mainMenuImagePath = getString(root, "mainMenuImagePath", "");
            mojangScreenImagePath = getString(root, "mojangScreenImagePath", "");
            loadingScreenImagePath = getString(root, "loadingScreenImagePath", "");

            // Modos de fondo
            mainMenuBackgroundMode = parseEnum(BackgroundMode.class, root, "mainMenuBackgroundMode", BackgroundMode.ORIGINAL);
            mainMenuBackgroundColor = getInt(root, "mainMenuBackgroundColor", mainMenuBackgroundColor);
            mojangScreenBackgroundMode = parseEnum(BackgroundMode.class, root, "mojangScreenBackgroundMode", BackgroundMode.ORIGINAL);
            mojangScreenBackgroundColor = getInt(root, "mojangScreenBackgroundColor", mojangScreenBackgroundColor);
            loadingScreenBackgroundMode = parseEnum(BackgroundMode.class, root, "loadingScreenBackgroundMode", BackgroundMode.ORIGINAL);
            loadingScreenBackgroundColor = getInt(root, "loadingScreenBackgroundColor", loadingScreenBackgroundColor);

            // Validar imágenes
            validateImages();
        } catch (Exception e) {
            // Archivo corrupto o no existe → usar valores por defecto
        }
    }

    private static void validateImages() {
        if (mainMenuBackgroundMode == BackgroundMode.IMAGE && !mainMenuImagePath.isEmpty()
                && !Files.exists(Path.of(mainMenuImagePath))) {
            mainMenuBackgroundMode = BackgroundMode.ORIGINAL;
            mainMenuImagePath = "";
        }
        if (mojangScreenBackgroundMode == BackgroundMode.IMAGE && !mojangScreenImagePath.isEmpty()
                && !Files.exists(Path.of(mojangScreenImagePath))) {
            mojangScreenBackgroundMode = BackgroundMode.ORIGINAL;
            mojangScreenImagePath = "";
        }
        if (loadingScreenBackgroundMode == BackgroundMode.IMAGE && !loadingScreenImagePath.isEmpty()
                && !Files.exists(Path.of(loadingScreenImagePath))) {
            loadingScreenBackgroundMode = BackgroundMode.ORIGINAL;
            loadingScreenImagePath = "";
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            JsonObject root = new JsonObject();

            root.addProperty("openKeyCode", openKeyCode);
            root.addProperty("closeWithOpenKey", closeWithOpenKey);
            root.addProperty("fontType", fontType.name());
            root.addProperty("buttonStyle", buttonStyle.name());
            root.addProperty("mainMenuImagePath", mainMenuImagePath);
            root.addProperty("mojangScreenImagePath", mojangScreenImagePath);
            root.addProperty("loadingScreenImagePath", loadingScreenImagePath);
            root.addProperty("mainMenuBackgroundMode", mainMenuBackgroundMode.name());
            root.addProperty("mainMenuBackgroundColor", mainMenuBackgroundColor);
            root.addProperty("mojangScreenBackgroundMode", mojangScreenBackgroundMode.name());
            root.addProperty("mojangScreenBackgroundColor", mojangScreenBackgroundColor);
            root.addProperty("loadingScreenBackgroundMode", loadingScreenBackgroundMode.name());
            root.addProperty("loadingScreenBackgroundColor", loadingScreenBackgroundColor);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(CONFIG_FILE, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, JsonObject obj, String key, T fallback) {
        try {
            if (!obj.has(key)) return fallback;
            return Enum.valueOf(clazz, obj.get(key).getAsString());
        } catch (Exception e) {
            return fallback;
        }
    }
    private static int getInt(JsonObject o, String k, int d) {
        try { return o.get(k).getAsInt(); } catch (Exception e) { return d; }
    }
    private static boolean getBool(JsonObject o, String k, boolean d) {
        try { return o.get(k).getAsBoolean(); } catch (Exception e) { return d; }
    }
    private static String getString(JsonObject o, String k, String d) {
        try { return o.get(k).getAsString(); } catch (Exception e) { return d; }
    }
}