package com.nebulacrak.skinsync;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * HTTP transport skeleton. Requires an external Nebula SkinSync service (not included).
 * Endpoints (expected):
 *  POST {base}/v1/session {uuid}
 *  DELETE {base}/v1/session/{uuid}
 *  PUT {base}/v1/skins/{uuid} {owner,model,versionId,updatedAt,pngBase64}
 *  GET {base}/v1/skins?since=...&self=...
 *  GET {base}/v1/users
 * Server MUST reject publishes where path UUID != body owner != authenticated session UUID.
 */
public final class HttpSkinSyncTransport implements SkinSyncTransport {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/SkinSyncHttp");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile String baseUrl = "";
    private volatile boolean connected = false;
    private volatile String detail = "No configurado";

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }

    @Override
    public CompletableFuture<Void> connect(UUID selfUuid) {
        if (selfUuid == null) return CompletableFuture.failedFuture(new IllegalArgumentException("selfUuid"));
        if (baseUrl.isBlank()) { detail = "URL de servicio vacía"; return CompletableFuture.failedFuture(new IllegalStateException(detail)); }
        JsonObject body = new JsonObject(); body.addProperty("uuid", selfUuid.toString());
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/session"))
            .timeout(Duration.ofSeconds(8))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
            if (res.statusCode() / 100 == 2) { connected = true; detail = "Conectado"; }
            else { connected = false; detail = "HTTP " + res.statusCode(); throw new RuntimeException(detail); }
        });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        connected = false;
        detail = "Desconectado";
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> publish(SkinSyncRecord record) {
        if (!connected) return CompletableFuture.failedFuture(new IllegalStateException("No conectado"));
        JsonObject body = new JsonObject();
        body.addProperty("owner", record.owner().toString());
        body.addProperty("model", record.model());
        body.addProperty("versionId", record.versionId());
        body.addProperty("updatedAt", record.updatedAt());
        body.addProperty("pngBase64", Base64.getEncoder().encodeToString(record.pngBytes()));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/skins/" + record.owner()))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
            if (res.statusCode() / 100 != 2) throw new RuntimeException("Publish HTTP " + res.statusCode());
        });
    }

    @Override
    public CompletableFuture<List<SkinSyncRecord>> fetchUpdates(UUID selfUuid, long sinceEpochMs) {
        if (!connected) return CompletableFuture.completedFuture(List.of());
        String url = baseUrl + "/v1/skins?since=" + sinceEpochMs + "&self=" + selfUuid;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() / 100 != 2) throw new RuntimeException("Fetch HTTP " + res.statusCode());
            List<SkinSyncRecord> out = new ArrayList<>();
            JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    UUID owner = UUID.fromString(o.get("owner").getAsString());
                    String model = o.get("model").getAsString();
                    String versionId = o.get("versionId").getAsString();
                    long updatedAt = o.get("updatedAt").getAsLong();
                    byte[] png = Base64.getDecoder().decode(o.get("pngBase64").getAsString());
                    out.add(new SkinSyncRecord(owner, png, model, versionId, updatedAt));
                } catch (Exception ignored) {}
            }
            return out;
        });
    }

    @Override
    public CompletableFuture<List<UUID>> fetchCompatibleUsers() {
        if (!connected) return CompletableFuture.completedFuture(List.of());
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/users")).timeout(Duration.ofSeconds(10)).GET().build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() / 100 != 2) throw new RuntimeException("Users HTTP " + res.statusCode());
            List<UUID> out = new ArrayList<>();
            JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();
            for (JsonElement el : arr) {
                try { out.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {}
            }
            return out;
        });
    }

    @Override public boolean isConnected() { return connected; }
    @Override public String statusDetail() { return detail; }
}
