package com.nebulacrak.gui.hudmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudmcManager {
    private final List<HudmcPanelState> panels = new ArrayList<>();
    private int panelCounter = 1;

    public List<HudmcPanelState> panels() { return panels; }

    public void ensureDefault(int screenW, int screenH) {
        if (panels.isEmpty()) {
            HudmcPanelState p = HudmcPanelState.createDefault(screenW, screenH);
            p.name = "Panel 1";
            panels.add(p);
            panelCounter = 2;
            return;
        }
        validate(screenW, screenH);
        if (panels.stream().allMatch(p -> p.closed)) {
            panels.get(0).closed = false;
        }
    }

    public void validate(int screenW, int screenH) {
        for (HudmcPanelState p : panels) p.clampToScreen(screenW, screenH);
        int max = 1;
        for (HudmcPanelState p : panels) {
            if (p.name != null && p.name.startsWith("Panel ")) {
                try { max = Math.max(max, Integer.parseInt(p.name.substring(6).trim()) + 1); } catch (Exception ignored) {}
            }
        }
        panelCounter = Math.max(panelCounter, max);
    }

    public HudmcPanelState createPanel(int screenW, int screenH) {
        HudmcPanelState p = HudmcPanelState.createDefault(screenW, screenH);
        int offset = (panels.size() % 6) * 18;
        p.x = clamp(p.x + offset, 8, Math.max(8, screenW - p.width - 8));
        p.y = clamp(p.y + offset, 28, Math.max(28, screenH - p.height - 8));
        p.captureRestore();
        p.name = "Panel " + panelCounter++;
        panels.add(p);
        return p;
    }

    public void bringToFront(HudmcPanelState panel) {
        if (panel == null || panels.isEmpty() || panels.get(panels.size() - 1) == panel) return;
        panels.remove(panel);
        panels.add(panel);
    }

    public HudmcPanelState panelAt(double mx, double my) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            HudmcPanelState p = panels.get(i);
            if (p.closed) continue;
            int h = p.minimized ? 24 : p.height;
            if (mx >= p.x && mx <= p.x + p.width && my >= p.y && my <= p.y + h) return p;
        }
        return null;
    }

    public void load(JsonObject root, int screenW, int screenH) {
        panels.clear();
        panelCounter = 1;
        if (root != null && root.has("hudmcPanels")) {
            JsonElement el = root.get("hudmcPanels");
            if (el.isJsonArray()) {
                for (HudmcPanelState p : HudmcPanelState.listFromJson(el.getAsJsonArray())) panels.add(p);
            }
        }
        if (panels.isEmpty()) ensureDefault(screenW, screenH);
        validate(screenW, screenH);
    }

    public void save(JsonObject root) {
        root.add("hudmcPanels", HudmcPanelState.listToJson(panels));
    }

    public List<HudmcPanelState> visiblePanels() {
        List<HudmcPanelState> out = new ArrayList<>();
        for (HudmcPanelState p : panels) if (!p.closed) out.add(p);
        return out;
    }

    public List<HudmcPanelState> allPanelsSorted() {
        List<HudmcPanelState> out = new ArrayList<>(panels);
        out.sort(Comparator.comparing(p -> p.name == null ? "" : p.name));
        return out;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
