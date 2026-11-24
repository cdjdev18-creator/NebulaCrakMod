package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RangosChangeModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/RangosChange");
    public static RangosChangeModule INSTANCE;

    private static final Set<String> SIXB6T_HOSTS = Set.of(
        "6b6t.org", "play.6b6t.org", "anarchy.6b6t.org", "6b6t.com", "play.6b6t.com"
    );
    private static final long COMMAND_COOLDOWN_MS = 3000L;
    private static final long TAB_SCAN_INTERVAL_MS = 5000L;
    private static final long PREFIX_TIMEOUT_MS = 15000L;
    private static final int MAX_HISTORY = 8;
    private static final int MAX_CACHED_PLAYERS = 32;

    private static final Pattern PREFIX_PATTERN = Pattern.compile("\\[([A-Za-z0-9_+\\-]{1,24})\\]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public enum QuickAction {
        NONE("Ninguna"),
        OPEN_PREFIX_SELECTOR("Abrir /prefix"),
        REFRESH_TABLIST("Escanear Tablist"),
        CLEAR_LOCAL_STATE("Limpiar estado local");

        final String label;
        QuickAction(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final SettingGroup sgActions = settings.getDefaultGroup();
    private final SettingGroup sgAuto = settings.createGroup("Automatización");
    private final SettingGroup sgStatus = settings.createGroup("Estado y Roles");
    private final SettingGroup sgCache = settings.createGroup("Cache jugadores cercanos");

    private final Setting<Boolean> openPrefixSelector = sgActions.add(new BoolSetting.Builder()
        .name("abrir-selector-prefix")
        .description("1 clic: envía /prefix oficial en 6b6t para abrir el selector. No se envía automáticamente. Requiere permisos/rango reales del servidor.")
        .defaultValue(false)
        .build());

    private final Setting<QuickAction> quickAction = sgActions.add(new EnumSetting.Builder<QuickAction>()
        .name("accion-rapida")
        .description("Acción rápida con un clic (al seleccionar una acción distinta de Ninguna).")
        .defaultValue(QuickAction.NONE)
        .build());

    private final Setting<Boolean> clearStatus = sgActions.add(new BoolSetting.Builder()
        .name("limpiar-estado")
        .description("Limpia estado local mostrado. No envía comandos.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> scanTablist = sgAuto.add(new BoolSetting.Builder()
        .name("escanear-tablist-automatico")
        .description("Escanea periódicamente la tablist para actualizar tu rango/prefijo observado.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> notifyOnChange = sgAuto.add(new BoolSetting.Builder()
        .name("notificar-cambios")
        .description("Muestra mensaje local cuando cambia estado/rango/prefijo.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> notifySound = sgAuto.add(new BoolSetting.Builder()
        .name("sonido-notificacion")
        .description("Reproduce un sonido local cuando hay confirmación/rechazo/permiso denegado.")
        .defaultValue(false)
        .visible(notifyOnChange::get)
        .build());

    private final Setting<String> connectionStatus = sgStatus.add(new StringSetting.Builder()
        .name("conexion").description("Estado de conexión.").defaultValue("Desconectado").build());
    private final Setting<String> detectedRank = sgStatus.add(new StringSetting.Builder()
        .name("rango-detectado").description("Rango detectado solo con evidencia fiable.").defaultValue("No se pudo verificar").build());
    private final Setting<String> currentPrefix = sgStatus.add(new StringSetting.Builder()
        .name("prefijo-actual").description("Prefijo actual observado/confirmado por servidor.").defaultValue("No se pudo verificar").build());
    private final Setting<String> assignedRole = sgStatus.add(new StringSetting.Builder()
        .name("rol-asignado").description("Rol interno asignado según evidencia visible (no permisos reales).").defaultValue("Ninguno").build());
    private final Setting<String> lastCommand = sgStatus.add(new StringSetting.Builder()
        .name("ultimo-comando").description("Último comando relacionado con rangos.").defaultValue("Ninguno").build());
    private final Setting<String> lastResponse = sgStatus.add(new StringSetting.Builder()
        .name("ultima-respuesta").description("Última respuesta del servidor relacionada con rangos/prefix.").defaultValue("Sin respuesta").build());
    private final Setting<String> verificationState = sgStatus.add(new StringSetting.Builder()
        .name("estado-verificacion").description("Seleccionando... / Comando enviado. / Cambio confirmado. / Operación rechazada. / No tienes permisos. / Opción no disponible. / No se pudo verificar. / Detectado.").defaultValue("No se pudo verificar").build());
    private final Setting<String> availableOptions = sgStatus.add(new StringSetting.Builder()
        .name("opciones-disponibles").description("Opciones observadas en respuestas reales del servidor. No se inventan.").defaultValue("Sin datos del servidor").build());

    private final Setting<String> history1 = sgStatus.add(new StringSetting.Builder().name("historial-1").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history2 = sgStatus.add(new StringSetting.Builder().name("historial-2").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history3 = sgStatus.add(new StringSetting.Builder().name("historial-3").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history4 = sgStatus.add(new StringSetting.Builder().name("historial-4").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history5 = sgStatus.add(new StringSetting.Builder().name("historial-5").description("Evento reciente.").defaultValue("-").build());

    private final Setting<String> cachedPlayers = sgCache.add(new StringSetting.Builder()
        .name("roles-cercanos-cache")
        .description("Cache local de roles/prefijos observados en tablist (máx 32). No es verificación de permisos.")
        .defaultValue("Sin datos")
        .build());

    private final Deque<String> history = new ArrayDeque<>();
    private final Map<String, String> nearbyRoleCache = new ConcurrentHashMap<>();

    private boolean resetting;
    private long lastCommandMs = 0L;
    private long lastPrefixRequestMs = 0L;
    private long lastTabScanMs = 0L;
    private boolean awaitingPrefixResponse = false;
    private String lastServerHost = "";
    private String lastNotifiedState = "";

    public RangosChangeModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "RangosChange", "Gestiona /prefix oficial en 6b6t con detección en tiempo real, estados claros y cache local de roles observados.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        updateConnectionStatus();
        addHistory("Módulo activado");
        notifyState("RangosChange activado");
    }

    @Override
    public void onDeactivate() {
        awaitingPrefixResponse = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;

        updateConnectionStatus();

        if (clearStatus.get()) {
            resetBool(clearStatus);
            clearLocalState("Estado local limpiado");
        }

        if (openPrefixSelector.get()) {
            resetBool(openPrefixSelector);
            requestPrefixSelector();
        }

        QuickAction action = quickAction.get();
        if (action != null && action != QuickAction.NONE) {
            quickAction.set(QuickAction.NONE);
            handleQuickAction(action);
        }

        long now = System.currentTimeMillis();
        if (scanTablist.get() && now - lastTabScanMs > TAB_SCAN_INTERVAL_MS) {
            lastTabScanMs = now;
            inspectPlayerTablist();
        }

        if (awaitingPrefixResponse && now - lastPrefixRequestMs > PREFIX_TIMEOUT_MS) {
            awaitingPrefixResponse = false;
            verificationState.set("No se pudo verificar");
            lastResponse.set("Sin respuesta clara del servidor a /prefix (15s). Verifica manualmente.");
            addHistory("Timeout /prefix");
            notifyState("Timeout esperando respuesta /prefix");
        }
    }

    private void handleQuickAction(QuickAction action) {
        if (action == null) return;
        switch (action) {
            case OPEN_PREFIX_SELECTOR -> requestPrefixSelector();
            case REFRESH_TABLIST -> {
                inspectPlayerTablist();
                addHistory("Tablist escaneada manualmente");
                notifyState("Tablist escaneada");
            }
            case CLEAR_LOCAL_STATE -> clearLocalState("Estado local limpiado");
            case NONE -> { }
        }
    }

    public void onCommandWritten(String raw) {
        if (!isActive()) return;
        String cmd = normalizeCommand(raw);
        if (!isRelevantCommand(cmd)) return;
        lastCommand.set("Escrito: " + truncate(cmd));
        addHistory("Escrito: " + truncate(cmd));
    }

    public void onCommandSent(String raw) {
        if (!isActive()) return;
        String cmd = normalizeCommand(raw);
        if (!isRelevantCommand(cmd)) return;
        lastCommand.set("Enviado: " + truncate(cmd));
        verificationState.set("Comando enviado.");
        addHistory("Enviado: " + truncate(cmd));
        if (cmd.toLowerCase(Locale.ROOT).startsWith("/prefix")) {
            awaitingPrefixResponse = true;
            lastPrefixRequestMs = System.currentTimeMillis();
        }
    }

    public void handleServerMessage(String rawMessage) {
        if (!isActive() || rawMessage == null) return;
        String msg = rawMessage.trim();
        if (msg.isEmpty()) return;
        String lower = msg.toLowerCase(Locale.ROOT);
        detectRankFromMessage(msg, lower);
        detectPrefixResponse(msg, lower);
    }

    private void requestPrefixSelector() {
        Minecraft mc = Minecraft.getInstance();
        String host = getCurrentServerHost();
        logDiag("server=" + host + ", rank=" + detectedRank.get() + ", op=abrir-selector-prefix");

        if (mc == null || mc.getConnection() == null || mc.player == null) {
            verificationState.set("No se pudo verificar");
            lastResponse.set("No conectado: no se puede enviar /prefix.");
            addHistory("Error: no conectado");
            logDiag("result=error reason=not-connected");
            info("No conectado.");
            return;
        }
        if (!is6b6tHost(host)) {
            verificationState.set("Opción no disponible.");
            lastResponse.set("Fuera de 6b6t: /prefix específico no se envía automáticamente. Host: " + host);
            addHistory("Bloqueado: fuera de 6b6t");
            logDiag("result=blocked reason=not-6b6t host=" + host);
            info("Servidor no compatible con /prefix de 6b6t.");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCommandMs < COMMAND_COOLDOWN_MS) {
            verificationState.set("Seleccionando...");
            lastResponse.set("Espera anti-spam antes de enviar otro comando.");
            addHistory("Anti-spam /prefix");
            logDiag("result=blocked reason=cooldown");
            return;
        }

        try {
            verificationState.set("Seleccionando...");
            lastCommandMs = now;
            lastPrefixRequestMs = now;
            awaitingPrefixResponse = true;
            lastCommand.set("Enviado: /prefix");
            addHistory("Click: abrir /prefix");
            logDiag("command=/prefix sent=true");
            mc.getConnection().sendCommand("prefix");
            verificationState.set("Comando enviado.");
            lastResponse.set("Solicitud enviada: /prefix. Esperando respuesta real del servidor.");
            info("Comando enviado: /prefix");
            notifyState("Comando /prefix enviado");
        } catch (Throwable t) {
            awaitingPrefixResponse = false;
            verificationState.set("Operación rechazada.");
            lastResponse.set("Error al enviar /prefix: " + t.getClass().getSimpleName());
            addHistory("Error enviando /prefix");
            logDiag("result=rejected reason=send-exception type=" + t.getClass().getSimpleName());
            LOG.warn("[RangosChange] Failed to send /prefix", t);
            notifyState("Error enviando /prefix");
        }
    }

    private void inspectPlayerTablist() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null || mc.player == null) return;

        String selfName = mc.player.getGameProfile().name();
        if (selfName == null || selfName.isBlank()) return;

        Map<String, String> snapshot = new LinkedHashMap<>();
        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            if (info == null || info.getProfile() == null) continue;
            String name = info.getProfile().name();
            if (name == null || name.isBlank()) continue;
            Component displayName = info.getTabListDisplayName();
            String text = displayName != null ? displayName.getString() : name;
            String prefix = extractPrefixToken(text);
            String rank = rankFromText(text);
            if (!prefix.isBlank() || !rank.isBlank()) {
                snapshot.put(name, (prefix.isBlank() ? "-" : prefix) + (rank.isBlank() ? "" : " " + rank));
            }
            if (name.equalsIgnoreCase(selfName)) {
                parseAndAssignRoleFromText(text);
            }
        }

        if (!snapshot.isEmpty()) {
            nearbyRoleCache.clear();
            int count = 0;
            for (Map.Entry<String, String> e : snapshot.entrySet()) {
                nearbyRoleCache.put(e.getKey(), e.getValue());
                count++;
                if (count >= MAX_CACHED_PLAYERS) break;
            }
            cachedPlayers.set(formatNearbyCache());
        }
    }

    private String formatNearbyCache() {
        if (nearbyRoleCache.isEmpty()) return "Sin datos";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, String> e : nearbyRoleCache.entrySet()) {
            if (i > 0) sb.append(" | ");
            sb.append(truncate(e.getKey() + ": " + e.getValue(), 40));
            i++;
            if (i >= 6) break; // keep setting readable
        }
        if (nearbyRoleCache.size() > 6) sb.append(" (+").append(nearbyRoleCache.size() - 6).append(")");
        return sb.toString();
    }

    private void parseAndAssignRoleFromText(String text) {
        if (text == null || text.isBlank()) return;
        String prefix = extractPrefixToken(text);
        if (!prefix.isBlank()) {
            currentPrefix.set(prefix + " (Tablist)");
        }
        String rank = rankFromText(text);
        if (!rank.isBlank()) {
            detectedRank.set(rank + " (Tablist)");
            assignedRole.set(mapRole(rank));
        }
    }

    private void detectRankFromMessage(String msg, String lower) {
        Minecraft mc = Minecraft.getInstance();
        String self = mc != null && mc.player != null ? mc.player.getGameProfile().name() : "";
        if (self == null || self.isBlank()) return;
        boolean mentionsSelf = lower.contains(self.toLowerCase(Locale.ROOT));
        if (!mentionsSelf) return;

        String rank = rankFromText(msg);
        if (!rank.isBlank()) {
            detectedRank.set(rank + " (evidencia en mensaje)");
            assignedRole.set(mapRole(rank));
            addHistory("Rango detectado: " + rank);
            notifyState("Rango detectado: " + rank);
        }
    }

    private void detectPrefixResponse(String msg, String lower) {
        boolean relevant = awaitingPrefixResponse || lower.contains("prefix") || lower.contains("prefijo") || lower.contains("rango") || lower.contains("rank");
        if (!relevant) return;

        if (containsAny(lower, "no permission", "don't have permission", "not allowed", "you need", "must be", "required", "no tienes permiso", "no puedes", "sin permiso")) {
            awaitingPrefixResponse = false;
            verificationState.set("No tienes permisos.");
            lastResponse.set(truncate(msg));
            assignedRole.set("Sin permisos para prefijos");
            addHistory("Rechazado: permisos");
            logDiag("response=permission-denied raw=\"" + sanitizeForLog(msg) + "\"");
            notifyState("Permiso denegado por servidor");
            return;
        }

        if (lower.contains("prefix") || lower.contains("prefijo")) {
            lastResponse.set(truncate(msg));
            addHistory("Respuesta: " + truncate(msg));
            logDiag("response=prefix raw=\"" + sanitizeForLog(msg) + "\"");
            if (awaitingPrefixResponse) awaitingPrefixResponse = false;

            if (containsAny(lower, "selected", "set", "changed", "updated", "seleccion", "cambiado", "actualizado", "establecido")) {
                verificationState.set("Cambio confirmado.");
                String observed = extractPrefixToken(msg);
                if (!observed.isBlank()) {
                    currentPrefix.set(observed + " (confirmado por servidor)");
                    assignedRole.set("Prefijo activo: " + observed);
                }
                addHistory("Cambio confirmado por servidor");
                logDiag("result=confirmed prefix=" + observed);
                notifyState("Prefijo confirmado por servidor");
            } else if (containsAny(lower, "available", "options", "select", "choose", "opciones", "elige", "selecciona", "[")) {
                verificationState.set("Detectado");
                availableOptions.set(truncate(msg));
                String observed = extractPrefixToken(msg);
                if (!observed.isBlank()) currentPrefix.set(observed + " (observado)");
                logDiag("result=options-observed");
            } else if (containsAny(lower, "denied", "rejected", "can't", "cannot", "invalid", "rechaz", "inválid", "invalido")) {
                verificationState.set("Operación rechazada.");
                logDiag("result=rejected");
                notifyState("Operación rechazada por servidor");
            } else {
                verificationState.set("Detectado");
                logDiag("result=detected");
            }
        }

        if (selfMentioned(msg)) {
            String observed = extractPrefixToken(msg);
            if (!observed.isBlank() && (lower.contains("prefix") || lower.contains("[") || awaitingPrefixResponse)) {
                currentPrefix.set(observed + " (observado)");
            }
        }
    }

    private boolean selfMentioned(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || msg == null) return false;
        String self = mc.player.getGameProfile().name();
        return self != null && !self.isBlank() && msg.toLowerCase(Locale.ROOT).contains(self.toLowerCase(Locale.ROOT));
    }

    private String rankFromText(String text) {
        if (text == null || text.isBlank()) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsRankToken(lower, "apex")) return "Apex";
        if (containsRankToken(lower, "legend")) return "Legend";
        if (containsRankToken(lower, "elite")) return "Elite";
        if (containsRankToken(lower, "prime")) return "Prime";
        return "";
    }

    private String mapRole(String rank) {
        if (rank == null) return "Ninguno";
        return switch (rank.toLowerCase(Locale.ROOT)) {
            case "apex" -> "Rango VIP: Apex (evidencia visible)";
            case "legend" -> "Rango VIP: Legend (evidencia visible)";
            case "elite" -> "Rango VIP: Elite (evidencia visible)";
            case "prime" -> "Rango VIP: Prime (evidencia visible)";
            default -> "Rango estándar / no verificado";
        };
    }

    private boolean containsRankToken(String text, String token) {
        if (text == null || token == null) return false;
        String t = token.toLowerCase(Locale.ROOT);
        String s = text.toLowerCase(Locale.ROOT);
        return s.contains("[" + t + "]") || s.contains(" " + t + " ") || s.startsWith(t + " ") || s.endsWith(" " + t) || s.contains(t + " rank") || s.contains("rank " + t);
    }

    private String extractPrefixToken(String msg) {
        if (msg == null || msg.isBlank()) return "";
        Matcher matcher = PREFIX_PATTERN.matcher(msg);
        while (matcher.find()) {
            String inner = matcher.group(1);
            if (inner == null) continue;
            String lower = inner.toLowerCase(Locale.ROOT);
            if (lower.contains("apex") || lower.contains("elite") || lower.contains("legend") || lower.contains("prime") || lower.contains("default") || lower.contains("owner")) {
                return "[" + inner + "]";
            }
        }
        return "";
    }

    private void updateConnectionStatus() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getCurrentServer() == null) {
            if (!lastServerHost.isEmpty()) {
                lastServerHost = "";
                clearTransientOnDisconnect();
            }
            connectionStatus.set("Desconectado");
            return;
        }

        String host = getCurrentServerHost();
        if (!Objects.equals(host, lastServerHost)) {
            lastServerHost = host;
            clearTransientOnDisconnect();
            addHistory("Conectado: " + host);
            notifyState("Conectado a " + host);
        }

        if (is6b6tHost(host)) connectionStatus.set("Conectado a 6b6t (" + host + ")");
        else connectionStatus.set("Conectado a otro servidor (" + host + ") - /prefix 6b6t no automático");
    }

    private void clearTransientOnDisconnect() {
        awaitingPrefixResponse = false;
        verificationState.set("No se pudo verificar");
        lastResponse.set("Sin respuesta");
        availableOptions.set("Sin datos del servidor");
        currentPrefix.set("No se pudo verificar");
        detectedRank.set("No se pudo verificar");
        assignedRole.set("Ninguno");
        lastCommand.set("Ninguno");
        nearbyRoleCache.clear();
        cachedPlayers.set("Sin datos");
        history.clear();
        refreshHistorySettings();
    }

    private void clearLocalState(String reason) {
        awaitingPrefixResponse = false;
        detectedRank.set("No se pudo verificar");
        currentPrefix.set("No se pudo verificar");
        assignedRole.set("Ninguno");
        lastCommand.set("Ninguno");
        lastResponse.set(reason == null ? "Estado local limpiado" : reason);
        verificationState.set("No se pudo verificar");
        availableOptions.set("Sin datos del servidor");
        nearbyRoleCache.clear();
        cachedPlayers.set("Sin datos");
        history.clear();
        addHistory(reason == null ? "Estado local limpiado" : reason);
        notifyState("Estado local limpiado");
    }

    private void addHistory(String entry) {
        String e = truncate(entry);
        if (e.isBlank()) return;
        history.addFirst(e);
        while (history.size() > MAX_HISTORY) history.removeLast();
        refreshHistorySettings();
    }

    private void refreshHistorySettings() {
        List<String> list = new ArrayList<>(history);
        history1.set(getHist(list, 0));
        history2.set(getHist(list, 1));
        history3.set(getHist(list, 2));
        history4.set(getHist(list, 3));
        history5.set(getHist(list, 4));
    }

    private static String getHist(List<String> list, int idx) {
        if (list == null || idx < 0 || idx >= list.size()) return "-";
        String v = list.get(idx);
        return v == null || v.isBlank() ? "-" : v;
    }

    private boolean isRelevantCommand(String cmd) {
        if (cmd == null) return false;
        String c = cmd.toLowerCase(Locale.ROOT).trim();
        return c.startsWith("/prefix") || c.startsWith("/skin") || c.startsWith("/rank") || c.startsWith("/ranks")
            || c.startsWith("/chatcolor") || c.startsWith("/namecolor") || c.contains("prefix");
    }

    private static String normalizeCommand(String raw) {
        if (raw == null) return "";
        String c = raw.trim();
        if (c.isEmpty()) return "";
        return c.startsWith("/") ? c : "/" + c;
    }

    private static boolean containsAny(String lower, String... needles) {
        if (lower == null || needles == null) return false;
        for (String n : needles) {
            if (n != null && lower.contains(n.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String getCurrentServerHost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getCurrentServer() == null || mc.getCurrentServer().ip == null) return "";
        return mc.getCurrentServer().ip.toLowerCase(Locale.ROOT).trim();
    }

    private boolean is6b6tHost(String host) {
        if (host == null || host.isBlank()) return false;
        String h = host.toLowerCase(Locale.ROOT).trim();
        int colon = h.indexOf(':');
        if (colon >= 0) h = h.substring(0, colon);
        if (SIXB6T_HOSTS.contains(h)) return true;
        for (String d : SIXB6T_HOSTS) if (h.endsWith("." + d)) return true;
        return h.contains("6b6t");
    }

    private static String truncate(String s) {
        return truncate(s, 140);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String v = WHITESPACE_PATTERN.matcher(s.trim()).replaceAll(" ");
        if (max < 4) max = 4;
        return v.length() > max ? v.substring(0, max - 3) + "..." : v;
    }

    private static String sanitizeForLog(String s) {
        return truncate(s, 120).replace("\"", "'");
    }

    private void resetBool(Setting<Boolean> setting) {
        if (setting == null || resetting) return;
        resetting = true;
        try {
            setting.set(false);
        } finally {
            resetting = false;
        }
    }

    private void notifyState(String msg) {
        if (!notifyOnChange.get() || msg == null || msg.isBlank()) return;
        if (msg.equals(lastNotifiedState)) return;
        lastNotifiedState = msg;
        info(msg);
        if (notifySound.get()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7f, 1.2f);
                    }
                });
            }
        }
    }

    private void logDiag(String msg) {
        if (msg == null) return;
        LOG.info("[RangosChange][diag] {}", msg);
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || msg == null) return;
        String safe = truncate(msg, 180);
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[RangosChange] " + safe), false);
            LOG.info("[RangosChange] {}", safe);
        });
    }
}
