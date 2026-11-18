package com.nebulacrak.modules;

import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdjModule extends Module {
    private final SettingGroup sgTranslate = settings.createGroup("Translate+");
    private final SettingGroup sgMuteChat = settings.createGroup("MuteChat+");
    private final SettingGroup sgDefaults = settings.createGroup("Defaults");
    private final SettingGroup sgAbout = settings.createGroup("About");

    public final Setting<LanguageManager.Language> language;
    public final Setting<Boolean> translateMeteor;
    public final Setting<LanguageManager.Language> translationMode;
    public final Setting<Boolean> muteChat;
    public Setting<Boolean> restoreDefaults;

    private boolean syncing;
    private static final Pattern SENDER = Pattern.compile("^<([^>]+)>");

    public CdjModule() {
        super(Categories.Misc, "+CDJ", "Main customization and tools module for NebulaCrakMod.");

        language = sgTranslate.add(new EnumSetting.Builder<LanguageManager.Language>()
            .name("language")
            .description("Interface language.")
            .defaultValue(LanguageManager.Language.EN_US)
            .onChanged(value -> {
                LanguageManager.set(value);
                TranslationManager.reload();
                syncLanguage(value, true);
            })
            .build());

        translateMeteor = sgTranslate.add(new BoolSetting.Builder()
            .name("translate+")
            .description("Enable Meteor translations.")
            .defaultValue(false)
            .build());

        translationMode = sgTranslate.add(new EnumSetting.Builder<LanguageManager.Language>()
            .name("translation-mode")
            .description("Language used for Meteor translations.")
            .defaultValue(LanguageManager.Language.EN_US)
            .visible(() -> translateMeteor.get())
            .onChanged(value -> {
                LanguageManager.set(value);
                TranslationManager.reload();
                syncLanguage(value, false);
            })
            .build());

        muteChat = sgMuteChat.add(new BoolSetting.Builder()
            .name("mute-chat+")
            .description("Hide normal messages from non-friends.")
            .defaultValue(false)
            .build());

        restoreDefaults = sgDefaults.add(new BoolSetting.Builder()
            .name("restore-defaults")
            .description("Restore NebulaCrakMod defaults. Toggle on to confirm.")
            .defaultValue(false)
            .onChanged(value -> {
                if (value && !syncing) {
                    syncing = true;
                    LanguageManager.set(LanguageManager.Language.EN_US);
                    language.set(LanguageManager.Language.EN_US);
                    translationMode.set(LanguageManager.Language.EN_US);
                    translateMeteor.set(false);
                    muteChat.set(false);
                    restoreDefaults.set(false);
                    syncing = false;
                }
            })
            .build());

        sgAbout.add(new StringSetting.Builder()
            .name("nebulacrakmod")
            .description("NebulaCrakMod")
            .defaultValue("NebulaCrakMod")
            .build());
        sgAbout.add(new StringSetting.Builder()
            .name("module")
            .description("+CDJ")
            .defaultValue("+CDJ")
            .build());
        sgAbout.add(new StringSetting.Builder()
            .name("author")
            .description("CDJ dev 18")
            .defaultValue("CDJ dev 18")
            .build());
        sgAbout.add(new StringSetting.Builder()
            .name("minecraft")
            .description("1.21.11")
            .defaultValue("1.21.11")
            .build());
        sgAbout.add(new StringSetting.Builder()
            .name("meteor")
            .description("1.21.11-SNAPSHOT")
            .defaultValue("1.21.11-SNAPSHOT")
            .build());
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive() || !muteChat.get()) return;
        Matcher matcher = SENDER.matcher(event.getMessage().getString());
        if (!matcher.find()) return;
        String sender = matcher.group(1);
        if (Friends.get().get(sender) != null) return;
        event.cancel();
    }

    private void syncLanguage(LanguageManager.Language value, boolean fromLanguage) {
        if (syncing) return;
        syncing = true;
        if (fromLanguage) {
            translationMode.set(value);
        } else {
            language.set(value);
        }
        syncing = false;
    }

    public static void register() {
        Modules.get().add(new CdjModule());
    }
}
