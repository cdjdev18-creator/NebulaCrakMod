package com.nebulacrak.gui.hudmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HudmcPanelState {
    public enum TabType { CHAT, MIEMBROS, CONFIGURACION }

    public String id;
    public String name;
    public int x;
    public int y;
    public int width;
    public int height;
    public int restoreX;
    public int restoreY;
    public int restoreWidth;
    public int restoreHeight;
    public boolean minimized;
    public boolean maximized;
    public boolean closed;
    public TabType activeTab;
    public String chatChannel;
    public String chatDraft;

    public HudmcPanelState() {
        this.id = UUID.randomUUID().toString();
        this.name = "Panel 1";
        this.x = 40;
        this.y = 40;
        this.width = 320;
        this.height = 180;
        this.restoreX = x;
        this.restoreY = y;
        this.restoreWidth = width;
        this.restoreHeight = height;
        this.minimized = false;
        this.maximized = false;
        this.closed = false;
        this.activeTab = TabType.CHAT;
        this.chatChannel = "#general";
        this.chatDraft = "";
    }

    public static HudmcPanelState createDefault(int screenW, int screenH) {
        HudmcPanelState p = new HudmcPanelState();
        p.width = clamp(320, 220, Math.max(220, screenW - 40));
        p.height = clamp(180, 120, Math.max(120, screenH - 60));
        p.x = Math.max(10, (screenW - p.width) / 2);
        p.y = Math.max(30, (screenH - p.height) / 2);
        p.captureRestore();
        return p;
    }

    public void captureRestore() {
        if (!maximized && !minimized) {
            restoreX = x;
            restoreY = y;
            restoreWidth = width;
            restoreHeight = height;
        }
    }

    public void clampToScreen(int screenW, int screenH) {
        width = clamp(width, 220, Math.max(220, screenW - 20));
        height = clamp(height, minimized ? 24 : 120, Math.max(minimized ? 24 : 120, screenH - 40));
        x = clamp(x, 4, Math.max(4, screenW - width - 4));
        y = clamp(y, 24, Math.max(24, screenH - (minimized ? 24 : height) - 4));
        if (restoreWidth < 220) restoreWidth = 320;
        if (restoreHeight < 120) restoreHeight = 180;
        restoreX = clamp(restoreX, 4, Math.max(4, screenW - 40));
        restoreY = clamp(restoreY, 24, Math.max(24, screenH - 40));
        restoreWidth = clamp(restoreWidth, 220, Math.max(220, screenW - 20));
        restoreHeight = clamp(restoreHeight, 120, Math.max(120, screenH - 40));
        if (name == null || name.isBlank()) name = "Panel";
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (activeTab == null) activeTab = TabType.CHAT;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("width", width);
        o.addProperty("height", height);
        o.addProperty("restoreX", restoreX);
        o.addProperty("restoreY", restoreY);
        o.addProperty("restoreWidth", restoreWidth);
        o.addProperty("restoreHeight", restoreHeight);
        o.addProperty("minimized", minimized);
        o.addProperty("maximized", maximized);
        o.addProperty("closed", closed);
        o.addProperty("activeTab", activeTab.name());
        o.addProperty("chatChannel", chatChannel == null ? "#general" : chatChannel);
        o.addProperty("chatDraft", chatDraft == null ? "" : chatDraft);
        return o;
    }

    public static HudmcPanelState fromJson(JsonObject o) {
        HudmcPanelState p = new HudmcPanelState();
        p.id = getString(o, "id", p.id);
        p.name = getString(o, "name", p.name);
        p.x = getInt(o, "x", p.x);
        p.y = getInt(o, "y", p.y);
        p.width = getInt(o, "width", p.width);
        p.height = getInt(o, "height", p.height);
        p.restoreX = getInt(o, "restoreX", p.x);
        p.restoreY = getInt(o, "restoreY", p.y);
        p.restoreWidth = getInt(o, "restoreWidth", p.width);
        p.restoreHeight = getInt(o, "restoreHeight", p.height);
        p.minimized = getBool(o, "minimized", false);
        p.maximized = getBool(o, "maximized", false);
        p.closed = getBool(o, "closed", false);
        p.activeTab = parseTab(getString(o, "activeTab", "CHAT"));
        p.chatChannel = getString(o, "chatChannel", "#general");
        p.chatDraft = getString(o, "chatDraft", "");
        return p;
    }

    public static List<HudmcPanelState> listFromJson(JsonArray arr) {
        List<HudmcPanelState> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.size(); i++) {
            try {
                if (arr.get(i).isJsonObject()) out.add(fromJson(arr.get(i).getAsJsonObject()));
            } catch (Exception ignored) {}
        }
        return out;
    }

    public static JsonArray listToJson(List<HudmcPanelState> panels) {
        JsonArray arr = new JsonArray();
        if (panels == null) return arr;
        for (HudmcPanelState p : panels) {
            if (p != null) arr.add(p.toJson());
        }
        return arr;
    }

    private static TabType parseTab(String s) {
        try { return TabType.valueOf(s); } catch (Exception ignored) { return TabType.CHAT; }
    }

    private static int getInt(JsonObject o, String k, int d) { try { return o.get(k).getAsInt(); } catch (Exception e) { return d; } }
    private static boolean getBool(JsonObject o, String k, boolean d) { try { return o.get(k).getAsBoolean(); } catch (Exception e) { return d; } }
    private static String getString(JsonObject o, String k, String d) { try { return o.get(k).getAsString(); } catch (Exception e) { return d; } }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
