package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;

public class MuteChatPlusModule extends Module {
    public static final Category CATEGORY = NebulaModules.NEBULA_CDJ_DEV;

    public static MuteChatPlusModule INSTANCE;
    public Setting<Boolean> muteChat;

    public MuteChatPlusModule() {
        super(CATEGORY, "MuteChat+", "Hide chat messages from non-friends.");
        INSTANCE = this;
        SettingGroup sg = settings.getDefaultGroup();
        muteChat = sg.add(new BoolSetting.Builder()
            .name("mute-chat+").description("Hide normal messages from non-friends.")
            .defaultValue(true).build());
    }

    @Override
    public void onActivate() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    TranslationManager.get("nebula.mutechat.enabled", "[MuteChat+] Activado")
                ), false);
        }
    }

    @Override
    public void onDeactivate() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    TranslationManager.get("nebula.mutechat.disabled", "[MuteChat+] Desactivado")
                ), false);
        }
    }

    public static void register() {
        if (INSTANCE == null) {
            Modules.get().add(new MuteChatPlusModule());
        }
    }
}
