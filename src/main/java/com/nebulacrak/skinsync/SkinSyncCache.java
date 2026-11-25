package com.nebulacrak.skinsync;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinSyncCache {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/SkinSyncCache");
    private final Path dir;
    private final Path indexFile;
    private final Map<UUID, SkinSyncRecord> mem = new ConcurrentHashMap<>();

    public SkinSyncCache(Path dir) {
        this.dir = dir;
        this.indexFile = dir.resolve("index.json");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        loadIndex();
    }

    public SkinSyncRecord get(UUID uuid) { return uuid == null ? null : mem.get(uuid); }

    public void put(SkinSyncRecord rec) {
        if (rec == null) return;
        SkinSyncRecord old = mem.get(rec.owner());
        if (old != null && old.versionId().equals(rec.versionId()) && old.updatedAt() == rec.updatedAt()) return;
        mem.put(rec.owner(), rec);
        try {
            Files.write(dir.resolve(rec.owner().toString() + ".png"), rec.pngBytes());
            saveIndex();
        } catch (IOException e) {
            LOG.warn("[SkinSync] cache write failed for {}", rec.owner(), e);
        }
    }

    public Collection<SkinSyncRecord> all() { return mem.values(); }

    public void clear() {
        mem.clear();
        try {
            if (Files.isDirectory(dir)) {
                try (var s = Files.list(dir)) {
                    for (Path p : (Iterable<Path>) s::iterator) Files.deleteIfExists(p);
                }
            }
        } catch (IOException ignored) {}
    }

    private void loadIndex() {
        if (!Files.exists(indexFile)) return;
        try (Reader r = Files.newBufferedReader(indexFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (String key : root.keySet()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    JsonObject o = root.getAsJsonObject(key);
                    String model = o.get("model").getAsString();
                    String version = o.get("versionId").getAsString();
                    long updatedAt = o.get("updatedAt").getAsLong();
                    Path png = dir.resolve(uuid + ".png");
                    if (!Files.exists(png)) continue;
                    byte[] bytes = Files.readAllBytes(png);
                    mem.put(uuid, new SkinSyncRecord(uuid, bytes, model, version, updatedAt));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOG.warn("[SkinSync] cache index load failed", e);
        }
    }

    private synchronized void saveIndex() {
        JsonObject root = new JsonObject();
        for (SkinSyncRecord rec : mem.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("model", rec.model());
            o.addProperty("versionId", rec.versionId());
            o.addProperty("updatedAt", rec.updatedAt());
            root.add(rec.owner().toString(), o);
        }
        try (Writer w = Files.newBufferedWriter(indexFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
        } catch (IOException e) {
            LOG.warn("[SkinSync] cache index save failed", e);
        }
    }
}
