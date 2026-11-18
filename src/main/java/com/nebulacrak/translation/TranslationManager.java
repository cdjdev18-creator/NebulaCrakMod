package com.nebulacrak.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulacrak.language.LanguageManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private static final Map<String, String> entries = new HashMap<>();

    public static void load() {
        entries.clear();
        String file = "/translations/" + LanguageManager.code() + ".json";
        try (InputStream in = TranslationManager.class.getResourceAsStream(file)) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : root.keySet()) entries.put(key, root.get(key).getAsString());
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
}
EOF