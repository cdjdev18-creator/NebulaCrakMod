package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MuteChatPlusModule extends Module {
    public static MuteChatPlusModule INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMuted = settings.createGroup("Muted Players");

    public final Setting<Boolean> muteChat = sgGeneral.add(new BoolSetting.Builder()
        .name("mute-chat+")
        .description("Hide normal chat messages from muted players.")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Never mute Meteor friends.")
        .defaultValue(true)
        .build());

    private final Setting<List<String>> mutedPlayers = sgMuted.add(new StringListSetting.Builder()
        .name("muted-players")
        .description("Player names or UUIDs to mute (one per entry).")
        .defaultValue(new ArrayList<>())
        .build());

    private final Setting<String> addPlayer = sgMuted.add(new StringSetting.Builder()
        .name("add-player")
        .description("Type a player name or UUID and toggle Add.")
        .defaultValue("")
        .build());

    private final Setting<Boolean> addAction = sgMuted.add(new BoolSetting.Builder()
        .name("add")
        .description("Add the player from add-player to the muted list.")
        .defaultValue(false)
        .build());

    private final Setting<String> removePlayer = sgMuted.add(new StringSetting.Builder()
        .name("remove-player")
        .description("Type a player name or UUID and toggle Remove.")
        .defaultValue("")
        .build());

    private final Setting<Boolean> removeAction = sgMuted.add(new BoolSetting.Builder()
        .name("remove")
        .description("Remove the player from the muted list.")
        .defaultValue(false)
        .build());

    private final Set<String> mutedNamesLower = ConcurrentHashMap.newKeySet();
    private final Set<String> mutedUuidsLower = ConcurrentHashMap.newKeySet();
    private boolean resetting;

    public MuteChatPlusModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "MuteChat+", "Hide chat messages from muted players.");
        INSTANCE = this;
        rebuildCache();
    }

    @Override
    public void onActivate() {
        rebuildCache();
        info(TranslationManager.get("nebula.mutechat.enabled", "[MuteChat+] Activado"));
    }

    @Override
    public void onDeactivate() {
        info(TranslationManager.get("nebula.mutechat.disabled", "[MuteChat+] Desactivado"));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (addAction.get()) { addMuted(addPlayer.get()); resetBool(addAction); }
        if (removeAction.get()) { removeMuted(removePlayer.get()); resetBool(removeAction); }
    }

    private void resetBool(Setting<Boolean> setting) {
        if (resetting) return;
        resetting = true;
        try { setting.set(false); } finally { resetting = false; }
    }

    public void rebuildCache() {
        mutedNamesLower.clear();
        mutedUuidsLower.clear();
        List<String> list = mutedPlayers.get();
        if (list == null) return;
        for (String raw : list) {
            String s = normalize(raw);
            if (s.isEmpty()) continue;
            if (isUuid(s)) mutedUuidsLower.add(s.toLowerCase(Locale.ROOT));
            else mutedNamesLower.add(s.toLowerCase(Locale.ROOT));
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String v = s.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() > 1) v = v.substring(1, v.length() - 1).trim();
        return v;
    }

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; } catch (Exception ignored) { return false; }
    }

    private void addMuted(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) { info("Escribe un nombre o UUID."); return; }
        List<String> list = new ArrayList<>(mutedPlayers.get());
        boolean exists = list.stream().anyMatch(e -> normalize(e).equalsIgnoreCase(s));
        if (!exists) {
            list.add(s);
            mutedPlayers.set(list);
            rebuildCache();
            info("Muteado: " + s);
        } else {
            info("Ya estaba muteado: " + s);
        }
        addPlayer.set("");
    }

    private void removeMuted(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) { info("Escribe un nombre o UUID para eliminar."); return; }
        List<String> list = new ArrayList<>(mutedPlayers.get());
        boolean removed = list.removeIf(e -> normalize(e).equalsIgnoreCase(s));
        if (removed) {
            mutedPlayers.set(list);
            rebuildCache();
            info("Desmuteado: " + s);
        } else {
            info("No encontrado en lista mute: " + s);
        }
        removePlayer.set("");
    }

    public boolean shouldMutePlayer(UUID uuid, String name) {
        if (!isActive() || !muteChat.get()) return false;
        rebuildCacheIfNeeded();

        if (uuid != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && uuid.equals(mc.player.getUUID())) return false;
            if (mutedUuidsLower.contains(uuid.toString().toLowerCase(Locale.ROOT))) return true;
        }

        String n = normalize(name);
        if (n.isEmpty()) return false;
        String nl = n.toLowerCase(Locale.ROOT);

        if (ignoreFriends.get()) {
            try {
                if (Friends.get().get(n) != null) return false;
            } catch (Throwable ignored) {}
        }

        return mutedNamesLower.contains(nl);
    }

    private long lastCacheRebuild;
    private void rebuildCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheRebuild > 1000) {
            lastCacheRebuild = now;
            rebuildCache();
        }
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
        });
    }
}
