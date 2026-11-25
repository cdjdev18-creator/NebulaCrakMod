package com.nebulacrak.skinsync;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Decoupled transport for Nebula SkinSync. Implementations may use HTTP, WebSocket, etc.
 * Implementations MUST NOT block the Minecraft main thread.
 */
public interface SkinSyncTransport {
    CompletableFuture<Void> connect(UUID selfUuid);
    CompletableFuture<Void> disconnect();
    CompletableFuture<Void> publish(SkinSyncRecord record);
    CompletableFuture<List<SkinSyncRecord>> fetchUpdates(UUID selfUuid, long sinceEpochMs);
    CompletableFuture<List<UUID>> fetchCompatibleUsers();
    boolean isConnected();
    String statusDetail();
}
