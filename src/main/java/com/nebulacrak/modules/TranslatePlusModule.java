package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaGUIScreen;
import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.client.Minecraft;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class TranslatePlusModule extends Module {
    public static TranslatePlusModule INSTANCE;

    public final Setting<LanguageManager.Language> language;
    public final Setting<Boolean> translateMeteor;
    public final Setting<LanguageManager.Language> translationMode;

    private boolean syncing;

    public TranslatePlusModule() {
        super(NebulaModules.NEBULA, "Translate+", "Translation settings for NebulaCrakMod.");
        INSTANCE = this;
        SettingGroup sg = settings.getDefaultGroup();
        language = sg.add(new EnumSetting.Builder<LanguageManager.Language>()
            .name("language").description("Interface language.")
            .defaultValue(LanguageManager.Language.EN_US)
            .onChanged(v -> { LanguageManager.set(v); TranslationManager.reload(); sync(v, true); })
            .build());
        translateMeteor = sg.add(new BoolSetting.Builder()
            .name("translate+").description("Enable Meteor translations.")
            .defaultValue(false).build());

        sg.add(new BoolSetting.Builder()
            .name("open-module-browser").description("Open NebulaCrakMod module browser.")
            .defaultValue(false)
            .onChanged(v -> {
                if (v) {
                    Minecraft.getInstance().setScreen(new NebulaGUIScreen(Minecraft.getInstance().screen));
                    Setting<?> setting = sg.get("open-module-browser");
                    if (setting instanceof BoolSetting boolSetting) {
                        boolSetting.set(false);
                    }
                }
            })
            .build());
        translationMode = sg.add(new EnumSetting.Builder<LanguageManager.Language>()
            .name("translation-mode").description("Language used for Meteor translations.")
            .defaultValue(LanguageManager.Language.EN_US)
            .visible(() -> translateMeteor.get())
            .onChanged(v -> { LanguageManager.set(v); TranslationManager.reload(); sync(v, false); })
            .build());
    }

    private void sync(LanguageManager.Language v, boolean fromLanguage) {
        if (syncing) return;
        syncing = true;
        if (fromLanguage) translationMode.set(v); else language.set(v);
        syncing = false;
    }

    public static void register() {
        if (INSTANCE == null) {
            Modules.get().add(new TranslatePlusModule());
        }
    }
}
