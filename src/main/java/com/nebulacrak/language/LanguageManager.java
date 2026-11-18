package com.nebulacrak.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class LanguageManager {
    public enum Language { EN_US, ES_MX }

    private static Language current = Language.EN_US;
    private static final Path CONFIG = Paths.get("config", "nebulacrakmod.properties");

    public static void load() {
        try {
            if (Files.exists(CONFIG)) {
                Properties p = new Properties();
                p.load(Files.newInputStream(CONFIG));
                String v = p.getProperty("language", "en_us");
                current = v.equalsIgnoreCase("es_mx") ? Language.ES_MX : Language.EN_US;
            }
        } catch (Exception ignored) {
        }
    }

    public static void set(Language language) {
        current = language;
        save();
    }

    public static Language get() {
        return current;
    }

    public static String code() {
        return current == Language.ES_MX ? "es_mx" : "en_us";
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Properties p = new Properties();
            p.setProperty("language", code());
            p.store(Files.newOutputStream(CONFIG), "NebulaCrakMod");
        } catch (Exception ignored) {
        }
    }
}
