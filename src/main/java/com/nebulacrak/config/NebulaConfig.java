package com.nebulacrak.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración general de NebulaCrakMod.
 * Se guarda en .minecraft/config/nebulacrakmod/nebula.json
 */
public class NebulaConfig {

    public enum UIStyle { ORIGINAL, EDICION }
    public enum BackgroundMode { ORIGINAL, COLOR, IMAGE, TRANSPARENT }
    public enum LoadingBarMode { ORIGINAL, TRANSPARENT }
    public enum FontType { ARIAL, CONSOLAS }
    public enum ButtonStyle { DEFAULT, TRANSPARENT, METEOR }

    // -- General / CONFI --
    public static int openKeyCode = 192;
    public static boolean closeWithOpenKey = true;
    public static FontType fontType = FontType.ARIAL;
    public static ButtonStyle buttonStyle = ButtonStyle.DEFAULT;
    public static UIStyle uiStyle = UIStyle.ORIGINAL;

    // -- EDICION visual customization --
    public static int editionModuleBgColor = 0x66101820;
    public static int editionModuleBgHoverColor = 0x88202A38;
    public static int editionModuleActiveColor = 0x6634D399;
    public static int editionModuleInactiveColor = 0x4494A3B8;
    public static int editionBorderColor = 0x332D3342;
    public static int editionTextColor = 0xFFE5E7EB;
    public static int editionButtonColor = 0x551A2230;
    public static int editionButtonHoverColor = 0x88303A4D;
    public static int editionIndicatorColor = 0xFF8B5CF6;
    public static int editionDividerColor = 0x222D3342;
    public static int editionPanelBgColor = 0xCC10141C;
    public static int editionTopBarBgColor = 0xD00C1018;

    // -- Dropdown / panel layout state --
    public static final Map<String, int[]> dropdownPositions = new HashMap<>();
    public static final Map<String, Boolean> dropdownExpanded = new HashMap<>();

    // -- Per-screen backgrounds (EDIT,MC) --
    public static BackgroundMode mainMenuBackgroundMode = BackgroundMode.ORIGINAL;
    public static int mainMenuBackgroundColor = 0xFF121218;
    public static String mainMenuImagePath = "";

    public static BackgroundMode singleplayerBackgroundMode = BackgroundMode.ORIGINAL;
    public static int singleplayerBackgroundColor = 0xFF121218;
    public static String singleplayerImagePath = "";

    public static BackgroundMode multiplayerBackgroundMode = BackgroundMode.ORIGINAL;
    public static int multiplayerBackgroundColor = 0xFF121218;
    public static String multiplayerImagePath = "";

    public static BackgroundMode worldSelectBackgroundMode = BackgroundMode.ORIGINAL;
    public static int worldSelectBackgroundColor = 0xFF121218;
    public static String worldSelectImagePath = "";

    public static BackgroundMode worldCreateBackgroundMode = BackgroundMode.ORIGINAL;
    public static int worldCreateBackgroundColor = 0xFF121218;
    public static String worldCreateImagePath = "";

    public static BackgroundMode optionsBackgroundMode = BackgroundMode.ORIGINAL;
    public static int optionsBackgroundColor = 0xFF121218;
    public static String optionsImagePath = "";

    // Loading panels (Paneles de carga) - each configurable independently
    public static BackgroundMode mojangScreenBackgroundMode = BackgroundMode.ORIGINAL;
    public static int mojangScreenBackgroundColor = 0xFF121218;
    public static String mojangScreenImagePath = "";

    public static BackgroundMode worldLoadingBackgroundMode = BackgroundMode.ORIGINAL;
    public static int worldLoadingBackgroundColor = 0xFF121218;
    public static String worldLoadingImagePath = "";

    public static BackgroundMode connectingBackgroundMode = BackgroundMode.ORIGINAL;
    public static int connectingBackgroundColor = 0xFF121218;
    public static String connectingImagePath = "";

    public static BackgroundMode disconnectedBackgroundMode = BackgroundMode.ORIGINAL;
    public static int disconnectedBackgroundColor = 0xFF121218;
    public static String disconnectedImagePath = "";

    // Legacy aliases kept for backward compatibility with old configs
    public static BackgroundMode loadingScreenBackgroundMode = BackgroundMode.ORIGINAL;
    public static int loadingScreenBackgroundColor = 0xFF121218;
    public static String loadingScreenImagePath = "";

    // -- Loading bar --
    public static LoadingBarMode loadingBarMode = LoadingBarMode.ORIGINAL;

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("nebulacrakmod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("nebula.json");

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) return;

            JsonObject root = JsonParser.parseString(Files.readString(CONFIG_FILE, StandardCharsets.UTF_8)).getAsJsonObject();

            openKeyCode = getInt(root, "openKeyCode", openKeyCode);
            closeWithOpenKey = getBool(root, "closeWithOpenKey", closeWithOpenKey);
            fontType = parseEnum(FontType.class, root, "fontType", fontType);
            buttonStyle = parseEnum(ButtonStyle.class, root, "buttonStyle", buttonStyle);
            uiStyle = parseEnum(UIStyle.class, root, "uiStyle", uiStyle);

            editionModuleBgColor = getInt(root, "editionModuleBgColor", editionModuleBgColor);
            editionModuleBgHoverColor = getInt(root, "editionModuleBgHoverColor", editionModuleBgHoverColor);
            editionModuleActiveColor = getInt(root, "editionModuleActiveColor", editionModuleActiveColor);
            editionModuleInactiveColor = getInt(root, "editionModuleInactiveColor", editionModuleInactiveColor);
            editionBorderColor = getInt(root, "editionBorderColor", editionBorderColor);
            editionTextColor = getInt(root, "editionTextColor", editionTextColor);
            editionButtonColor = getInt(root, "editionButtonColor", editionButtonColor);
            editionButtonHoverColor = getInt(root, "editionButtonHoverColor", editionButtonHoverColor);
            editionIndicatorColor = getInt(root, "editionIndicatorColor", editionIndicatorColor);
            editionDividerColor = getInt(root, "editionDividerColor", editionDividerColor);
            editionPanelBgColor = getInt(root, "editionPanelBgColor", editionPanelBgColor);
            editionTopBarBgColor = getInt(root, "editionTopBarBgColor", editionTopBarBgColor);

            loadBackground(root, "mainMenu", () -> mainMenuBackgroundMode, v -> mainMenuBackgroundMode = v, () -> mainMenuBackgroundColor, v -> mainMenuBackgroundColor = v, () -> mainMenuImagePath, v -> mainMenuImagePath = v);
            loadBackground(root, "singleplayer", () -> singleplayerBackgroundMode, v -> singleplayerBackgroundMode = v, () -> singleplayerBackgroundColor, v -> singleplayerBackgroundColor = v, () -> singleplayerImagePath, v -> singleplayerImagePath = v);
            loadBackground(root, "multiplayer", () -> multiplayerBackgroundMode, v -> multiplayerBackgroundMode = v, () -> multiplayerBackgroundColor, v -> multiplayerBackgroundColor = v, () -> multiplayerImagePath, v -> multiplayerImagePath = v);
            loadBackground(root, "worldSelect", () -> worldSelectBackgroundMode, v -> worldSelectBackgroundMode = v, () -> worldSelectBackgroundColor, v -> worldSelectBackgroundColor = v, () -> worldSelectImagePath, v -> worldSelectImagePath = v);
            loadBackground(root, "worldCreate", () -> worldCreateBackgroundMode, v -> worldCreateBackgroundMode = v, () -> worldCreateBackgroundColor, v -> worldCreateBackgroundColor = v, () -> worldCreateImagePath, v -> worldCreateImagePath = v);
            loadBackground(root, "options", () -> optionsBackgroundMode, v -> optionsBackgroundMode = v, () -> optionsBackgroundColor, v -> optionsBackgroundColor = v, () -> optionsImagePath, v -> optionsImagePath = v);
            loadBackground(root, "mojang", () -> mojangScreenBackgroundMode, v -> mojangScreenBackgroundMode = v, () -> mojangScreenBackgroundColor, v -> mojangScreenBackgroundColor = v, () -> mojangScreenImagePath, v -> mojangScreenImagePath = v);
            loadBackground(root, "worldLoading", () -> worldLoadingBackgroundMode, v -> worldLoadingBackgroundMode = v, () -> worldLoadingBackgroundColor, v -> worldLoadingBackgroundColor = v, () -> worldLoadingImagePath, v -> worldLoadingImagePath = v);
            loadBackground(root, "connecting", () -> connectingBackgroundMode, v -> connectingBackgroundMode = v, () -> connectingBackgroundColor, v -> connectingBackgroundColor = v, () -> connectingImagePath, v -> connectingImagePath = v);
            loadBackground(root, "disconnected", () -> disconnectedBackgroundMode, v -> disconnectedBackgroundMode = v, () -> disconnectedBackgroundColor, v -> disconnectedBackgroundColor = v, () -> disconnectedImagePath, v -> disconnectedImagePath = v);
            loadBackground(root, "loading", () -> loadingScreenBackgroundMode, v -> loadingScreenBackgroundMode = v, () -> loadingScreenBackgroundColor, v -> loadingScreenBackgroundColor = v, () -> loadingScreenImagePath, v -> loadingScreenImagePath = v);

            // Backward compat: old single "loading" key maps to worldLoading when new keys absent
            if (!root.has("worldLoadingBackgroundMode") && root.has("loadingScreenBackgroundMode")) {
                worldLoadingBackgroundMode = loadingScreenBackgroundMode;
                worldLoadingBackgroundColor = loadingScreenBackgroundColor;
                worldLoadingImagePath = loadingScreenImagePath;
            }
            if (!root.has("connectingBackgroundMode") && root.has("loadingScreenBackgroundMode")) {
                connectingBackgroundMode = loadingScreenBackgroundMode;
                connectingBackgroundColor = loadingScreenBackgroundColor;
                connectingImagePath = loadingScreenImagePath;
            }

            loadingBarMode = parseEnum(LoadingBarMode.class, root, "loadingBarMode", loadingBarMode);

            dropdownPositions.clear();
            if (root.has("dropdownPositions") && root.get("dropdownPositions").isJsonObject()) {
                JsonObject dp = root.getAsJsonObject("dropdownPositions");
                for (String k : dp.keySet()) {
                    try {
                        JsonObject p = dp.getAsJsonObject(k);
                        dropdownPositions.put(k, new int[]{getInt(p, "x", 0), getInt(p, "y", 0)});
                    } catch (Exception ignored) {}
                }
            }
            dropdownExpanded.clear();
            if (root.has("dropdownExpanded") && root.get("dropdownExpanded").isJsonObject()) {
                JsonObject de = root.getAsJsonObject("dropdownExpanded");
                for (String k : de.keySet()) {
                    try { dropdownExpanded.put(k, de.get(k).getAsBoolean()); } catch (Exception ignored) {}
                }
            }

            validateImages();
        } catch (Exception ignored) {
        }
    }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int v); }
    private interface StrGetter { String get(); }
    private interface StrSetter { void set(String v); }
    private interface ModeGetter { BackgroundMode get(); }
    private interface ModeSetter { void set(BackgroundMode v); }

    private static void loadBackground(JsonObject root, String key, ModeGetter mg, ModeSetter ms, IntGetter cg, IntSetter cs, StrGetter pg, StrSetter ps) {
        String modeKey = key + "BackgroundMode";
        String colorKey = key + "BackgroundColor";
        String pathKey = key + "ImagePath";
        // backward compat for old keys
        if ("loading".equals(key) && !root.has(modeKey) && root.has("loadingScreenBackgroundMode")) modeKey = "loadingScreenBackgroundMode";
        if ("loading".equals(key) && !root.has(colorKey) && root.has("loadingScreenBackgroundColor")) colorKey = "loadingScreenBackgroundColor";
        if ("loading".equals(key) && !root.has(pathKey) && root.has("loadingScreenImagePath")) pathKey = "loadingScreenImagePath";
        ms.set(parseEnum(BackgroundMode.class, root, modeKey, mg.get()));
        cs.set(getInt(root, colorKey, cg.get()));
        ps.set(getString(root, pathKey, pg.get()));
    }

    private static void validateImage(ModeGetter mg, ModeSetter ms, StrGetter pg, StrSetter ps) {
        if (mg.get() == BackgroundMode.IMAGE && !pg.get().isEmpty() && !Files.exists(Path.of(pg.get()))) {
            ms.set(BackgroundMode.ORIGINAL);
            ps.set("");
        }
    }

    private static void validateImages() {
        validateImage(() -> mainMenuBackgroundMode, v -> mainMenuBackgroundMode = v, () -> mainMenuImagePath, v -> mainMenuImagePath = v);
        validateImage(() -> singleplayerBackgroundMode, v -> singleplayerBackgroundMode = v, () -> singleplayerImagePath, v -> singleplayerImagePath = v);
        validateImage(() -> multiplayerBackgroundMode, v -> multiplayerBackgroundMode = v, () -> multiplayerImagePath, v -> multiplayerImagePath = v);
        validateImage(() -> worldSelectBackgroundMode, v -> worldSelectBackgroundMode = v, () -> worldSelectImagePath, v -> worldSelectImagePath = v);
        validateImage(() -> worldCreateBackgroundMode, v -> worldCreateBackgroundMode = v, () -> worldCreateImagePath, v -> worldCreateImagePath = v);
        validateImage(() -> optionsBackgroundMode, v -> optionsBackgroundMode = v, () -> optionsImagePath, v -> optionsImagePath = v);
        validateImage(() -> mojangScreenBackgroundMode, v -> mojangScreenBackgroundMode = v, () -> mojangScreenImagePath, v -> mojangScreenImagePath = v);
        validateImage(() -> worldLoadingBackgroundMode, v -> worldLoadingBackgroundMode = v, () -> worldLoadingImagePath, v -> worldLoadingImagePath = v);
        validateImage(() -> connectingBackgroundMode, v -> connectingBackgroundMode = v, () -> connectingImagePath, v -> connectingImagePath = v);
        validateImage(() -> disconnectedBackgroundMode, v -> disconnectedBackgroundMode = v, () -> disconnectedImagePath, v -> disconnectedImagePath = v);
        validateImage(() -> loadingScreenBackgroundMode, v -> loadingScreenBackgroundMode = v, () -> loadingScreenImagePath, v -> loadingScreenImagePath = v);
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            JsonObject root = new JsonObject();
            root.addProperty("openKeyCode", openKeyCode);
            root.addProperty("closeWithOpenKey", closeWithOpenKey);
            root.addProperty("fontType", fontType.name());
            root.addProperty("buttonStyle", buttonStyle.name());
            root.addProperty("uiStyle", uiStyle.name());

            root.addProperty("editionModuleBgColor", editionModuleBgColor);
            root.addProperty("editionModuleBgHoverColor", editionModuleBgHoverColor);
            root.addProperty("editionModuleActiveColor", editionModuleActiveColor);
            root.addProperty("editionModuleInactiveColor", editionModuleInactiveColor);
            root.addProperty("editionBorderColor", editionBorderColor);
            root.addProperty("editionTextColor", editionTextColor);
            root.addProperty("editionButtonColor", editionButtonColor);
            root.addProperty("editionButtonHoverColor", editionButtonHoverColor);
            root.addProperty("editionIndicatorColor", editionIndicatorColor);
            root.addProperty("editionDividerColor", editionDividerColor);
            root.addProperty("editionPanelBgColor", editionPanelBgColor);
            root.addProperty("editionTopBarBgColor", editionTopBarBgColor);

            saveBackground(root, "mainMenu", mainMenuBackgroundMode, mainMenuBackgroundColor, mainMenuImagePath);
            saveBackground(root, "singleplayer", singleplayerBackgroundMode, singleplayerBackgroundColor, singleplayerImagePath);
            saveBackground(root, "multiplayer", multiplayerBackgroundMode, multiplayerBackgroundColor, multiplayerImagePath);
            saveBackground(root, "worldSelect", worldSelectBackgroundMode, worldSelectBackgroundColor, worldSelectImagePath);
            saveBackground(root, "worldCreate", worldCreateBackgroundMode, worldCreateBackgroundColor, worldCreateImagePath);
            saveBackground(root, "options", optionsBackgroundMode, optionsBackgroundColor, optionsImagePath);
            saveBackground(root, "mojang", mojangScreenBackgroundMode, mojangScreenBackgroundColor, mojangScreenImagePath);
            saveBackground(root, "worldLoading", worldLoadingBackgroundMode, worldLoadingBackgroundColor, worldLoadingImagePath);
            saveBackground(root, "connecting", connectingBackgroundMode, connectingBackgroundColor, connectingImagePath);
            saveBackground(root, "disconnected", disconnectedBackgroundMode, disconnectedBackgroundColor, disconnectedImagePath);
            saveBackground(root, "loading", loadingScreenBackgroundMode, loadingScreenBackgroundColor, loadingScreenImagePath);
            // backward compat keys mirror worldLoading
            root.addProperty("loadingScreenBackgroundMode", worldLoadingBackgroundMode.name());
            root.addProperty("loadingScreenBackgroundColor", worldLoadingBackgroundColor);
            root.addProperty("loadingScreenImagePath", worldLoadingImagePath);

            root.addProperty("loadingBarMode", loadingBarMode.name());

            JsonObject dp = new JsonObject();
            for (Map.Entry<String, int[]> e : dropdownPositions.entrySet()) {
                JsonObject p = new JsonObject();
                p.addProperty("x", e.getValue()[0]);
                p.addProperty("y", e.getValue()[1]);
                dp.add(e.getKey(), p);
            }
            root.add("dropdownPositions", dp);

            JsonObject de = new JsonObject();
            for (Map.Entry<String, Boolean> e : dropdownExpanded.entrySet()) de.addProperty(e.getKey(), e.getValue());
            root.add("dropdownExpanded", de);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(CONFIG_FILE, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static void saveBackground(JsonObject root, String key, BackgroundMode mode, int color, String path) {
        root.addProperty(key + "BackgroundMode", mode.name());
        root.addProperty(key + "BackgroundColor", color);
        root.addProperty(key + "ImagePath", path);
    }

    public static ScreenBackground getScreenBackground(String id) {
        return switch (id) {
            case "mainMenu" -> new ScreenBackground(mainMenuBackgroundMode, mainMenuBackgroundColor, mainMenuImagePath);
            case "singleplayer", "worldSelect" -> new ScreenBackground(worldSelectBackgroundMode, worldSelectBackgroundColor, worldSelectImagePath);
            case "multiplayer" -> new ScreenBackground(multiplayerBackgroundMode, multiplayerBackgroundColor, multiplayerImagePath);
            case "worldCreate" -> new ScreenBackground(worldCreateBackgroundMode, worldCreateBackgroundColor, worldCreateImagePath);
            case "options" -> new ScreenBackground(optionsBackgroundMode, optionsBackgroundColor, optionsImagePath);
            case "mojang" -> new ScreenBackground(mojangScreenBackgroundMode, mojangScreenBackgroundColor, mojangScreenImagePath);
            case "worldLoading", "loading" -> new ScreenBackground(worldLoadingBackgroundMode, worldLoadingBackgroundColor, worldLoadingImagePath);
            case "connecting" -> new ScreenBackground(connectingBackgroundMode, connectingBackgroundColor, connectingImagePath);
            case "disconnected" -> new ScreenBackground(disconnectedBackgroundMode, disconnectedBackgroundColor, disconnectedImagePath);
            default -> new ScreenBackground(BackgroundMode.ORIGINAL, 0xFF121218, "");
        };
    }

    public record ScreenBackground(BackgroundMode mode, int color, String imagePath) {}

    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, JsonObject obj, String key, T fallback) {
        try {
            if (!obj.has(key)) return fallback;
            return Enum.valueOf(clazz, obj.get(key).getAsString());
        } catch (Exception e) {
            return fallback;
        }
    }
    private static int getInt(JsonObject o, String k, int d) { try { return o.get(k).getAsInt(); } catch (Exception e) { return d; } }
    private static boolean getBool(JsonObject o, String k, boolean d) { try { return o.get(k).getAsBoolean(); } catch (Exception e) { return d; } }
    private static String getString(JsonObject o, String k, String d) { try { return o.get(k).getAsString(); } catch (Exception e) { return d; } }
}
