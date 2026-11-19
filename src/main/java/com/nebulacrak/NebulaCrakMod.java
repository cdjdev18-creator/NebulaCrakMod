package com.nebulacrak;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.translation.TranslationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NebulaCrakMod extends MeteorAddon {
    public static final String NAME = "NebulaCrakMod";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static NebulaCrakMod INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        LOG.info("Initializing NebulaCrakMod...");
        LanguageManager.load();
        TranslationManager.load();
        ConfiColorsConfig.load();
        NebulaConfig.load();
        NebulaModules.registerModules();
        LOG.info("NebulaCrakMod initialized successfully!");
    }

    @Override
    public void onRegisterCategories() {
        NebulaModules.registerCategories();
    }

    @Override
    public String getPackage() {
        return "com.nebulacrak";
    }
}
