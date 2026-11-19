package com.nebulacrak.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;

/**
 * Persiste la configuración visual de HUD (antes ConfiColors) en:
 * .minecraft/config/nebulacrakmod/hud.json
 */
public class ConfiColorsConfig {

    // ── Fondo ────────────────────────────────────────────────────────────────
    public enum FondoMode { DISABLED, COLOR, IMAGE }
    public static FondoMode fondoMode       = FondoMode.DISABLED;
    public static int        fondoColor     = 0xFF121218;   // color por defecto
    public static String     fondoImagePath = "";
    public static String     fondoImageBase64 = ""; // Imagen incrustada en base64

    // ── PanelCargas ──────────────────────────────────────────────────────────
    public static boolean panelCargasVisible      = false;
    public static boolean panelCargasTransparent  = false;
    public static int     panelCargasColor        = 0xFFEF4444; // rojo inicial

    // ── Splash ───────────────────────────────────────────────────────────────
    public enum SplashMode { DISABLED, COLOR, IMAGE }
    public static SplashMode splashMode       = SplashMode.DISABLED;
    public static int         splashColor     = 0xFF8B5CF6;
    public static String      splashImagePath = "";
    public static String      splashImageBase64 = ""; // Imagen incrustada en base64

    // ── Ruta del archivo ─────────────────────────────────────────────────────
    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir()
               .resolve("nebulacrakmod").resolve("hud.json");
    }

    // ── Guardado ─────────────────────────────────────────────────────────────
    public static void save() {
        try {
            Path file = configFile();
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();

            JsonObject fondo = new JsonObject();
            fondo.addProperty("mode",        fondoMode.name());
            fondo.addProperty("color",       fondoColor);
            fondo.addProperty("imagePath",   fondoImagePath);
            fondo.addProperty("imageBase64", fondoImageBase64);
            root.add("fondo", fondo);

            JsonObject panel = new JsonObject();
            panel.addProperty("visible",     panelCargasVisible);
            panel.addProperty("transparent", panelCargasTransparent);
            panel.addProperty("color",       panelCargasColor);
            root.add("panelCargas", panel);

            JsonObject splash = new JsonObject();
            splash.addProperty("mode",        splashMode.name());
            splash.addProperty("color",       splashColor);
            splash.addProperty("imagePath",   splashImagePath);
            splash.addProperty("imageBase64", splashImageBase64);
            root.add("splash", splash);

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // fallo silencioso — no detiene el juego
        }
    }

    // ── Carga ─────────────────────────────────────────────────────────────────
    public static void load() {
        try {
            Path file = configFile();
            if (!Files.exists(file)) return;

            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            if (root.has("fondo")) {
                JsonObject f = root.getAsJsonObject("fondo");
                fondoMode      = parseEnum(FondoMode.class,  f, "mode",      FondoMode.DISABLED);
                fondoColor     = getInt(f, "color", fondoColor);
                fondoImagePath = getString(f, "imagePath", "");
                // Verificar que la imagen sigue existiendo
                if (fondoMode == FondoMode.IMAGE && !fondoImagePath.isEmpty()
                        && !Files.exists(Path.of(fondoImagePath))) {
                    fondoMode = FondoMode.DISABLED;
                    fondoImagePath = "";
                }
            }

            if (root.has("panelCargas")) {
                JsonObject p = root.getAsJsonObject("panelCargas");
                panelCargasVisible     = getBool(p, "visible",     false);
                panelCargasTransparent = getBool(p, "transparent", false);
                panelCargasColor       = getInt(p,  "color",       panelCargasColor);
            }

            if (root.has("splash")) {
                JsonObject s = root.getAsJsonObject("splash");
                splashMode      = parseEnum(SplashMode.class, s, "mode",      SplashMode.DISABLED);
                splashColor     = getInt(s, "color", splashColor);
                splashImagePath = getString(s, "imagePath", "");
                if (splashMode == SplashMode.IMAGE && !splashImagePath.isEmpty()
                        && !Files.exists(Path.of(splashImagePath))) {
                    splashMode = SplashMode.DISABLED;
                    splashImagePath = "";
                }
            }
        } catch (Exception e) {
            // fichero corrupto → valores por defecto
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static <T extends Enum<T>> T parseEnum(Class<T> clazz, JsonObject obj, String key, T fallback) {
        try { return Enum.valueOf(clazz, obj.get(key).getAsString()); }
        catch (Exception e) { return fallback; }
    }
    private static int    getInt   (JsonObject o, String k, int    d) { try { return o.get(k).getAsInt();     } catch (Exception e) { return d; } }
    private static boolean getBool (JsonObject o, String k, boolean d){ try { return o.get(k).getAsBoolean(); } catch (Exception e) { return d; } }
    private static String  getString(JsonObject o, String k, String d){ try { return o.get(k).getAsString();  } catch (Exception e) { return d; } }
}
