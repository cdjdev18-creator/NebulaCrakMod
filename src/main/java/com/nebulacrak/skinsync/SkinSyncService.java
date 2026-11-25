package com.nebulacrak.skinsync;

import com.nebulacrak.gui.ImageFileUtil;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class SkinSyncService {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/SkinSync");
    private static final SkinSyncService INSTANCE = new SkinSyncService();
    public static SkinSyncService get() { return INSTANCE; }

    public interface Listener {
        void onStatus(SkinSyncStatus status, String detail);
        void onRecord(SkinSyncRecord record);
    }

    private final AtomicReference<SkinSyncStatus> status = new AtomicReference<>(SkinSyncStatus.DISCONNECTED);
    private volatile String detail = "Desconectado";
    private volatile SkinSyncTransport transport;
    private volatile Listener listener;
    private volatile SkinSyncCache cache;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Nebula-SkinSync"); t.setDaemon(true); return t;
    });
    private final AtomicLong lastFetchMs = new AtomicLong(0);
    private volatile ScheduledFuture<?> pollFuture;
    private volatile UUID selfUuid;
    private volatile boolean shareMySkin = true;
    private volatile boolean showCompatible = true;

    private SkinSyncService() {}

    public void setListener(Listener l) { this.listener = l; }
    public void setShareMySkin(boolean v) { this.shareMySkin = v; }
    public void setShowCompatible(boolean v) { this.showCompatible = v; }
    public void setShowCompatibleSkins(boolean v) { this.showCompatible = v; }
    public SkinSyncStatus status() { return status.get(); }
    public String detail() { return detail; }
    public SkinSyncCache cache() { return cache; }

    public synchronized void configure(String baseUrl) {
        HttpSkinSyncTransport http = new HttpSkinSyncTransport();
        http.setBaseUrl(baseUrl);
        this.transport = http;
        if (cache == null) {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config/nebulacrakmod/skinsync-cache");
            cache = new SkinSyncCache(dir);
        }
    }

    public void connect(UUID self) {
        this.selfUuid = self;
        SkinSyncTransport t = transport;
        if (t == null || self == null) { setStatus(SkinSyncStatus.ERROR, "Servicio no configurado"); return; }
        setStatus(SkinSyncStatus.CONNECTING, "Conectando...");
        t.connect(self).whenCompleteAsync((v, e) -> {
            if (e != null) { setStatus(SkinSyncStatus.ERROR, safeErr(e)); return; }
            setStatus(SkinSyncStatus.CONNECTED, t.statusDetail());
            schedulePoll();
        }, exec);
    }

    public void disconnect() {
        ScheduledFuture<?> pf = pollFuture; pollFuture = null;
        if (pf != null) pf.cancel(false);
        SkinSyncTransport t = transport;
        if (t != null) t.disconnect();
        setStatus(SkinSyncStatus.DISCONNECTED, "Desconectado");
    }

    public void shutdown() { disconnect(); exec.shutdownNow(); }

    public void publishLocalSkin(Path pngPath, String model, UUID self) {
        if (!shareMySkin) return;
        SkinSyncTransport t = transport;
        if (t == null || self == null || pngPath == null) return;
        if (status.get() != SkinSyncStatus.CONNECTED && status.get() != SkinSyncStatus.SYNCING) return;
        exec.execute(() -> {
            try {
                String err = ImageFileUtil.validateSkinPng(pngPath);
                if (err != null) { LOG.warn("[SkinSync] invalid skin not published: {}", err); return; }
                byte[] bytes = Files.readAllBytes(pngPath);
                String normalizedModel = "SLIM".equalsIgnoreCase(model) ? "SLIM" : "CLASSIC";
                String versionId = Integer.toHexString(Arrays.hashCode(bytes)) + "-" + bytes.length;
                SkinSyncRecord rec = new SkinSyncRecord(self, bytes, normalizedModel, versionId, System.currentTimeMillis());
                setStatus(SkinSyncStatus.SYNCING, "Publicando skin...");
                t.publish(rec).whenCompleteAsync((v, e) -> {
                    if (e != null) setStatus(SkinSyncStatus.ERROR, "Error al publicar: " + safeErr(e));
                    else setStatus(SkinSyncStatus.CONNECTED, "Skin publicada");
                }, exec);
            } catch (Exception e) {
                setStatus(SkinSyncStatus.ERROR, safeErr(e));
            }
        });
    }

    public void clearCache() {
        if (cache != null) cache.clear();
    }

    private void schedulePoll() {
        ScheduledFuture<?> pf = pollFuture;
        if (pf != null) pf.cancel(false);
        pollFuture = exec.scheduleWithFixedDelay(this::pollOnce, 2, 10, TimeUnit.SECONDS);
    }

    private void pollOnce() {
        SkinSyncTransport t = transport;
        UUID self = selfUuid;
        if (t == null || self == null || !t.isConnected()) return;
        long since = lastFetchMs.get();
        setStatus(SkinSyncStatus.SYNCING, "Sincronizando...");
        t.fetchUpdates(self, since).whenCompleteAsync((list, e) -> {
            if (e != null) { setStatus(SkinSyncStatus.ERROR, safeErr(e)); return; }
            lastFetchMs.set(System.currentTimeMillis());
            for (SkinSyncRecord rec : list) {
                if (rec.owner().equals(self)) continue; // do not apply own remote echo as other-player override
                if (!showCompatible) continue;
                String err = ImageFileUtil.validateSkinPngBytes(rec.pngBytes());
                if (err != null) continue;
                if (cache != null) cache.put(rec);
                Listener l = listener; if (l != null) l.onRecord(rec);
            }
            setStatus(SkinSyncStatus.CONNECTED, "Conectado");
        }, exec);
    }

    private void setStatus(SkinSyncStatus s, String d) {
        status.set(s); detail = d == null ? s.label : d;
        Listener l = listener; if (l != null) l.onStatus(s, detail);
        LOG.info("[SkinSync] {} - {}", s, detail);
    }

    private static String safeErr(Throwable e) {
        Throwable c = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
        return c.getClass().getSimpleName() + (c.getMessage() != null ? ": " + c.getMessage() : "");
    }
}
