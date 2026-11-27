package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MuteChatPlusModule extends Module {
    public static MuteChatPlusModule INSTANCE;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMuted = settings.createGroup("Muted Players");
    private final SettingGroup sgExceptions = settings.createGroup("Exceptions");
    private final SettingGroup sgFilters = settings.createGroup("Filters");
    private final SettingGroup sgStats = settings.createGroup("Statistics");

    // General
    public final Setting<Boolean> friendsOnlyChat = sgGeneral.add(new BoolSetting.Builder()
        .name("friends-only-chat")
        .description("Only display normal player chat messages from players in Meteor Friends.")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Never mute Meteor friends.")
        .defaultValue(true)
        .onChanged(v -> invalidateCache())
        .build());

    // Muted players
    private final Setting<List<String>> mutedPlayers = sgMuted.add(new StringListSetting.Builder()
        .name("muted-players")
        .description("Player names or UUIDs to mute (one per entry).")
        .defaultValue(new ArrayList<>())
        .onChanged(v -> invalidateCache())
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

    private final Setting<String> searchPlayer = sgMuted.add(new StringSetting.Builder()
        .name("search-player")
        .description("Search within the muted list (name or UUID fragment).")
        .defaultValue("")
        .onChanged(v -> updateSearchResult())
        .build());

    private final Setting<String> searchResult = sgMuted.add(new StringSetting.Builder()
        .name("search-result")
        .description("Matches found in the muted list.")
        .defaultValue("-")
        .build());

    // Exceptions
    private final Setting<List<String>> exceptionPlayers = sgExceptions.add(new StringListSetting.Builder()
        .name("exceptions")
        .description("Players who are never muted, even if in muted list. Names or UUIDs.")
        .defaultValue(new ArrayList<>())
        .onChanged(v -> invalidateCache())
        .build());

    private final Setting<String> addException = sgExceptions.add(new StringSetting.Builder()
        .name("add-exception")
        .description("Type a player name or UUID and toggle Add Exception.")
        .defaultValue("")
        .build());

    private final Setting<Boolean> addExceptionAction = sgExceptions.add(new BoolSetting.Builder()
        .name("add-exception-action")
        .description("Add the player from add-exception to the exceptions list.")
        .defaultValue(false)
        .build());

    private final Setting<String> removeException = sgExceptions.add(new StringSetting.Builder()
        .name("remove-exception")
        .description("Type a player name or UUID and toggle Remove Exception.")
        .defaultValue("")
        .build());

    private final Setting<Boolean> removeExceptionAction = sgExceptions.add(new BoolSetting.Builder()
        .name("remove-exception-action")
        .description("Remove the player from the exceptions list.")
        .defaultValue(false)
        .build());

    // Filters
    public final Setting<Boolean> mutePlayerChat = sgFilters.add(new BoolSetting.Builder()
        .name("mute-player-chat")
        .description("Hide normal player chat messages from muted players.")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> mutePrivateMessages = sgFilters.add(new BoolSetting.Builder()
        .name("mute-private-messages")
        .description("Hide identifiable private messages from muted players.")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> mutePlayerJoin = sgFilters.add(new BoolSetting.Builder()
        .name("mute-player-join")
        .description("Hide identifiable player join messages for muted players.")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> mutePlayerLeave = sgFilters.add(new BoolSetting.Builder()
        .name("mute-player-leave")
        .description("Hide identifiable player leave messages for muted players.")
        .defaultValue(false)
        .build());

    // Statistics
    private final Setting<String> statTotalHidden = sgStats.add(new StringSetting.Builder()
        .name("hidden-total")
        .description("Messages hidden during this session.")
        .defaultValue("0")
        .build());
    private final Setting<String> statPrivateHidden = sgStats.add(new StringSetting.Builder()
        .name("hidden-private")
        .description("Private messages hidden during this session.")
        .defaultValue("0")
        .build());
    private final Setting<String> statJoinLeaveHidden = sgStats.add(new StringSetting.Builder()
        .name("hidden-join-leave")
        .description("Join/leave messages hidden during this session.")
        .defaultValue("0")
        .build());
    private final Setting<String> statMutedCount = sgStats.add(new StringSetting.Builder()
        .name("muted-count")
        .description("Currently muted players/UUIDs in the list.")
        .defaultValue("0")
        .build());
    private final Setting<Boolean> resetStatsAction = sgStats.add(new BoolSetting.Builder()
        .name("reset-stats")
        .description("Reset session statistics.")
        .defaultValue(false)
        .build());

    private final Set<String> mutedNamesLower = ConcurrentHashMap.newKeySet();
    private final Set<String> mutedUuidsLower = ConcurrentHashMap.newKeySet();
    private final Set<String> exceptionNamesLower = ConcurrentHashMap.newKeySet();
    private final Set<String> exceptionUuidsLower = ConcurrentHashMap.newKeySet();
    private volatile boolean cacheDirty = true;
    private boolean resetting;

    private int hiddenTotal;
    private int hiddenPrivate;
    private int hiddenJoinLeave;

    public MuteChatPlusModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "MuteChat+", "Configurable chat filtering for muted players.");
        INSTANCE = this;
        invalidateCache();
    }

    @Override
    public void onActivate() {
        invalidateCache();
        rebuildCacheIfNeeded();
        updateStats();
        info(TranslationManager.get("nebula.mutechat.enabled", "[MuteChat+] Activado"));
    }

    @Override
    public void onDeactivate() {
        info(TranslationManager.get("nebula.mutechat.disabled", "[MuteChat+] Desactivado"));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (addAction.get()) { addToList(mutedPlayers, addPlayer, "Muteado", "mute"); resetBool(addAction); }
        if (removeAction.get()) { removeFromList(mutedPlayers, removePlayer, "Desmuteado", "mute"); resetBool(removeAction); }
        if (addExceptionAction.get()) { addToList(exceptionPlayers, addException, "Excepción agregada", "excepción"); resetBool(addExceptionAction); }
        if (removeExceptionAction.get()) { removeFromList(exceptionPlayers, removeException, "Excepción eliminada", "excepción"); resetBool(removeExceptionAction); }
        if (resetStatsAction.get()) { resetStats(); resetBool(resetStatsAction); }
        if (cacheDirty) rebuildCacheIfNeeded();
    }

    private void resetBool(Setting<Boolean> setting) {
        if (resetting || setting == null) return;
        resetting = true;
        try { setting.set(false); } finally { resetting = false; }
    }

    private void invalidateCache() {
        cacheDirty = true;
    }

    public void rebuildCacheIfNeeded() {
        if (!cacheDirty) return;
        cacheDirty = false;
        rebuildSet(mutedPlayers.get(), mutedNamesLower, mutedUuidsLower);
        rebuildSet(exceptionPlayers.get(), exceptionNamesLower, exceptionUuidsLower);
        updateSearchResult();
        updateStats();
    }

    private void rebuildSet(List<String> list, Set<String> names, Set<String> uuids) {
        names.clear();
        uuids.clear();
        if (list == null) return;
        for (String raw : list) {
            String s = normalize(raw);
            if (s.isEmpty()) continue;
            if (looksLikeUuid(s)) {
                UUID u = parseUuid(s);
                if (u != null) uuids.add(u.toString().toLowerCase(Locale.ROOT));
            } else {
                names.add(s.toLowerCase(Locale.ROOT));
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String v = WHITESPACE.matcher(s.trim()).replaceAll(" ");
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() > 1) v = v.substring(1, v.length() - 1).trim();
        return v;
    }

    private static boolean looksLikeUuid(String s) {
        if (s == null) return false;
        String v = s.trim();
        return v.length() >= 32 && v.indexOf('-') >= 0;
    }

    private static UUID parseUuid(String s) {
        if (s == null) return null;
        try { return UUID.fromString(s.trim()); } catch (IllegalArgumentException ignored) { return null; }
    }

    private void addToList(Setting<List<String>> listSetting, Setting<String> inputSetting, String successLabel, String listLabel) {
        String raw = normalize(inputSetting.get());
        if (raw.isEmpty()) { info("Escribe un nombre o UUID."); return; }

        boolean uuidLike = looksLikeUuid(raw) || raw.length() == 32;
        UUID parsedUuid = parseUuid(raw);
        if (uuidLike && parsedUuid == null) {
            info("UUID inválida: " + raw);
            return;
        }
        String normalizedEntry = parsedUuid != null ? parsedUuid.toString() : raw;

        List<String> list = new ArrayList<>(listSetting.get() == null ? Collections.emptyList() : listSetting.get());
        boolean exists = list.stream().anyMatch(e -> isSameEntry(normalize(e), normalizedEntry));
        if (exists) {
            info("Ya existe en " + listLabel + ": " + normalizedEntry);
            inputSetting.set("");
            return;
        }
        list.add(normalizedEntry);
        listSetting.set(list);
        invalidateCache();
        rebuildCacheIfNeeded();
        info(successLabel + ": " + normalizedEntry);
        inputSetting.set("");
    }

    private void removeFromList(Setting<List<String>> listSetting, Setting<String> inputSetting, String successLabel, String listLabel) {
        String raw = normalize(inputSetting.get());
        if (raw.isEmpty()) { info("Escribe un nombre o UUID para eliminar."); return; }

        List<String> list = new ArrayList<>(listSetting.get() == null ? Collections.emptyList() : listSetting.get());
        boolean removed = list.removeIf(e -> isSameEntry(normalize(e), raw));
        if (removed) {
            listSetting.set(list);
            invalidateCache();
            rebuildCacheIfNeeded();
            info(successLabel + ": " + raw);
        } else {
            info("No encontrado en " + listLabel + ": " + raw);
        }
        inputSetting.set("");
    }

    private static boolean isSameEntry(String a, String b) {
        if (a == null || b == null) return false;
        UUID ua = parseUuid(a);
        UUID ub = parseUuid(b);
        if (ua != null && ub != null) return ua.equals(ub);
        return a.equalsIgnoreCase(b);
    }

    private void updateSearchResult() {
        String q = normalize(searchPlayer.get());
        List<String> list = mutedPlayers.get();
        if (q.isEmpty()) {
            searchResult.set(list == null ? "0 jugadores" : list.size() + " jugadores");
            return;
        }
        if (list == null || list.isEmpty()) { searchResult.set("Sin coincidencias"); return; }
        String ql = q.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String e : list) {
            String n = normalize(e);
            if (n.toLowerCase(Locale.ROOT).contains(ql)) matches.add(n);
            if (matches.size() >= 5) break;
        }
        if (matches.isEmpty()) searchResult.set("Sin coincidencias");
        else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matches.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(matches.get(i));
            }
            searchResult.set(sb.toString());
        }
    }

    private void resetStats() {
        hiddenTotal = 0;
        hiddenPrivate = 0;
        hiddenJoinLeave = 0;
        updateStats();
        info("Estadísticas de MuteChat+ reiniciadas.");
    }

    private void updateStats() {
        statTotalHidden.set(String.valueOf(hiddenTotal));
        statPrivateHidden.set(String.valueOf(hiddenPrivate));
        statJoinLeaveHidden.set(String.valueOf(hiddenJoinLeave));
        int count = mutedNamesLower.size() + mutedUuidsLower.size();
        if (cacheDirty) {
            List<String> list = mutedPlayers.get();
            count = list == null ? 0 : (int) list.stream().map(MuteChatPlusModule::normalize).filter(s -> !s.isEmpty()).count();
        }
        statMutedCount.set(String.valueOf(count));
    }

    public boolean shouldMutePlayerChat(UUID uuid, String name) {
        if (!isActive()) return false;
        if (!mutePlayerChat.get() && !friendsOnlyChat.get()) return false;
        rebuildCacheIfNeeded();

        String normalizedName = normalize(name);
        String nameLower = normalizedName.toLowerCase(Locale.ROOT);
        String uuidLower = uuid == null ? null : uuid.toString().toLowerCase(Locale.ROOT);

        // 1. Self never muted
        if (isSelf(uuid, normalizedName)) return false;

        boolean friend = isMeteorFriend(uuid, normalizedName);

        // 2. Friends Only Chat: hide non-friends regardless of exceptions/manual allow
        if (friendsOnlyChat.get() && !friend) return true;

        // 3. Manual mute list (only when player-chat mute filter is enabled)
        if (!mutePlayerChat.get()) return false;
        boolean manuallyMuted = (uuidLower != null && mutedUuidsLower.contains(uuidLower))
            || (!nameLower.isEmpty() && mutedNamesLower.contains(nameLower));
        if (!manuallyMuted) return false;

        // 4. Exceptions/friends can exempt from manual mute (but not from Friends Only above)
        if (uuidLower != null && exceptionUuidsLower.contains(uuidLower)) return false;
        if (!nameLower.isEmpty() && exceptionNamesLower.contains(nameLower)) return false;
        if (ignoreFriends.get() && friend) return false;

        return true;
    }

    public boolean shouldMutePrivateMessage(UUID uuid, String name) {
        if (!isActive() || !mutePrivateMessages.get()) return false;
        return shouldMute(uuid, name);
    }

    public boolean shouldMuteJoinLeave(String name) {
        if (!isActive()) return false;
        if (!mutePlayerJoin.get() && !mutePlayerLeave.get()) return false;
        return shouldMute(null, name);
    }

    public boolean isJoinMuteEnabled() { return isActive() && mutePlayerJoin.get(); }
    public boolean isLeaveMuteEnabled() { return isActive() && mutePlayerLeave.get(); }

    public void recordHiddenPrivate() { hiddenPrivate++; hiddenTotal++; updateStats(); }
    public void recordHiddenJoinLeave() { hiddenJoinLeave++; hiddenTotal++; updateStats(); }
    public void recordHiddenChat() { hiddenTotal++; updateStats(); }

    private boolean isSelf(UUID uuid, String normalizedName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        if (uuid != null && uuid.equals(mc.player.getUUID())) return true;
        String selfName = mc.player.getGameProfile().name();
        return selfName != null && !normalizedName.isEmpty() && selfName.equalsIgnoreCase(normalizedName);
    }

    private boolean shouldMute(UUID uuid, String name) {
        rebuildCacheIfNeeded();

        String normalizedName = normalize(name);
        String nameLower = normalizedName.toLowerCase(Locale.ROOT);
        String uuidLower = uuid == null ? null : uuid.toString().toLowerCase(Locale.ROOT);

        // 1. Self never muted
        if (isSelf(uuid, normalizedName)) return false;

        // 2. Exceptions never muted
        if (uuidLower != null && exceptionUuidsLower.contains(uuidLower)) return false;
        if (!nameLower.isEmpty() && exceptionNamesLower.contains(nameLower)) return false;

        // 3. Meteor friends
        if (ignoreFriends.get() && isMeteorFriend(uuid, normalizedName)) return false;

        // 4. Muted list
        if (uuidLower != null && mutedUuidsLower.contains(uuidLower)) return true;
        if (!nameLower.isEmpty() && mutedNamesLower.contains(nameLower)) return true;
        return false;
    }

    private boolean isMeteorFriend(UUID uuid, String name) {
        try {
            Friends friends = Friends.get();
            if (friends == null) return false;
            if (name != null && !name.isEmpty()) {
                Friend f = friends.get(name);
                if (f != null) return true;
            }
            // Fallback: scan friends for UUID match when available
            if (uuid != null) {
                for (Friend f : friends) {
                    if (f == null) continue;
                    String fn = f.getName();
                    if (fn != null && !fn.isEmpty() && fn.equalsIgnoreCase(name)) return true;
                }
            }
        } catch (RuntimeException ignored) {
            // fail-open
        }
        return false;
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || msg == null) return;
        String safe = msg;
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[MuteChat+] " + safe), false);
        });
    }
}
