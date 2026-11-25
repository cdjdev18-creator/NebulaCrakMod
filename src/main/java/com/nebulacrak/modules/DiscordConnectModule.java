package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordConnectModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/DiscordConnect");
    public static DiscordConnectModule INSTANCE;

    private static final int MAX_MESSAGES = 8;
    private static final long SEND_COOLDOWN_MS = 5000L;
    private static final long CHECK_COOLDOWN_MS = 8000L;
    private static final long RECONNECT_BASE_MS = 15000L;

    private final SettingGroup sgConfig = settings.getDefaultGroup();
    private final SettingGroup sgStatus = settings.createGroup("Estado");

    private final Setting<String> bridgeUrl = sgConfig.add(new StringSetting.Builder()
        .name("bridge-api-url")
        .description("URL base de API intermediaria autorizada (Minecraft <-> Bridge <-> Bot Discord). No introducir tokens de bot en el mod.")
        .defaultValue("")
        .build());
    private final Setting<String> inviteUrl = sgConfig.add(new StringSetting.Builder()
        .name("invite-url")
        .description("Enlace OAuth2 para invitar el bot al servidor Discord. Invitar no autentica ni conecta el chat por sí solo.")
        .defaultValue("")
        .build());
    private final Setting<String> botName = sgConfig.add(new StringSetting.Builder()
        .name("bot-name").description("Nombre del bot (informativo).").defaultValue("").build());
    private final Setting<String> guildChannel = sgConfig.add(new StringSetting.Builder()
        .name("servidor-canal").description("Servidor/canal autorizado (informativo).").defaultValue("").build());
    private final Setting<String> messageToSend = sgConfig.add(new StringSetting.Builder()
        .name("mensaje-a-enviar").description("Mensaje a enviar por bridge autorizado. No se envía automáticamente.").defaultValue("").build());

    private final Setting<Boolean> openInvite = sgConfig.add(new BoolSetting.Builder()
        .name("abrir-invitacion").description("Abre el enlace de invitación OAuth2 en el navegador.").defaultValue(false).build());
    private final Setting<Boolean> checkConnection = sgConfig.add(new BoolSetting.Builder()
        .name("comprobar-conexion").description("Comprueba estado del bridge/API (GET /status o /health).").defaultValue(false).build());
    private final Setting<Boolean> sendMessage = sgConfig.add(new BoolSetting.Builder()
        .name("enviar-mensaje").description("1 clic: envía mensaje al bridge autorizado (POST /send).").defaultValue(false).build());
    private final Setting<Boolean> clearLocal = sgConfig.add(new BoolSetting.Builder()
        .name("limpiar-local").description("Limpia estado local.").defaultValue(false).build());

    private final Setting<String> connectionStatus = sgStatus.add(new StringSetting.Builder()
        .name("estado-conexion").description("Estado real de integración.").defaultValue("No configurado").build());
    private final Setting<String> lastResult = sgStatus.add(new StringSetting.Builder()
        .name("ultimo-resultado").description("Último resultado.").defaultValue("Pendiente: requiere bridge/API compatible").build());
    private final Setting<String> msg1 = sgStatus.add(new StringSetting.Builder().name("msg-1").defaultValue("-").build());
    private final Setting<String> msg2 = sgStatus.add(new StringSetting.Builder().name("msg-2").defaultValue("-").build());
    private final Setting<String> msg3 = sgStatus.add(new StringSetting.Builder().name("msg-3").defaultValue("-").build());
    private final Setting<String> msg4 = sgStatus.add(new StringSetting.Builder().name("msg-4").defaultValue("-").build());
    private final Setting<String> msg5 = sgStatus.add(new StringSetting.Builder().name("msg-5").defaultValue("-").build());

    private final Deque<String> messages = new ArrayDeque<>();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile long lastSendMs = 0L;
    private volatile long lastCheckMs = 0L;
    private volatile long nextReconnectMs = 0L;
    private boolean resetting;

    public DiscordConnectModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "DiscordConnect", "Integración Discord autorizada vía API intermediaria. No usa self-bots/tokens en el mod.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        connectionStatus.set(isBridgeConfigured() ? "Configurado (sin verificación real)" : "No configurado - requiere bridge/API");
        addMsg("Módulo activado. Conexión real pendiente de backend autorizado.");
    }

    @Override
    public void onDeactivate() {
        connectionStatus.set("Desactivado");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;
        if (clearLocal.get()) { resetBool(clearLocal); clearLocalState(); }
        if (openInvite.get()) { resetBool(openInvite); openInviteLink(); }
        if (checkConnection.get()) { resetBool(checkConnection); requestCheck(); }
        if (sendMessage.get()) { resetBool(sendMessage); requestSend(); }
    }

    private void openInviteLink() {
        String url = inviteUrl.get() == null ? "" : inviteUrl.get().trim();
        if (url.isBlank()) { fail("Sin enlace de invitación"); return; }
        if (!url.startsWith("https://discord.com/oauth2/") && !url.startsWith("https://discord.com/api/oauth2/")) {
            fail("Enlace de invitación inválido (debe ser OAuth2 de Discord)");
            return;
        }
        try {
            Util.getPlatform().openUri(url);
            lastResult.set("Invitación abierta en navegador. Invitar no conecta el chat.");
            addMsg("Invitación OAuth2 abierta");
        } catch (Throwable t) {
            fail("No se pudo abrir invitación: " + t.getClass().getSimpleName());
        }
    }

    private void requestCheck() {
        String base = normalizeBase(bridgeUrl.get());
        if (base.isBlank()) { fail("Bridge/API no configurada"); connectionStatus.set("No configurado - requiere bridge/API"); return; }
        long now = System.currentTimeMillis();
        if (now - lastCheckMs < CHECK_COOLDOWN_MS) { fail("Cooldown de comprobación activo"); return; }
        if (now < nextReconnectMs) { fail("Reconexión en espera tras fallo"); return; }
        if (!busy.compareAndSet(false, true)) { fail("Operación en curso"); return; }
        lastCheckMs = now;
        connectionStatus.set("Comprobando...");
        runAsync(() -> doRequest("GET", base + "/status", null, true));
    }

    private void requestSend() {
        String base = normalizeBase(bridgeUrl.get());
        String msg = messageToSend.get() == null ? "" : messageToSend.get().trim();
        if (base.isBlank()) { fail("Bridge/API no configurada"); connectionStatus.set("No configurado - requiere bridge/API"); return; }
        if (msg.isBlank()) { fail("Mensaje vacío"); return; }
        long now = System.currentTimeMillis();
        if (now - lastSendMs < SEND_COOLDOWN_MS) { fail("Cooldown de envío activo"); return; }
        if (now < nextReconnectMs) { fail("Reconexión en espera tras fallo"); return; }
        if (!busy.compareAndSet(false, true)) { fail("Operación en curso"); return; }
        lastSendMs = now;
        connectionStatus.set("Enviando...");
        String payload = "{\"content\":\"" + escapeJson(msg) + "\",\"channel\":\"" + escapeJson(guildChannel.get() == null ? "" : guildChannel.get().trim()) + "\"}";
        runAsync(() -> doRequest("POST", base + "/send", payload, false, msg));
    }

    private void runAsync(Runnable r) { Thread t = new Thread(r, "Nebula-DiscordConnect"); t.setDaemon(true); t.start(); }

    private void doRequest(String method, String url, String payload, boolean checkOnly) { doRequest(method, url, payload, checkOnly, null); }

    private void doRequest(String method, String url, String payload, boolean checkOnly, String originalMsg) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
            if ("POST".equals(method)) b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload == null ? "{}" : payload));
            else b.GET();
            HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code >= 200 && code < 300) {
                onUi(() -> {
                    connectionStatus.set("Conectado (bridge verificado)");
                    lastResult.set((checkOnly ? "Comprobación OK" : "Mensaje enviado") + " (HTTP " + code + ")");
                    addMsg(checkOnly ? "Bridge OK HTTP " + code : "Enviado: " + truncate(originalMsg));
                    nextReconnectMs = 0L;
                });
            } else {
                onUi(() -> {
                    connectionStatus.set("Error API/bridge (HTTP " + code + ")");
                    lastResult.set("Rechazado por bridge/API: HTTP " + code);
                    addMsg("Error HTTP " + code);
                    nextReconnectMs = System.currentTimeMillis() + RECONNECT_BASE_MS;
                });
            }
        } catch (Throwable e) {
            LOG.warn("[DiscordConnect] request failed", e);
            onUi(() -> {
                connectionStatus.set("Desconectado/error de red");
                lastResult.set("Fallo de red/bridge: " + e.getClass().getSimpleName());
                addMsg("Fallo: " + e.getClass().getSimpleName());
                nextReconnectMs = System.currentTimeMillis() + RECONNECT_BASE_MS;
            });
        } finally {
            busy.set(false);
        }
    }

    private boolean isBridgeConfigured() { return !normalizeBase(bridgeUrl.get()).isBlank(); }
    private static String normalizeBase(String url) {
        if (url == null) return "";
        String u = url.trim();
        if (u.isBlank()) return "";
        if (!(u.startsWith("https://") || u.startsWith("http://localhost") || u.startsWith("http://127.0.0.1"))) return "";
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private void clearLocalState() {
        messages.clear(); refreshMsgs(); nextReconnectMs = 0L;
        lastResult.set("Estado local limpiado");
        connectionStatus.set(isBridgeConfigured() ? "Configurado (sin verificación real)" : "No configurado - requiere bridge/API");
    }

    private void fail(String reason) { connectionStatus.set("Error"); lastResult.set(reason); addMsg(reason); }
    private void addMsg(String msg) { String m = truncate(msg); if (m.isBlank()) return; messages.addFirst(m); while (messages.size() > MAX_MESSAGES) messages.removeLast(); refreshMsgs(); }
    private void refreshMsgs() { msg1.set(get(messages, 0)); msg2.set(get(messages, 1)); msg3.set(get(messages, 2)); msg4.set(get(messages, 3)); msg5.set(get(messages, 4)); }
    private static String get(Deque<String> d, int i) { int idx = 0; for (String s : d) { if (idx == i) return s; idx++; } return "-"; }
    private static String truncate(String s) { if (s == null) return ""; String v = s.replaceAll("\\s+", " ").trim(); return v.length() > 140 ? v.substring(0, 137) + "..." : v; }
    private static String escapeJson(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
    private void resetBool(Setting<Boolean> s) { if (resetting || s == null) return; resetting = true; try { s.set(false); } finally { resetting = false; } }
    private void onUi(Runnable r) { Minecraft mc = Minecraft.getInstance(); if (mc != null) mc.execute(r); else r.run(); }
}
