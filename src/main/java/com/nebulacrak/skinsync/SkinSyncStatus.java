package com.nebulacrak.skinsync;

public enum SkinSyncStatus {
    DISCONNECTED("Desconectado"),
    CONNECTING("Conectando"),
    CONNECTED("Conectado"),
    SYNCING("Sincronizando"),
    ERROR("Error de conexión");

    public final String label;
    SkinSyncStatus(String label) { this.label = label; }
}
