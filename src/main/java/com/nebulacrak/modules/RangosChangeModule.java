package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RangosChangeModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/RangosChange");
    public static RangosChangeModule INSTANCE;

    private static final Set<String> SIXB6T_HOSTS = Set.of(
        "6b6t.org", "play.6b6t.org", "anarchy.6b6t.org", "6b6t.com", "play.6b6t.com"
    );
    private static final long COMMAND_COOLDOWN_MS = 3000L;
    private static final int MAX_HISTORY = 8;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStatus = settings.createGroup("Estado");

    private final Setting<String> connectionStatus = sgStatus.add(new StringSetting.Builder()
        .name("conexion").description("Estado de conexión.").defaultValue("Desconectado").build());
    private final Setting<String> detectedRank = sgStatus.add(new StringSetting.Builder()
        .name("rango-detectado").description("Rango detectado solo con evidencia fiable.").defaultValue("No se pudo verificar").build());
    private final Setting<String> currentPrefix = sgStatus.add(new StringSetting.Builder()
        .name("prefijo-actual").description("Prefijo actual observado/confirmado por servidor.").defaultValue("No se pudo verificar").build());
    private final Setting<String> lastCommand = sgStatus.add(new StringSetting.Builder()
        .name("ultimo-comando").description("Último comando relacionado con rangos detectado.").defaultValue("Ninguno").build());
    private final Setting<String> lastResponse = sgStatus.add(new StringSetting.Builder()
        .name("ultima-respuesta").description("Última respuesta del servidor relacionada con rangos/prefix.").defaultValue("Sin respuesta").build());
    private final Setting<String> verificationState = sgStatus.add(new StringSetting.Builder()
        .name("estado-verificacion").description("Detectado / Pendiente de confirmación / Confirmado por el servidor / Rechazado por el servidor / No se pudo verificar.").defaultValue("No se pudo verificar").build());
    private final Setting<String> availableOptions = sgStatus.add(new StringSetting.Builder()
        .name("opciones-disponibles").description("Opciones observadas en respuestas reales del servidor. No se inventan.").defaultValue("Sin datos del servidor").build());
    private final Setting<String> history1 = sgStatus.add(new StringSetting.Builder().name("historial-1").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history2 = sgStatus.add(new StringSetting.Builder().name("historial-2").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history3 = sgStatus.add(new StringSetting.Builder().name("historial-3").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history4 = sgStatus.add(new StringSetting.Builder().name("historial-4").description("Evento reciente.").defaultValue("-").build());
    private final Setting<String> history5 = sgStatus.add(new StringSetting.Builder().name("historial-5").description("Evento reciente.").defaultValue("-").build());

    private final Setting<Boolean> openPrefixSelector = sgGeneral.add(new BoolSetting.Builder()
        .name("abrir-selector-prefix").description("Envía /prefix manualmente en 6b6t. Respeta permisos reales del servidor.").defaultValue(false).build());
    private final Setting<Boolean> clearStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("limpiar-estado").description("Limpia estado local (no envía comandos).").defaultValue(false).build());

    private final Deque<String> history = new ArrayDeque<>();
    private boolean resetting;
    private long lastCommandMs = 0L;
    private long lastPrefixRequestMs = 0L;
    private boolean awaitingPrefixResponse = false;
    private String lastServerHost = "";

    public RangosChangeModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "RangosChange", "Observa rangos/prefix y usa /prefix oficial en 6b6t según permisos reales del servidor.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        updateConnectionStatus();
        addHistory("Módulo activado");
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
        if (awaitingPrefixResponse && System.currentTimeMillis() - lastPrefixRequestMs > 15000L) {
            awaitingPrefixResponse = false;
            verificationState.set("Pendiente de confirmación");
            lastResponse.set("Sin respuesta clara del servidor a /prefix (15s).");
            addHistory("Timeout /prefix sin respuesta clara");
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
        verificationState.set("Pendiente de confirmación");
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
        if (mc == null || mc.getConnection() == null || mc.player == null) {
            lastResponse.set("No conectado: no se puede enviar /prefix.");
            verificationState.set("No se pudo verificar");
            addHistory("Error: no conectado");
            return;
        }
        String host = getCurrentServerHost();
        if (!is6b6tHost(host)) {
            lastResponse.set("Servidor no compatible con /prefix de 6b6t: " + host);
            verificationState.set("No se pudo verificar");
            addHistory("Bloqueado: fuera de 6b6t");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCommandMs < COMMAND_COOLDOWN_MS) {
            lastResponse.set("Espera anti-spam antes de enviar otro comando.");
            addHistory("Anti-spam /prefix");
            return;
        }
        try {
            lastCommandMs = now;
            lastPrefixRequestMs = now;
            awaitingPrefixResponse = true;
            verificationState.set("Pendiente de confirmación");
            lastCommand.set("Enviado: /prefix");
            addHistory("Enviado: /prefix");
            mc.getConnection().sendCommand("prefix");
            lastResponse.set("Solicitud enviada: /prefix. Esperando respuesta real del servidor.");
        } catch (Throwable t) {
            awaitingPrefixResponse = false;
            verificationState.set("Rechazado por el servidor");
            lastResponse.set("Error al enviar /prefix: " + t.getClass().getSimpleName());
            LOG.warn("[RangosChange] Failed to send /prefix", t);
        }
    }

    private void detectRankFromMessage(String msg, String lower) {
        Minecraft mc = Minecraft.getInstance();
        String self = mc != null && mc.player != null ? mc.player.getGameProfile().name().toLowerCase(Locale.ROOT) : "";
        boolean mentionsSelf = !self.isEmpty() && lower.contains(self);
        if (mentionsSelf) {
            String rank = null;
            if (containsRankToken(lower, "apex") || msg.contains("[APEX]") || msg.contains("[Apex]")) rank = "Apex";
            else if (containsRankToken(lower, "elite")) rank = "Elite";
            else if (containsRankToken(lower, "legend")) rank = "Legend";
            else if (containsRankToken(lower, "prime")) rank = "Prime";
            if (rank != null) {
                detectedRank.set(rank + " (evidencia en mensaje)");
                verificationState.set("Detectado");
                addHistory("Rango detectado: " + rank);
            }
        }
    }

    private void detectPrefixResponse(String msg, String lower) {
        boolean relevant = awaitingPrefixResponse || lower.contains("prefix") || lower.contains("rango") || lower.contains("rank");
        if (!relevant) return;

        if (lower.contains("prefix") || lower.contains("prefijo")) {
            lastResponse.set(truncate(msg));
            addHistory("Respuesta: " + truncate(msg));
            if (awaitingPrefixResponse) awaitingPrefixResponse = false;

            if (containsAny(lower, "no permission", "not allowed", "you need", "must be", "required", "no tienes permiso", "solo", "only")) {
                verificationState.set("Rechazado por el servidor");
            } else if (containsAny(lower, "selected", "set", "changed", "updated", "seleccion", "cambiado", "actualizado", "establecido")) {
                verificationState.set("Confirmado por el servidor");
                String observed = extractPrefixToken(msg);
                if (!observed.isBlank()) currentPrefix.set(observed);
            } else if (containsAny(lower, "available", "options", "select", "choose", "opciones", "elige", "selecciona")) {
                verificationState.set("Detectado");
                availableOptions.set(truncate(msg));
            } else if (awaitingPrefixResponse) {
                verificationState.set("Detectado");
            }
        }

        // TAB/chat prefix evidence near self can update current prefix conservatively
        Minecraft mc = Minecraft.getInstance();
        String self = mc != null && mc.player != null ? mc.player.getGameProfile().name() : "";
        if (!self.isEmpty() && msg.contains(self)) {
            String observed = extractPrefixToken(msg);
            if (!observed.isBlank() && (lower.contains("prefix") || lower.contains("[") || awaitingPrefixResponse)) {
                currentPrefix.set(observed);
            }
        }
    }

    private String extractPrefixToken(String msg) {
        int l = msg.indexOf('[');
        int r = msg.indexOf(']', l + 1);
        if (l >= 0 && r > l + 1 && r - l <= 24) {
            String token = msg.substring(l, r + 1).trim();
            String inner = token.substring(1, token.length() - 1).toLowerCase(Locale.ROOT);
            if (inner.contains("apex") || inner.contains("elite") || inner.contains("legend") || inner.contains("prime") || inner.contains("owner") || inner.contains("default")) {
                return token;
            }
        }
        return "";
    }

    private void updateConnectionStatus() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getCurrentServer() == null) {
            String host = "";
            if (!host.equals(lastServerHost)) {
                lastServerHost = host;
                clearTransientOnDisconnect();
            }
            connectionStatus.set("Desconectado");
            return;
        }
        String host = getCurrentServerHost();
        if (!host.equals(lastServerHost)) {
            lastServerHost = host;
            clearTransientOnDisconnect();
            addHistory("Conectado: " + host);
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
        lastCommand.set("Ninguno");
        history.clear();
        refreshHistorySettings();
    }

    private void clearLocalState(String reason) {
        awaitingPrefixResponse = false;
        detectedRank.set("No se pudo verificar");
        currentPrefix.set("No se pudo verificar");
        lastCommand.set("Ninguno");
        lastResponse.set(reason);
        verificationState.set("No se pudo verificar");
        availableOptions.set("Sin datos del servidor");
        history.clear();
        addHistory(reason);
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

    private static String getHist(List<String> list, int idx) { return idx < list.size() ? list.get(idx) : "-"; }

    private boolean isRelevantCommand(String cmd) {
        if (cmd == null) return false;
        String c = cmd.toLowerCase(Locale.ROOT).trim();
        return c.startsWith("/prefix") || c.startsWith("/skin") || c.startsWith("/rank") || c.startsWith("/ranks")
            || c.startsWith("/chatcolor") || c.startsWith("/namecolor") || c.contains("prefix");
    }

    private static String normalizeCommand(String raw) {
        if (raw == null) return "";
        String c = raw.trim();
        if (c.startsWith("/")) return c;
        return "/" + c;
    }

    private boolean containsRankToken(String text, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        String s = text.toLowerCase(Locale.ROOT);
        return s.contains("[" + t + "]") || s.contains(" " + t + " ") || s.startsWith(t + " ") || s.endsWith(" " + t) || s.contains(t + " rank") || s.contains("rank " + t);
    }

    private static boolean containsAny(String lower, String... needles) {
        for (String n : needles) if (lower.contains(n.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private String getCurrentServerHost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getCurrentServer() == null) return "";
        String ip = mc.getCurrentServer().ip;
        return ip == null ? "" : ip.toLowerCase(Locale.ROOT).trim();
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
        if (s == null) return "";
        String v = s.replaceAll("\\s+", " ").trim();
        return v.length() > 140 ? v.substring(0, 137) + "..." : v;
    }

    private void resetBool(Setting<Boolean> setting) {
        if (resetting) return;
        resetting = true;
        try { setting.set(false); } finally { resetting = false; }
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[RangosChange] " + msg), false);
            LOG.info("[RangosChange] {}", msg);
        });
    }
}
