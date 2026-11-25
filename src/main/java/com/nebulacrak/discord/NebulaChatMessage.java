package com.nebulacrak.discord;

public record NebulaChatMessage(String id, String channel, String author, String authorId, String avatarUrl, String content, long timestampMs) {
    public NebulaChatMessage {
        id = id == null ? "" : id;
        channel = channel == null ? "" : channel;
        author = author == null ? "?" : author;
        authorId = authorId == null ? "" : authorId;
        avatarUrl = avatarUrl == null ? "" : avatarUrl;
        content = content == null ? "" : content;
    }
}
