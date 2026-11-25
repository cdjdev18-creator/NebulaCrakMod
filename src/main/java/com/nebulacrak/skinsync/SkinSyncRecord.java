package com.nebulacrak.skinsync;

import java.util.UUID;

public record SkinSyncRecord(
    UUID owner,
    byte[] pngBytes,
    String model, // CLASSIC or SLIM
    String versionId,
    long updatedAt
) {
    public SkinSyncRecord {
        if (owner == null) throw new IllegalArgumentException("owner");
        if (pngBytes == null || pngBytes.length == 0) throw new IllegalArgumentException("pngBytes");
        if (model == null || !(model.equals("CLASSIC") || model.equals("SLIM"))) throw new IllegalArgumentException("model");
        if (versionId == null || versionId.isBlank()) throw new IllegalArgumentException("versionId");
        if (updatedAt <= 0) throw new IllegalArgumentException("updatedAt");
    }
}
