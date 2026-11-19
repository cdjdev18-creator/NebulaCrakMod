package com.nebulacrak.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulacrak.language.LanguageManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TranslationManager {
    private static final Map<String, String> entries = new HashMap<>();

    public static void load() {
        entries.clear();
        String file = "/translations/" + LanguageManager.code() + ".json";
        try (InputStream in = TranslationManager.class.getResourceAsStream(file)) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : root.keySet()) {
                entries.put(key, root.get(key).getAsString());
            }
        } catch (Exception ignored) {
        }
        if (entries.isEmpty() && !LanguageManager.code().equals("en_us")) {
            LanguageManager.set(LanguageManager.Language.EN_US);
            load();
        }
    }

    public static String get(String key, String fallback) {
        return entries.getOrDefault(key, fallback);
    }

    public static void reload() {
        load();
    }

    /**
     * Obtiene el nombre visual de una categoría de Meteor o Nebula traducido,
     * o el nombre original si no existe traducción en nuestro diccionario.
     */
    public static String getCategoryName(String originalName) {
        if (originalName == null || originalName.isEmpty()) return "";

        // Claves directas como nebula.category.player
        if (entries.containsKey(originalName)) {
            return entries.get(originalName);
        }

        // Claves de categoría Meteor: meteor.category.<nombre>
        String cleanCat = originalName.toLowerCase(Locale.ROOT).replace(" ", "_");
        String meteorKey = "meteor.category." + cleanCat;
        if (entries.containsKey(meteorKey)) {
            return entries.get(meteorKey);
        }

        return originalName;
    }

    /**
     * Obtiene el nombre visual de un módulo traducido, o el nombre original
     * si no existe traducción en nuestro diccionario.
     */
    public static String getModuleName(String originalName) {
        if (originalName == null || originalName.isEmpty()) return "";

        // 1. Claves de Nebula: nebula.module.<nombre>
        String nebulaKey = "nebula.module." + originalName.toLowerCase(Locale.ROOT).replace(" ", "-");
        if (entries.containsKey(nebulaKey)) {
            return entries.get(nebulaKey);
        }

        // 2. Normalización de Meteor sin guiones ni espacios (ej: KillAura -> meteor.module.killaura)
        String cleanName = originalName.toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("+", "");
        String meteorKey = "meteor.module." + cleanName;
        if (entries.containsKey(meteorKey)) {
            return entries.get(meteorKey);
        }

        // 3. Normalización con guiones (ej: meteor.module.kill-aura)
        String hyphenKey = "meteor.module." + originalName.toLowerCase(Locale.ROOT).replace(" ", "-");
        if (entries.containsKey(hyphenKey)) {
            return entries.get(hyphenKey);
        }

        return originalName;
    }

    /**
     * Obtiene la descripción visual traducida de un módulo si existe en nuestro diccionario.
     */
    public static String getModuleDescription(String originalName, String fallback) {
        if (originalName == null || originalName.isEmpty()) return fallback;

        String cleanName = originalName.toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace(" ", "")
            .replace("_", "")
            .replace("+", "");
        String key = "meteor.module." + cleanName + ".description";
        if (entries.containsKey(key)) {
            return entries.get(key);
        }

        String hyphenKey = "meteor.module." + originalName.toLowerCase(Locale.ROOT).replace(" ", "-") + ".description";
        if (entries.containsKey(hyphenKey)) {
            return entries.get(hyphenKey);
        }

        String nebulaKey = "nebula.module." + originalName.toLowerCase(Locale.ROOT).replace(" ", "-") + ".description";
        if (entries.containsKey(nebulaKey)) {
            return entries.get(nebulaKey);
        }

        return fallback;
    }
}
