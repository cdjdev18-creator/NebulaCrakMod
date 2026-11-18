package com.nebulacrak.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuteChatPlusModule extends Module {
    public static final Category CATEGORY = TranslatePlusModule.CATEGORY;

    public static MuteChatPlusModule INSTANCE;
    public Setting<Boolean> muteChat;
    private static final Pattern SENDER = Pattern.compile("^<([^>]+)>");

    public MuteChatPlusModule() {
        super(CATEGORY, "MuteChat+", "Hide chat messages from non-friends.");
        INSTANCE = this;
        SettingGroup sg = settings.getDefaultGroup();
        muteChat = sg.add(new BoolSetting.Builder()
            .name("mute-chat+").description("Hide normal messages from non-friends.")
            .defaultValue(false).build());
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive() || !muteChat.get()) return;
        Matcher m = SENDER.matcher(event.getMessage().getString());
        if (!m.find()) return;
        String sender = m.group(1);
        if (Friends.get().get(sender) != null) return;
        event.cancel();
    }

    public static void register() { Modules.get().add(new MuteChatPlusModule()); }
}
