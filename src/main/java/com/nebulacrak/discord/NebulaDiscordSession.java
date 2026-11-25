package com.nebulacrak.discord;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NebulaDiscordSession {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/DiscordSession");
    private static final NebulaDiscordSession I = new NebulaDiscordSession();
    public static NebulaDiscordSession get() { return I; }

    public enum Status { DISCONNECTED, CONFIG_REQUIRED, CONNECTING, AUTHENTICATING, CONNECTED, ERROR }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final List<NebulaChatMessage> messages = new CopyOnWriteArrayList<>();
    private final List<String> onlineUsers = new CopyOnWriteArrayList<>();
    private final List<String> channels = new CopyOnWriteArrayList<>(List.of("#general", "#minecraft", "#anuncios"));
    private volatile Status status = Status.DISCONNECTED;
    private volatile String statusDetail = "No conectado";
    private volatile String sessionToken = "";
    private volatile NebulaDiscordProfile profile = NebulaDiscordProfile.empty();
    private volatile String currentChannel = "#general";
    private volatile WebSocket ws;
    private volatile long lastReconnectMs;
    private volatile long lastPollMs;

    private NebulaDiscordSession() {}

    public Status status() { return status; }
    public String statusDetail() { return statusDetail; }
    public NebulaDiscordProfile profile() { return profile; }
    public List<NebulaChatMessage> messages() { return messages; }
    public List<String> onlineUsers() { return onlineUsers; }
    public List<String> channels() { return channels; }
    public String currentChannel() { return currentChannel; }
    public boolean isConnected() { return status == Status.CONNECTED && !sessionToken.isBlank(); }

    public void setCurrentChannel(String ch) {
        if (ch == null || ch.isBlank()) return;
        currentChannel = ch;
        if (isConnected()) {
            fetchHistoryAsync();
            fetchMembersAsync();
            sendWs("{\"type\":\"join\",\"channel\":\"" + esc(ch) + "\"}");
        }
    }

    public void beginLogin(String backendBase, String clientIdPublicHint) {
        String base = normalizeBase(backendBase);
        if (base.isBlank()) { setStatus(Status.CONFIG_REQUIRED, "Backend no configurado"); return; }
        // Never use client secret in mod. Backend handles OAuth2 secret/callback exchange.
        String returnTo = base + "/oauth2/start?client=nebula-mc&redirect=" + enc(base + "/oauth2/callback");
        setStatus(Status.AUTHENTICATING, "Abriendo navegador para OAuth2 Discord...");
        try {
            Util.getPlatform().openUri(returnTo);
            setStatus(Status.AUTHENTICATING, "Autoriza en navegador y pega el token de sesión de Nebula si el backend lo muestra.");
        } catch (Throwable t) {
            LOG.warn("OAuth open failed", t);
            setStatus(Status.ERROR, "No se pudo abrir navegador: " + t.getClass().getSimpleName());
        }
    }

    public void applySessionToken(String backendBase, String token) {
        String base = normalizeBase(backendBase);
        String t = token == null ? "" : token.trim();
        if (base.isBlank()) { setStatus(Status.CONFIG_REQUIRED, "Backend no configurado"); return; }
        if (t.isBlank()) { setStatus(Status.ERROR, "Token de sesión vacío"); return; }
        sessionToken = t;
        setStatus(Status.CONNECTING, "Validando sesión...");
        async(() -> {
            try {
                JsonObject me = getJson(base + "/api/me");
                profile = parseProfile(me);
                if (!profile.isValid()) throw new IllegalStateException("Perfil inválido");
                setStatus(Status.CONNECTED, "Sesión vinculada");
                fetchChannelsAsync(base);
                fetchHistoryAsync(base);
                fetchMembersAsync(base);
                connectWs(base);
            } catch (Throwable e) {
                LOG.warn("Session validation failed", e);
                sessionToken = "";
                profile = NebulaDiscordProfile.empty();
                setStatus(Status.ERROR, "Sesión inválida/no autorizada: " + safeErr(e));
            }
        });
    }

    public void logout(String backendBase) {
        String base = normalizeBase(backendBase);
        String t = sessionToken;
        sessionToken = "";
        profile = NebulaDiscordProfile.empty();
        messages.clear();
        onlineUsers.clear();
        closeWs();
        setStatus(Status.DISCONNECTED, "Sesión cerrada");
        if (!base.isBlank() && !t.isBlank()) {
            async(() -> { try { postJson(base + "/api/logout", new JsonObject()); } catch (Throwable ignored) {} });
        }
    }

    public void sendMessage(String backendBase, String content) {
        String base = normalizeBase(backendBase);
        String msg = content == null ? "" : content.trim();
        if (!isConnected()) { setStatus(Status.ERROR, "No autenticado"); return; }
        if (base.isBlank()) { setStatus(Status.CONFIG_REQUIRED, "Backend no configurado"); return; }
        if (msg.isBlank()) return;
        if (msg.length() > 500) msg = msg.substring(0, 500);
        JsonObject body = new JsonObject();
        body.addProperty("channel", currentChannel);
        body.addProperty("content", msg);
        String finalMsg = msg;
        async(() -> {
            try {
                postJson(base + "/api/messages", body);
                // Do not fake receive; wait for WS/history. As fallback, fetch history.
                fetchHistoryAsync(base);
            } catch (Throwable e) {
                LOG.warn("Send failed", e);
                setStatus(Status.ERROR, "No se pudo enviar: " + safeErr(e));
            }
        });
    }

    public void poll(String backendBase) {
        long now = System.currentTimeMillis();
        if (now - lastPollMs < 5000) return;
        lastPollMs = now;
        if (!isConnected()) return;
        String base = normalizeBase(backendBase);
        if (base.isBlank()) return;
        fetchHistoryAsync(base);
        fetchMembersAsync(base);
    }

    private void fetchChannelsAsync() { String b = lastBackend; if (!b.isBlank()) fetchChannelsAsync(b); }
    private void fetchHistoryAsync() { String b = lastBackend; if (!b.isBlank()) fetchHistoryAsync(b); }
    private void fetchMembersAsync() { String b = lastBackend; if (!b.isBlank()) fetchMembersAsync(b); }
    private volatile String lastBackend = "";

    private void fetchChannelsAsync(String base) {
        lastBackend = base;
        async(() -> {
            try {
                JsonArray arr = getJsonArray(base + "/api/channels");
                List<String> next = new ArrayList<>();
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        String name = opt(o, "name");
                        if (!name.isBlank()) next.add(name.startsWith("#") ? name : "#" + name);
                    } else if (el.isJsonPrimitive()) next.add(el.getAsString());
                }
                if (!next.isEmpty()) {
                    channels.clear(); channels.addAll(next);
                    if (!channels.contains(currentChannel)) currentChannel = channels.get(0);
                }
            } catch (Throwable ignored) {}
        });
    }

    private void fetchHistoryAsync(String base) {
        lastBackend = base;
        async(() -> {
            try {
                JsonArray arr = getJsonArray(base + "/api/messages?channel=" + enc(currentChannel) + "&limit=50");
                List<NebulaChatMessage> next = new ArrayList<>();
                for (JsonElement el : arr) if (el.isJsonObject()) next.add(parseMsg(el.getAsJsonObject()));
                next.sort(Comparator.comparingLong(NebulaChatMessage::timestampMs));
                messages.clear();
                messages.addAll(next.size() > 100 ? next.subList(next.size() - 100, next.size()) : next);
            } catch (Throwable e) {
                if (isConnected()) setStatus(Status.ERROR, "Backend desconectado: " + safeErr(e));
            }
        });
    }

    private void fetchMembersAsync(String base) {
        lastBackend = base;
        async(() -> {
            try {
                JsonArray arr = getJsonArray(base + "/api/members?channel=" + enc(currentChannel));
                List<String> next = new ArrayList<>();
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        String n = opt(o, "username"); if (n.isBlank()) n = opt(o, "name");
                        String id = opt(o, "id");
                        if (!n.isBlank()) next.add(id.isBlank() ? n : n + " (" + id + ")");
                    } else if (el.isJsonPrimitive()) next.add(el.getAsString());
                }
                onlineUsers.clear(); onlineUsers.addAll(next);
                if (status == Status.ERROR && !sessionToken.isBlank()) setStatus(Status.CONNECTED, "Sesión vinculada");
            } catch (Throwable ignored) {}
        });
    }

    private void connectWs(String base) {
        closeWs();
        try {
            String wsUrl = base.replaceFirst("^http", "ws") + "/ws?token=" + enc(sessionToken);
            ws = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                private final StringBuilder buf = new StringBuilder();
                @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    buf.append(data);
                    if (last) {
                        String s = buf.toString(); buf.setLength(0);
                        handleWsMessage(s);
                    }
                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }
                @Override public void onError(WebSocket webSocket, Throwable error) {
                    LOG.warn("WS error", error);
                    if (isConnected()) setStatus(Status.ERROR, "WS desconectado: " + safeErr(error));
                }
                @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    if (!sessionToken.isBlank()) NebulaDiscordSession.this.status = Status.ERROR;
                    return CompletableFuture.completedFuture(null);
                }
            }).join();
            sendWs("{\"type\":\"join\",\"channel\":\"" + esc(currentChannel) + "\"}");
        } catch (Throwable e) {
            LOG.warn("WS connect failed", e);
            setStatus(Status.ERROR, "WS no disponible: " + safeErr(e));
        }
    }

    private void handleWsMessage(String s) {
        try {
            JsonObject o = JsonParser.parseString(s).getAsJsonObject();
            String type = opt(o, "type");
            if ("message".equals(type) || o.has("content")) {
                NebulaChatMessage m = parseMsg(o.has("message") && o.get("message").isJsonObject() ? o.getAsJsonObject("message") : o);
                if (!m.content().isBlank()) {
                    messages.add(m);
                    while (messages.size() > 100) messages.remove(0);
                }
            } else if ("members".equals(type) && o.has("members") && o.get("members").isJsonArray()) {
                onlineUsers.clear();
                for (JsonElement el : o.getAsJsonArray("members")) if (el.isJsonPrimitive()) onlineUsers.add(el.getAsString());
            } else if ("status".equals(type)) {
                setStatus(Status.CONNECTED, opt(o, "detail", "Conectado"));
            }
        } catch (Throwable ignored) {}
    }

    private void sendWs(String text) {
        try { WebSocket w = ws; if (w != null) w.sendText(text, true); } catch (Throwable ignored) {}
    }
    private void closeWs() { try { WebSocket w = ws; ws = null; if (w != null) w.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); } catch (Throwable ignored) {} }

    private JsonObject getJson(String url) throws Exception { return JsonParser.parseString(request("GET", url, null)).getAsJsonObject(); }
    private JsonArray getJsonArray(String url) throws Exception { return JsonParser.parseString(request("GET", url, null)).getAsJsonArray(); }
    private void postJson(String url, JsonObject body) throws Exception { request("POST", url, body.toString()); }

    private String request(String method, String url, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (!sessionToken.isBlank()) b.header("Authorization", "Bearer " + sessionToken);
        if ("POST".equals(method)) b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
        else b.GET();
        HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) throw new IllegalStateException("HTTP " + res.statusCode());
        return res.body() == null ? "{}" : res.body();
    }

    private NebulaChatMessage parseMsg(JsonObject o) {
        String id = opt(o, "id");
        String ch = opt(o, "channel", currentChannel);
        String author = opt(o, "author"); if (author.isBlank()) author = opt(o, "username");
        String authorId = opt(o, "authorId"); if (authorId.isBlank()) authorId = opt(o, "discordId");
        String avatar = opt(o, "avatar"); if (avatar.isBlank()) avatar = opt(o, "avatarUrl");
        String content = opt(o, "content"); if (content.isBlank()) content = opt(o, "message");
        long ts = optLong(o, "timestamp", System.currentTimeMillis());
        if (ts < 1_000_000_000_000L) ts = ts * 1000L;
        return new NebulaChatMessage(id, ch, author, authorId, avatar, content, ts);
    }

    private NebulaDiscordProfile parseProfile(JsonObject o) {
        JsonObject u = o.has("user") && o.get("user").isJsonObject() ? o.getAsJsonObject("user") : o;
        String id = opt(u, "id"); if (id.isBlank()) id = opt(u, "discordId");
        String username = opt(u, "username"); if (username.isBlank()) username = opt(u, "name");
        String avatar = opt(u, "avatarUrl"); if (avatar.isBlank()) avatar = opt(u, "avatar");
        return new NebulaDiscordProfile(id, username, avatar);
    }

    private void setStatus(Status s, String detail) {
        this.status = s;
        this.statusDetail = detail == null ? "" : detail;
    }

    private void async(Runnable r) {
        Thread t = new Thread(r, "Nebula-DiscordSession");
        t.setDaemon(true);
        t.start();
    }

    public static String normalizeBase(String url) {
        if (url == null) return "";
        String u = url.trim();
        if (u.isBlank()) return "";
        if (!(u.startsWith("https://") || u.startsWith("http://localhost") || u.startsWith("http://127.0.0.1"))) return "";
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static String opt(JsonObject o, String k) { return opt(o, k, ""); }
    private static String opt(JsonObject o, String k, String d) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d; } catch (Exception e) { return d; } }
    private static long optLong(JsonObject o, String k, long d) { try { return o.has(k) ? o.get(k).getAsLong() : d; } catch (Exception e) { return d; } }
    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String safeErr(Throwable t) { String m = t.getMessage(); return t.getClass().getSimpleName() + (m == null || m.isBlank() ? "" : ": " + m); }
}
