package com.nebulacrak;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.modules.MuteChatPlusModule;
import com.nebulacrak.modules.TranslatePlusModule;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
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
        TranslatePlusModule.register();
        MuteChatPlusModule.register();
        LOG.info("NebulaCrakMod initialized successfully!");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(TranslatePlusModule.CATEGORY);
    }

    public void onRegisterCommands() {
    }

    @Override
    public String getPackage() {
        return "com.nebulacrak";
    }
}
