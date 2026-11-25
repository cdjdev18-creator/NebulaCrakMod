package com.nebulacrak.discord;

public record NebulaDiscordProfile(String id, String username, String avatarUrl) {
    public static NebulaDiscordProfile empty() { return new NebulaDiscordProfile("", "", ""); }
    public boolean isValid() { return username != null && !username.isBlank(); }
    public String displayName() {
        if (!isValid()) return "No conectado";
        return id == null || id.isBlank() ? username : username + " (" + id + ")";
    }
}
