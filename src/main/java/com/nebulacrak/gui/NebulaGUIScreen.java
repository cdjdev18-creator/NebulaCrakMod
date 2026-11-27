package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.nebulacrak.gui.hudmc.HudmcManager;
import com.nebulacrak.gui.hudmc.HudmcPanelRenderer;
import com.nebulacrak.gui.hudmc.HudmcPanelState;
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class NebulaGUIScreen extends Screen {

    public enum Tab { NEBULA, METEOR, HUD, EDIT_MC, CONFI }

    private final Screen parent;
    private Tab currentTab = Tab.NEBULA;

    private boolean nebulaRootExpanded = true;
    private boolean languageDropdownOpen = false;
    private final Map<String, Boolean> categoryExpandedStates = new HashMap<>();
    private String draggingPanelId = null;
    private int dragOffX, dragOffY;

    // HUDMC
    private final HudmcManager hudmcManager = new HudmcManager();
    private final HudmcPanelRenderer hudmcRenderer = new HudmcPanelRenderer();
    private HudmcPanelState hudmcDragging;
    private HudmcPanelState hudmcResizing;
    private int hudmcDragOffX, hudmcDragOffY;
    private int hudmcResizeStartW, hudmcResizeStartH, hudmcResizeStartMouseX, hudmcResizeStartMouseY;
    private boolean hudmcContextOpen;
    private int hudmcContextX, hudmcContextY;
    private HudmcPanelState hudmcConfigPanel;
    private String hudmcFocusedChatPanelId;

    private final List<AbstractWidget> navButtons = new ArrayList<>();
    private final List<AbstractWidget> contentButtons = new ArrayList<>();
    private CompactButton languageButton;
    private record PanelRect(String id, int x, int y, int w, int h, boolean expanded) {}
    private final List<PanelRect> panelRects = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private boolean waitingForKeybind = false;

    private AbstractWidget hoveredWidget = null;
    private long hoverStartTime = 0;
    private static final long TOOLTIP_DELAY_MS = 1500;

    private int topBarH() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 20 : 24; }
    private int contentTop() { return topBarH() + 4; }
    private int footerPad() { return 8; }
    private int panelWMax() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 460 : 430; }
    private int panelPadX() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 10 : 12; }
    private int rowHModule() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 14 : 16; }
    private int rowHCategory() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 16 : 18; }
    private int rowHHeader() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 18 : 20; }
    private int rowGap() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 2 : 4; }

    public NebulaGUIScreen(Screen parent) {
        super(Component.literal("NebulaCrakMod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        navButtons.clear();
        contentButtons.clear();
        languageDropdownOpen = false;

        boolean ed = NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION;
        int tabH = ed ? 12 : 16;
        int tabY = 4;
        int gap = ed ? 2 : 3;
        int langBtnW = ed ? 56 : 64;
        int leftBound = 8;
        int langBtnX = this.width - langBtnW - 8;

        Tab[] tabs = Tab.values();
        String[] labels = {
            TranslationManager.get("nebula.tab.nebula", "NebulaCrakMod"),
            TranslationManager.get("nebula.tab.meteor", "CDJ dev"),
            TranslationManager.get("nebula.tab.hud", "HUD"),
            TranslationManager.get("nebula.tab.edit_mc", "EDIT,MC"),
            TranslationManager.get("nebula.tab.conf", "CONFI")
        };

        int available = Math.max(120, langBtnX - leftBound - 8);
        int maxTabW = ed ? 66 : 82;
        int minTabW = ed ? 38 : 46;
        int tabW = Math.max(minTabW, Math.min(maxTabW, (available - gap * (tabs.length - 1)) / tabs.length));
        int totalTabsW = tabW * tabs.length + gap * (tabs.length - 1);
        int tabsStartX = leftBound;
        if (tabsStartX + totalTabsW > langBtnX - 6) {
            tabW = Math.max(minTabW, (langBtnX - 6 - leftBound - gap * (tabs.length - 1)) / tabs.length);
            totalTabsW = tabW * tabs.length + gap * (tabs.length - 1);
        }
        if (tabsStartX + totalTabsW > langBtnX - 6) {
            tabsStartX = Math.max(8, langBtnX - 6 - totalTabsW);
        }

        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];
            int x = tabsStartX + i * (tabW + gap);
            CompactButton btn = new CompactButton(x, tabY, tabW, tabH, Component.literal(labels[i]),
                b -> { if (currentTab != tab) { currentTab = tab; scrollOffset = 0; } rebuild(); },
                CompactButton.Type.TAB);
            btn.setSelected(currentTab == tab);
            navButtons.add(btn);
            addRenderableWidget(btn);
        }

        String langCode = LanguageManager.code().toUpperCase(Locale.ROOT);
        languageButton = new CompactButton(langBtnX, tabY, langBtnW, tabH,
            Component.literal(langCode),
            b -> languageDropdownOpen = !languageDropdownOpen,
            CompactButton.Type.LANGUAGE);
        navButtons.add(languageButton);
        addRenderableWidget(languageButton);

        loadHudmcFromConfig();
        rebuild();
    }

    private void rebuild() {
        for (AbstractWidget b : contentButtons) removeWidget(b);
        contentButtons.clear();
        panelRects.clear();
        hoveredWidget = null;

        for (int i = 0; i < navButtons.size(); i++) {
            AbstractWidget w = navButtons.get(i);
            if (w instanceof CompactButton b && b != languageButton) {
                Tab[] tabs = Tab.values();
                if (i < tabs.length) b.setSelected(currentTab == tabs[i]);
            }
        }

        int panelW = Math.min(panelWMax(), this.width - 24);
        int panelLeft = (this.width - panelW) / 2;
        int cLeft = panelLeft + panelPadX();
        int cWidth = panelW - panelPadX() * 2;
        int y = contentTop() + (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 6 : 8) - scrollOffset;

        switch (currentTab) {
            case NEBULA -> y = renderNebulaModules(cLeft, y, cWidth);
            case METEOR -> y = renderMeteorModules(cLeft, y, cWidth);
            case HUD -> y = renderHUD(cLeft, y, cWidth);
            case EDIT_MC -> y = renderEditMC(cLeft, y, cWidth);
            case CONFI -> y = renderConfi(cLeft, y, cWidth);
        }

        calculateMaxScroll(y + scrollOffset);
        updateWidgetVisibility();
    }

    private void addContent(AbstractWidget btn) {
        contentButtons.add(btn);
        addRenderableWidget(btn);
    }

    private void updateWidgetVisibility() {
        int top = contentTop();
        int bottom = this.height - footerPad();
        for (AbstractWidget b : contentButtons) {
            boolean vis = b.getY() + b.getHeight() > top && b.getY() < bottom;
            b.visible = vis;
            b.active = vis;
        }
    }

    private void calculateMaxScroll(int contentBottom) {
        int visibleBottom = this.height - footerPad();
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }


    private int renderNebulaModules(int left, int y, int width) {
        if (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION) return renderNebulaModulesEdition(y);
        return renderNebulaModulesOriginal(left, y, width);
    }

    private int renderNebulaModulesOriginal(int left, int y, int width) {
        String arrow = nebulaRootExpanded ? "v " : "> ";
        String rootTitle = arrow + TranslationManager.get("nebula.section.root", "NebulaCrakMod");
        CompactButton rootBtn = new CompactButton(left, y, width, rowHHeader(),
                Component.literal(rootTitle),
                b -> { nebulaRootExpanded = !nebulaRootExpanded; NebulaConfig.dropdownExpanded.put("nebula_root", nebulaRootExpanded); NebulaConfig.save(); rebuild(); },
                CompactButton.Type.MASTER_HEADER);
        addContent(rootBtn);
        y += rowHHeader() + rowGap();
        if (!nebulaRootExpanded) return y;

        int catLeft = left + 4;
        int catWidth = width - 8;
        for (NebulaModules.CDJCategory category : NebulaModules.CDJ_CATEGORIES) {
            String catKey = "nebula.cat." + category.getId();
            boolean isExpanded = categoryExpandedStates.getOrDefault(catKey, true);
            String catArrow = isExpanded ? "v " : "> ";
            CompactButton catBtn = new CompactButton(catLeft, y, catWidth, rowHCategory(),
                    Component.literal(catArrow + category.getDisplayName()),
                    b -> { categoryExpandedStates.put(catKey, !isExpanded); NebulaConfig.dropdownExpanded.put(catKey, !isExpanded); NebulaConfig.save(); rebuild(); },
                    CompactButton.Type.CATEGORY);
            addContent(catBtn);
            y += rowHCategory() + 2;
            if (isExpanded) {
                int modLeft = catLeft + 6;
                int modWidth = catWidth - 12;
                for (Module module : category.getModules()) y = addModuleRow(modLeft, y, modWidth, module);
                y += rowGap();
            }
        }
        return y;
    }

    private int renderNebulaModulesEdition(int ignoredY) {
        int pw = panelW();
        int ph = panelH("nebula_root", -1);
        int px = panelX("nebula_root");
        int py = panelY("nebula_root");
        nebulaRootExpanded = NebulaConfig.dropdownExpanded.getOrDefault("nebula_root", nebulaRootExpanded);
        drawDraggablePanel("nebula_root", px, py, pw, ph, TranslationManager.get("nebula.section.root", "NebulaCrakMod"), nebulaRootExpanded, () -> {
            nebulaRootExpanded = !nebulaRootExpanded;
            NebulaConfig.dropdownExpanded.put("nebula_root", nebulaRootExpanded);
            NebulaConfig.save();
            rebuild();
        });
        int y = py + 20 - scrollOffset;
        if (!nebulaRootExpanded) return py + ph + 6;
        int catLeft = px + 6;
        int catWidth = pw - 12;
        for (NebulaModules.CDJCategory category : NebulaModules.CDJ_CATEGORIES) {
            String catKey = "nebula.cat." + category.getId();
            boolean isExpanded = NebulaConfig.dropdownExpanded.getOrDefault(catKey, categoryExpandedStates.getOrDefault(catKey, true));
            categoryExpandedStates.put(catKey, isExpanded);
            String catArrow = isExpanded ? "v " : "> ";
            CompactButton catBtn = new CompactButton(catLeft, y, catWidth, rowHCategory(),
                    Component.literal(catArrow + category.getDisplayName()),
                    b -> { boolean nv = !NebulaConfig.dropdownExpanded.getOrDefault(catKey, true); categoryExpandedStates.put(catKey, nv); NebulaConfig.dropdownExpanded.put(catKey, nv); NebulaConfig.save(); rebuild(); },
                    CompactButton.Type.CATEGORY);
            addContent(catBtn);
            y += rowHCategory() + 1;
            if (isExpanded) {
                int modLeft = catLeft + 4;
                int modWidth = catWidth - 8;
                for (Module module : category.getModules()) y = addModuleRow(modLeft, y, modWidth, module);
                y += 2;
            }
        }
        return Math.max(py + ph + 6, y + scrollOffset + 6);
    }

    private int renderMeteorModules(int left, int y, int width) {
        if (MeteorClient.mc == null || Modules.get() == null) {
            addContent(new CompactButton(left, y, width, 18,
                    Component.literal(TranslationManager.get("gui.meteor.not_loaded", "Meteor Client not loaded")),
                    b -> {}, CompactButton.Type.CATEGORY));
            return y + 22;
        }

        Map<Category, List<Module>> byCategory = new LinkedHashMap<>();
        for (Module mod : Modules.get().getAll()) {
            if (mod.category == null) continue;
            String cname = mod.category.name;
            if (cname.startsWith("nebula.") || cname.startsWith("Nebula") || "CDJ dev+".equals(cname)) continue;
            byCategory.computeIfAbsent(mod.category, k -> new ArrayList<>()).add(mod);
        }

        List<Category> sorted = new ArrayList<>(byCategory.keySet());
        sorted.sort(Comparator.comparing(c -> c.name));
        if (sorted.isEmpty()) {
            addContent(new CompactButton(left, y, width, 18,
                    Component.literal(TranslationManager.get("gui.meteor.none", "No Meteor modules found")),
                    b -> {}, CompactButton.Type.CATEGORY));
            return y + 22;
        }

        if (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION) return renderMeteorModulesEdition(sorted, byCategory);
        return renderMeteorModulesOriginal(left, y, width, sorted, byCategory);
    }

    private int renderMeteorModulesOriginal(int left, int y, int width, List<Category> sorted, Map<Category, List<Module>> byCategory) {
        for (Category cat : sorted) {
            String stateKey = "meteor." + cat.name;
            boolean isExpanded = NebulaConfig.dropdownExpanded.getOrDefault(stateKey, categoryExpandedStates.getOrDefault(stateKey, false));
            categoryExpandedStates.put(stateKey, isExpanded);
            String arrow = isExpanded ? "v " : "> ";
            String catName = TranslationManager.getCategoryName(cat.name);
            CompactButton catBtn = new CompactButton(left, y, width, rowHCategory(),
                    Component.literal(arrow + catName),
                    b -> { boolean nv = !NebulaConfig.dropdownExpanded.getOrDefault(stateKey, false); categoryExpandedStates.put(stateKey, nv); NebulaConfig.dropdownExpanded.put(stateKey, nv); NebulaConfig.save(); rebuild(); },
                    CompactButton.Type.CATEGORY);
            addContent(catBtn);
            y += rowHCategory() + 2;
            if (isExpanded) {
                int modLeft = left + 6;
                int modWidth = width - 12;
                List<Module> mods = byCategory.get(cat);
                if (mods != null) for (Module mod : mods) y = addModuleRow(modLeft, y, modWidth, mod);
                y += rowGap();
            }
        }
        return y;
    }

    private int renderMeteorModulesEdition(List<Category> sorted, Map<Category, List<Module>> byCategory) {
        int bottom = contentTop() + 8;
        int col = 0;
        int colX = 8;
        int colW = panelW();
        for (Category cat : sorted) {
            String stateKey = "meteor." + cat.name;
            boolean isExpanded = NebulaConfig.dropdownExpanded.getOrDefault(stateKey, categoryExpandedStates.getOrDefault(stateKey, false));
            categoryExpandedStates.put(stateKey, isExpanded);
            int px = panelX(stateKey, colX);
            int py = panelY(stateKey, contentTop() + 8 + col * 24);
            int ph = panelH(stateKey, byCategory.getOrDefault(cat, List.of()).size());
            drawDraggablePanel(stateKey, px, py, colW, ph, TranslationManager.getCategoryName(cat.name), isExpanded, () -> {
                boolean nv = !NebulaConfig.dropdownExpanded.getOrDefault(stateKey, false);
                categoryExpandedStates.put(stateKey, nv);
                NebulaConfig.dropdownExpanded.put(stateKey, nv);
                NebulaConfig.save();
                rebuild();
            });
            int y = py + 20 - scrollOffset;
            if (isExpanded) {
                int modLeft = px + 4;
                int modWidth = colW - 8;
                for (Module mod : byCategory.getOrDefault(cat, List.of())) y = addModuleRow(modLeft, y, modWidth, mod);
            }
            bottom = Math.max(bottom, py + ph + 6);
            col++;
        }
        return bottom;
    }

    private int renderHUD(int left, int y, int width) {
        hudmcManager.ensureDefault(this.width, this.height);
        syncHudmcToConfig();
        if (hudmcConfigPanel != null && !hudmcManager.panels().contains(hudmcConfigPanel)) hudmcConfigPanel = null;
        if (hudmcConfigPanel != null) {
            y = renderHudmcConfigPanel(left, y, width, hudmcConfigPanel);
        } else {
            CompactButton info = new CompactButton(left, y, width, 18,
                    Component.literal("HUDMC: click derecho en espacio vacío para abrir menú contextual."),
                    b -> {}, CompactButton.Type.LABEL);
            info.active = false;
            addContent(info);
            y += 22;
            CompactButton create = new CompactButton(left, y, Math.min(160, width), 20,
                    Component.literal("+ Nuevo panel"),
                    b -> { hudmcManager.createPanel(this.width, this.height); syncHudmcToConfig(); rebuild(); },
                    CompactButton.Type.SETTINGS);
            addContent(create);
            y += 26;
        }
        return y;
    }

    private int renderHudmcConfigPanel(int left, int y, int width, HudmcPanelState p) {
        CompactButton back = new CompactButton(left, y, 90, 20, Component.literal("< Volver"), b -> { hudmcConfigPanel = null; rebuild(); }, CompactButton.Type.SETTINGS);
        addContent(back);
        y += 24;
        CompactButton title = new CompactButton(left, y, width, 18, Component.literal("Configurar paneles - " + p.name), b -> {}, CompactButton.Type.MASTER_HEADER);
        title.active = false;
        addContent(title);
        y += 22;
        String[] labels = {"Chat", "Miembros", "Configuración"};
        HudmcPanelState.TabType[] tabs = HudmcPanelState.TabType.values();
        for (int i = 0; i < tabs.length; i++) {
            HudmcPanelState.TabType t = tabs[i];
            boolean sel = p.activeTab == t;
            CompactButton tb = new CompactButton(left + 4, y, width - 8, 18,
                    Component.literal((sel ? "[x] " : "[ ] ") + "Tab: " + labels[i]),
                    b -> { p.activeTab = t; syncHudmcToConfig(); rebuild(); },
                    sel ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            addContent(tb);
            y += 20;
        }
        y += 4;
        CompactButton min = new CompactButton(left + 4, y, width - 8, 20,
                Component.literal(p.minimized ? "Restaurar panel" : "Minimizar panel"),
                b -> { hudmcRenderer.toggleMinimize(p, this.width, this.height); syncHudmcToConfig(); rebuild(); },
                CompactButton.Type.SETTINGS);
        addContent(min);
        y += 22;
        CompactButton max = new CompactButton(left + 4, y, width - 8, 20,
                Component.literal(p.maximized ? "Restaurar tamaño" : "Maximizar panel"),
                b -> { hudmcRenderer.toggleMaximize(p, this.width, this.height); syncHudmcToConfig(); rebuild(); },
                CompactButton.Type.SETTINGS);
        addContent(max);
        y += 22;
        CompactButton close = new CompactButton(left + 4, y, width - 8, 20,
                Component.literal("Cerrar panel"),
                b -> { p.closed = true; hudmcConfigPanel = null; syncHudmcToConfig(); rebuild(); },
                CompactButton.Type.DANGER);
        addContent(close);
        y += 24;
        return y;
    }

    private void syncHudmcToConfig() {
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            hudmcManager.save(root);
            NebulaConfig.hudmcPanelsJson = root.get("hudmcPanels").toString();
            NebulaConfig.save();
        } catch (Exception ignored) {}
    }

    private void loadHudmcFromConfig() {
        try {
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(
                    NebulaConfig.hudmcPanelsJson == null || NebulaConfig.hudmcPanelsJson.isBlank() ? "[]" : NebulaConfig.hudmcPanelsJson
            ).getAsJsonArray();
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            root.add("hudmcPanels", arr);
            hudmcManager.load(root, this.width, this.height);
        } catch (Exception ignored) {
            hudmcManager.ensureDefault(this.width, this.height);
        }
        hudmcManager.validate(this.width, this.height);
    }

    private void renderHudmcPanels(GuiGraphics g, int mouseX, int mouseY) {
        if (currentTab != Tab.HUD) return;
        List<HudmcPanelState> visible = hudmcManager.panels();
        for (HudmcPanelState p : visible) {
            if (p.closed) continue;
            boolean active = !visible.isEmpty() && visible.get(visible.size() - 1) == p;
            // find last non-closed
        }
        HudmcPanelState last = null;
        for (HudmcPanelState p : visible) if (!p.closed) last = p;
        for (HudmcPanelState p : visible) {
            if (p.closed) continue;
            hudmcRenderer.renderPanel(g, p, p == last, mouseX, mouseY);
        }
        if (hudmcContextOpen) renderHudmcContextMenu(g, mouseX, mouseY);
    }

    private List<String> hudmcContextItems() {
        List<String> items = new ArrayList<>();
        items.add("+ Nuevo panel");
        for (HudmcPanelState p : hudmcManager.allPanelsSorted()) items.add((p.closed ? "[C] " : "") + p.name);
        items.add("Configurar paneles");
        return items;
    }

    private void renderHudmcContextMenu(GuiGraphics g, int mouseX, int mouseY) {
        List<String> items = hudmcContextItems();
        int w = 170;
        int itemH = 16;
        int h = items.size() * itemH + 4;
        int x = clamp(hudmcContextX, 4, Math.max(4, this.width - w - 4));
        int yPos = clamp(hudmcContextY, topBarH() + 2, Math.max(topBarH() + 2, this.height - h - 4));
        g.fill(x, yPos, x + w, yPos + h, 0xF0141822);
        g.fill(x, yPos, x + w, yPos + 1, 0xFF8B5CF6);
        g.fill(x, yPos + h - 1, x + w, yPos + h, 0xFF2D3342);
        g.fill(x, yPos, x + 1, yPos + h, 0xFF2D3342);
        g.fill(x + w - 1, yPos, x + w, yPos + h, 0xFF2D3342);
        for (int i = 0; i < items.size(); i++) {
            int iy = yPos + 2 + i * itemH;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= iy && mouseY < iy + itemH;
            if (hover) g.fill(x + 1, iy, x + w - 1, iy + itemH, 0xFF273142);
            g.drawString(Minecraft.getInstance().font, items.get(i), x + 8, iy + 4, hover ? 0xFFFFFFFF : 0xFFD1D5DB, false);
        }
    }

    private boolean handleHudmcContextClick(double mx, double my) {
        List<String> items = hudmcContextItems();
        int w = 170;
        int itemH = 16;
        int h = items.size() * itemH + 4;
        int x = clamp(hudmcContextX, 4, Math.max(4, this.width - w - 4));
        int yPos = clamp(hudmcContextY, topBarH() + 2, Math.max(topBarH() + 2, this.height - h - 4));
        if (mx < x || mx >= x + w || my < yPos || my >= yPos + h) { hudmcContextOpen = false; return false; }
        int idx = (int) ((my - yPos - 2) / itemH);
        if (idx < 0 || idx >= items.size()) { hudmcContextOpen = false; return true; }
        String item = items.get(idx);
        if ("+ Nuevo panel".equals(item)) {
            HudmcPanelState p = hudmcManager.createPanel(this.width, this.height);
            hudmcManager.bringToFront(p);
            syncHudmcToConfig(); hudmcContextOpen = false; rebuild(); return true;
        }
        if ("Configurar paneles".equals(item)) {
            List<HudmcPanelState> all = hudmcManager.allPanelsSorted();
            hudmcConfigPanel = all.isEmpty() ? null : all.get(0);
            hudmcContextOpen = false; rebuild(); return true;
        }
        for (HudmcPanelState p : hudmcManager.allPanelsSorted()) {
            String label = (p.closed ? "[C] " : "") + p.name;
            if (label.equals(item)) {
                p.closed = false; hudmcManager.bringToFront(p); hudmcConfigPanel = p;
                syncHudmcToConfig(); hudmcContextOpen = false; rebuild(); return true;
            }
        }
        hudmcContextOpen = false; return true;
    }

    private boolean handleHudmcMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (currentTab != Tab.HUD) return false;
        double mx = event.x(), my = event.y();
        if (hudmcContextOpen) {
            if (handleHudmcContextClick(mx, my)) return true;
            hudmcContextOpen = false;
        }
        if (event.button() == 1) {
            HudmcPanelState p = hudmcManager.panelAt(mx, my);
            if (p != null) { hudmcManager.bringToFront(p); syncHudmcToConfig(); rebuild(); return true; }
            if (my >= topBarH() && my < this.height) {
                hudmcContextOpen = true; hudmcContextX = (int) mx; hudmcContextY = (int) my; return true;
            }
            return false;
        }
        if (event.button() != 0) return false;
        for (int i = hudmcManager.panels().size() - 1; i >= 0; i--) {
            HudmcPanelState p = hudmcManager.panels().get(i);
            if (p.closed) continue;
            int h = p.minimized ? HudmcPanelRenderer.HEADER_H : p.height;
            if (mx < p.x || mx > p.x + p.width || my < p.y || my > p.y + h) continue;
            hudmcManager.bringToFront(p);
            if (hudmcRenderer.mouseClicked(p, event, hudmcActions(), this.width, this.height)) return true;
            if (hudmcRenderer.isResizeHit(p, mx, my)) {
                hudmcResizing = p;
                hudmcResizeStartW = p.width; hudmcResizeStartH = p.height;
                hudmcResizeStartMouseX = (int) mx; hudmcResizeStartMouseY = (int) my;
                return true;
            }
            if (hudmcRenderer.isHeaderHit(p, mx, my)) {
                hudmcDragging = p; hudmcDragOffX = (int) mx - p.x; hudmcDragOffY = (int) my - p.y;
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean handleHudmcChatKey(KeyEvent event) {
        if (hudmcFocusedChatPanelId == null) return false;
        HudmcPanelState panel = null;
        for (HudmcPanelState p : hudmcManager.panels()) if (p.id.equals(hudmcFocusedChatPanelId) && !p.closed) { panel = p; break; }
        if (panel == null || panel.activeTab != HudmcPanelState.TabType.CHAT || panel.minimized) return false;
        int key = event.key();
        String draft = panel.chatDraft == null ? "" : panel.chatDraft;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            hudmcActions().sendChat(panel);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!draft.isEmpty()) { panel.chatDraft = draft.substring(0, draft.length() - 1); syncHudmcToConfig(); rebuild(); }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            hudmcFocusedChatPanelId = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (currentTab == Tab.HUD && hudmcFocusedChatPanelId != null) {
            HudmcPanelState panel = null;
            for (HudmcPanelState p : hudmcManager.panels()) if (p.id.equals(hudmcFocusedChatPanelId) && !p.closed) { panel = p; break; }
            if (panel != null && panel.activeTab == HudmcPanelState.TabType.CHAT && !panel.minimized) {
                int cp = event.codepoint();
                if (cp >= 32 && cp != 127) {
                    String draft = panel.chatDraft == null ? "" : panel.chatDraft;
                    if (draft.length() < 500) {
                        panel.chatDraft = draft + new String(Character.toChars(cp));
                        syncHudmcToConfig();
                        rebuild();
                    }
                    return true;
                }
            }
        }
        return super.charTyped(event);
    }

    private HudmcPanelRenderer.Actions hudmcActions() {
        return new HudmcPanelRenderer.Actions() {
            @Override public void rebuild() { NebulaGUIScreen.this.rebuild(); }
            @Override public void save() { syncHudmcToConfig(); }
            @Override public void bringToFront(HudmcPanelState panel) { hudmcManager.bringToFront(panel); }
            @Override public void openContextMenu(int x, int y) { hudmcContextOpen = true; hudmcContextX = x; hudmcContextY = y; }
            @Override public void openConfig(HudmcPanelState panel) { hudmcConfigPanel = panel; rebuild(); }
            @Override public void closePanel(HudmcPanelState panel) { panel.closed = true; if (hudmcConfigPanel == panel) hudmcConfigPanel = null; }
            @Override public void loginDiscord(HudmcPanelState panel) {
                hudmcFocusedChatPanelId = panel.id;
                var mod = com.nebulacrak.modules.DiscordConnectModule.INSTANCE;
                if (mod != null) mod.triggerLogin();
                syncHudmcToConfig(); rebuild();
            }
            @Override public void logoutDiscord(HudmcPanelState panel) {
                var mod = com.nebulacrak.modules.DiscordConnectModule.INSTANCE;
                if (mod != null) mod.triggerLogout();
                syncHudmcToConfig(); rebuild();
            }
            @Override public void sendChat(HudmcPanelState panel) {
                var mod = com.nebulacrak.modules.DiscordConnectModule.INSTANCE;
                if (mod != null) {
                    mod.setChannel(panel.chatChannel == null ? "#general" : panel.chatChannel);
                    mod.setMessageDraft(panel.chatDraft == null ? "" : panel.chatDraft);
                    mod.triggerSend();
                    panel.chatDraft = "";
                }
                syncHudmcToConfig(); rebuild();
            }
            @Override public void setChatChannel(HudmcPanelState panel, String channel) {
                panel.chatChannel = channel == null ? "#general" : channel;
                hudmcFocusedChatPanelId = panel.id;
                var mod = com.nebulacrak.modules.DiscordConnectModule.INSTANCE;
                if (mod != null) mod.setChannel(panel.chatChannel);
                com.nebulacrak.discord.NebulaDiscordSession.get().setCurrentChannel(panel.chatChannel);
                syncHudmcToConfig(); rebuild();
            }
        };
    }
    private int renderEditMC(int left, int y, int width) {
        NebulaConfig.BackgroundMode[] std = {NebulaConfig.BackgroundMode.ORIGINAL, NebulaConfig.BackgroundMode.COLOR, NebulaConfig.BackgroundMode.TRANSPARENT, NebulaConfig.BackgroundMode.IMAGE};
        String[] stdLabels = {
                TranslationManager.get("confi.fondo.disabled", "Original"),
                TranslationManager.get("confi.fondo.color", "Color Sólido"),
                TranslationManager.get("confi.splash.transparent", "Transparente"),
                TranslationManager.get("confi.fondo.image", "Imagen Personalizada")
        };
        NebulaConfig.BackgroundMode[] noTransparent = {NebulaConfig.BackgroundMode.ORIGINAL, NebulaConfig.BackgroundMode.COLOR, NebulaConfig.BackgroundMode.IMAGE};
        String[] noTransparentLabels = {
                TranslationManager.get("confi.fondo.disabled", "Original"),
                TranslationManager.get("confi.fondo.color", "Color Sólido"),
                TranslationManager.get("confi.fondo.image", "Imagen Personalizada")
        };

        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.main_menu.title", "Menú Principal"), TranslationManager.get("gui.nebula.edit_mc.main_menu.desc", "Personaliza el fondo del menú principal de Minecraft."), "mainMenu", noTransparent, noTransparentLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.singleplayer.title", "Un jugador / Selección de mundos"), TranslationManager.get("gui.nebula.edit_mc.singleplayer.desc", "Personaliza el fondo de selección de mundos."), "worldSelect", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.multiplayer.title", "Multijugador"), TranslationManager.get("gui.nebula.edit_mc.multiplayer.desc", "Personaliza el fondo de la pantalla multijugador."), "multiplayer", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.world_create.title", "Creación de mundos"), TranslationManager.get("gui.nebula.edit_mc.world_create.desc", "Personaliza el fondo de creación de mundos."), "worldCreate", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.options.title", "Configuración"), TranslationManager.get("gui.nebula.edit_mc.options.desc", "Personaliza el fondo de pantallas de opciones/configuración."), "options", std, stdLabels); y += 6;
        y = renderEditMCLoadingBarSection(left, y, width);
        y += 6;
        y = sectionHeader(left, y, width,
                TranslationManager.get("gui.nebula.edit_mc.loading_panels.title", "Paneles de carga"),
                TranslationManager.get("gui.nebula.edit_mc.loading_panels.desc", "Personaliza por separado los paneles de carga de Minecraft."));
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.mojang.title", "Pantalla de Mojang"), TranslationManager.get("gui.nebula.edit_mc.mojang.desc", "Personaliza el fondo del splash inicial de Mojang. Logo, barra y progreso se conservan."), "mojang", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.world_loading.title", "Cargando mundo / progreso"), TranslationManager.get("gui.nebula.edit_mc.world_loading.desc", "Cargando mundo, terreno, progreso, reconexión/espera."), "worldLoading", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.connecting.title", "Conectando a servidor"), TranslationManager.get("gui.nebula.edit_mc.connecting.desc", "Conectando/uniéndose a servidor."), "connecting", std, stdLabels); y += 6;
        y = renderEditMCSection(left, y, width, TranslationManager.get("gui.nebula.edit_mc.disconnected.title", "Desconectado"), TranslationManager.get("gui.nebula.edit_mc.disconnected.desc", "Pantalla de desconexión del servidor."), "disconnected", std, stdLabels);
        return y;
    }

    private int renderEditMCLoadingBarSection(int left, int y, int width) {
        y = sectionHeader(left, y, width,
                TranslationManager.get("gui.nebula.edit_mc.loading_bar.title", "Barra de carga de recursos"),
                TranslationManager.get("gui.nebula.edit_mc.loading_bar.desc", "Controla solo el relleno rojo de progreso de la barra de carga."));
        NebulaConfig.LoadingBarMode[] values = NebulaConfig.LoadingBarMode.values();
        String[] labels = {
                TranslationManager.get("gui.nebula.edit_mc.loading_bar.original", "Original"),
                TranslationManager.get("gui.nebula.edit_mc.loading_bar.transparent", "Transparente")
        };
        for (int i = 0; i < values.length; i++) {
            NebulaConfig.LoadingBarMode mode = values[i];
            boolean selected = NebulaConfig.loadingBarMode == mode;
            CompactButton btn = new CompactButton(left + 6, y, width - 12, 20,
                    Component.literal(labels[i]),
                    b -> { NebulaConfig.loadingBarMode = mode; NebulaConfig.save(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            btn.setSelected(selected);
            addContent(btn);
            y += 22;
        }
        return y + 2;
    }

    private int sectionHeader(int left, int y, int width, String title, String desc) {
        CompactButton header = new CompactButton(left, y, width, 18, Component.literal(title), b -> {}, CompactButton.Type.SECTION);
        header.active = false;
        addContent(header);
        y += 20;
        CompactButton sub = new CompactButton(left, y, width, 14, Component.literal(desc), b -> {}, CompactButton.Type.LABEL);
        sub.active = false;
        addContent(sub);
        y += 18;
        return y;
    }

    private int renderEditMCSection(int left, int y, int width, String title, String desc, String prefix,
                                    NebulaConfig.BackgroundMode[] modes, String[] labels) {
        y = sectionHeader(left, y, width, title, desc);

        NebulaConfig.BackgroundMode currentMode = getBackgroundMode(prefix);
        for (int i = 0; i < modes.length; i++) {
            NebulaConfig.BackgroundMode mode = modes[i];
            boolean selected = currentMode == mode;
            CompactButton modeBtn = new CompactButton(left + 6, y, width - 12, 20,
                    Component.literal(labels[i]),
                    b -> { setBackgroundMode(prefix, mode); NebulaConfig.save(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            modeBtn.setSelected(selected);
            addContent(modeBtn);
            y += 22;
        }
        y += 2;

        NebulaConfig.BackgroundMode selectedMode = getBackgroundMode(prefix);
        if (selectedMode == NebulaConfig.BackgroundMode.COLOR) {
            y = buildColorPicker(left + 6, y, width - 12, getBackgroundColor(prefix), c -> {
                setBackgroundColor(prefix, c); NebulaConfig.save(); rebuild();
            });
        } else if (selectedMode == NebulaConfig.BackgroundMode.IMAGE) {
            y = buildEditMCImageSelector(left + 6, y, width - 12, getImagePath(prefix),
                    p -> { setImagePath(prefix, p); NebulaConfig.save(); rebuild(); },
                    () -> { setImagePath(prefix, ""); setBackgroundMode(prefix, NebulaConfig.BackgroundMode.ORIGINAL); NebulaConfig.save(); rebuild(); });
        }
        return y;
    }

    private NebulaConfig.BackgroundMode getBackgroundMode(String prefix) {
        return switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuBackgroundMode;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectBackgroundMode;
            case "multiplayer" -> NebulaConfig.multiplayerBackgroundMode;
            case "worldCreate" -> NebulaConfig.worldCreateBackgroundMode;
            case "options" -> NebulaConfig.optionsBackgroundMode;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingBackgroundMode;
            case "connecting" -> NebulaConfig.connectingBackgroundMode;
            case "disconnected" -> NebulaConfig.disconnectedBackgroundMode;
            case "mojang" -> NebulaConfig.mojangScreenBackgroundMode;
            default -> NebulaConfig.BackgroundMode.ORIGINAL;
        };
    }
    private void setBackgroundMode(String prefix, NebulaConfig.BackgroundMode mode) {
        switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuBackgroundMode = mode;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectBackgroundMode = mode;
            case "multiplayer" -> NebulaConfig.multiplayerBackgroundMode = mode;
            case "worldCreate" -> NebulaConfig.worldCreateBackgroundMode = mode;
            case "options" -> NebulaConfig.optionsBackgroundMode = mode;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingBackgroundMode = mode;
            case "connecting" -> NebulaConfig.connectingBackgroundMode = mode;
            case "disconnected" -> NebulaConfig.disconnectedBackgroundMode = mode;
            case "mojang" -> NebulaConfig.mojangScreenBackgroundMode = mode;
        }
    }
    private int getBackgroundColor(String prefix) {
        return switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuBackgroundColor;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectBackgroundColor;
            case "multiplayer" -> NebulaConfig.multiplayerBackgroundColor;
            case "worldCreate" -> NebulaConfig.worldCreateBackgroundColor;
            case "options" -> NebulaConfig.optionsBackgroundColor;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingBackgroundColor;
            case "connecting" -> NebulaConfig.connectingBackgroundColor;
            case "disconnected" -> NebulaConfig.disconnectedBackgroundColor;
            case "mojang" -> NebulaConfig.mojangScreenBackgroundColor;
            default -> 0xFF121218;
        };
    }
    private void setBackgroundColor(String prefix, int color) {
        switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuBackgroundColor = color;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectBackgroundColor = color;
            case "multiplayer" -> NebulaConfig.multiplayerBackgroundColor = color;
            case "worldCreate" -> NebulaConfig.worldCreateBackgroundColor = color;
            case "options" -> NebulaConfig.optionsBackgroundColor = color;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingBackgroundColor = color;
            case "connecting" -> NebulaConfig.connectingBackgroundColor = color;
            case "disconnected" -> NebulaConfig.disconnectedBackgroundColor = color;
            case "mojang" -> NebulaConfig.mojangScreenBackgroundColor = color;
        }
    }
    private String getImagePath(String prefix) {
        return switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuImagePath;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectImagePath;
            case "multiplayer" -> NebulaConfig.multiplayerImagePath;
            case "worldCreate" -> NebulaConfig.worldCreateImagePath;
            case "options" -> NebulaConfig.optionsImagePath;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingImagePath;
            case "connecting" -> NebulaConfig.connectingImagePath;
            case "disconnected" -> NebulaConfig.disconnectedImagePath;
            case "mojang" -> NebulaConfig.mojangScreenImagePath;
            default -> "";
        };
    }
    private void setImagePath(String prefix, String path) {
        switch (prefix) {
            case "mainMenu" -> NebulaConfig.mainMenuImagePath = path;
            case "singleplayer", "worldSelect" -> NebulaConfig.worldSelectImagePath = path;
            case "multiplayer" -> NebulaConfig.multiplayerImagePath = path;
            case "worldCreate" -> NebulaConfig.worldCreateImagePath = path;
            case "options" -> NebulaConfig.optionsImagePath = path;
            case "loading", "worldLoading" -> NebulaConfig.worldLoadingImagePath = path;
            case "connecting" -> NebulaConfig.connectingImagePath = path;
            case "disconnected" -> NebulaConfig.disconnectedImagePath = path;
            case "mojang" -> NebulaConfig.mojangScreenImagePath = path;
        }
    }


    private int buildColorPicker(int left, int y, int width, int currentColor, java.util.function.IntConsumer setter) {
        int[] quickColors = {
            0xFF121218, 0xFF1E1E1E, 0xFF0D1117, 0xFF1F1F2E,
            0xFF2D1B2D, 0xFF1B2D2D, 0xFF2D2D1B, 0xFF000000,
            0xFF8B0000, 0xFF006400, 0xFF00008B, 0xFF4B0082,
            0xFFFFD700, 0xFFFF8C00, 0xFFFF0000, 0xFF00FFFF
        };

        int swatchSize = 14;
        int gap = 3;
        int total = quickColors.length;
        int rowWidth = total * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);

        for (int i = 0; i < total; i++) {
            final int color = quickColors[i];
            boolean isSelected = (currentColor == color);
            int sx = startX + i * (swatchSize + gap);
            CompactButton swatch = new CompactButton(sx, y, swatchSize, swatchSize, Component.empty(),
                    b -> setter.accept(color), CompactButton.Type.SETTINGS) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                    g.fill(x, iy, x + w, iy + h, isSelected ? 0xFFFFFFFF : 0xFF555565);
                    g.fill(x + 1, iy + 1, x + w - 1, iy + h - 1, color | 0xFF000000);
                    if (isSelected) {
                        g.fill(x + w / 2 - 1, iy + h / 2 - 1, x + w / 2 + 2, iy + h / 2 + 2, 0xFFFFFFFF);
                    }
                }
            };
            addContent(swatch);
        }
        y += swatchSize + 8;

        ColorInputButton hexInput = new ColorInputButton(left, y, width, 22, currentColor, c -> {
            setter.accept(c);
            rebuild();
        });
        addContent(hexInput);
        y += 26;
        return y;
    }

    private int buildEditMCImageSelector(int left, int y, int width, String currentPath,
                                         java.util.function.Consumer<String> onSelect, Runnable onClear) {
        CompactButton browseBtn = new CompactButton(left, y, width, 22,
                Component.literal(TranslationManager.get("gui.nebula.hud.image.browse", "Seleccionar Imagen")),
                b -> openFileChooser(onSelect),
                CompactButton.Type.SETTINGS);
        addContent(browseBtn);
        y += 24;

        if (currentPath != null && !currentPath.isEmpty()) {
            String displayPath = shortenPath(currentPath);
            CompactButton previewBtn = new CompactButton(left, y, width, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.current", "Imagen: ") + displayPath),
                    b -> openImageInViewer(currentPath),
                    CompactButton.Type.LABEL);
            previewBtn.active = false;
            addContent(previewBtn);
            y += 24;

            CompactButton replaceBtn = new CompactButton(left, y, width, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.replace", "Reemplazar Imagen")),
                    b -> openFileChooser(onSelect),
                    CompactButton.Type.SETTINGS);
            addContent(replaceBtn);
            y += 24;

            CompactButton clearBtn = new CompactButton(left, y, width, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.remove", "Eliminar Imagen")),
                    b -> onClear.run(),
                    CompactButton.Type.DANGER);
            addContent(clearBtn);
            y += 24;
        } else {
            CompactButton noneBtn = new CompactButton(left, y, width, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.none", "Sin imagen seleccionada")),
                    b -> {}, CompactButton.Type.LABEL);
            noneBtn.active = false;
            addContent(noneBtn);
            y += 24;
        }
        return y;
    }


    private int renderConfi(int left, int y, int width) {
        String styleTitle = TranslationManager.get("confi.ui_style.title", "GUI Style");
        String styleDesc = TranslationManager.get("confi.ui_style.desc", "Switch between ORIGINAL and EDICION interface styles.");
        y = sectionHeader(left, y, width, styleTitle, styleDesc);
        NebulaConfig.UIStyle[] styles = NebulaConfig.UIStyle.values();
        String[] styleNames = {
                TranslationManager.get("confi.ui_style.original", "ORIGINAL"),
                TranslationManager.get("confi.ui_style.edition", "EDICIÓN")
        };
        for (int i = 0; i < styles.length; i++) {
            NebulaConfig.UIStyle st = styles[i];
            boolean selected = NebulaConfig.uiStyle == st;
            CompactButton b = new CompactButton(left + 6, y, width - 12, 20,
                    Component.literal(styleNames[i]),
                    btn -> { NebulaConfig.uiStyle = st; NebulaConfig.save(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            b.setSelected(selected);
            addContent(b);
            y += 22;
        }
        y += 4;

        if (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION) {
            y = renderEditionColorSettings(left, y, width);
        }

        String keybindTitle = TranslationManager.get("confi.keybind.title", "Open Key");
        String keybindDesc = TranslationManager.get("confi.keybind.desc", "Key to open/close the NebulaCrakMod GUI.");
        y = sectionHeader(left, y, width, keybindTitle, keybindDesc);

        String keyName = GLFW.glfwGetKeyName(NebulaConfig.openKeyCode, 0);
        if (keyName == null) keyName = "KEY_" + NebulaConfig.openKeyCode;
        keyName = keyName.toUpperCase(Locale.ROOT);
        String currentLabel = TranslationManager.get("confi.keybind.current", "Current: ");
        String changeLabel = waitingForKeybind
                ? TranslationManager.get("confi.keybind.waiting", "Press any key...")
                : TranslationManager.get("confi.keybind.change", "Change Key");

        int btnW = 120;
        int btnH = 20;
        int btnX = left + width - btnW;
        CompactButton currentKeyBtn = new CompactButton(left, y, width - btnW - 8, btnH,
                Component.literal(currentLabel + keyName), b -> {}, CompactButton.Type.LABEL);
        currentKeyBtn.active = false;
        addContent(currentKeyBtn);
        CompactButton changeKeyBtn = new CompactButton(btnX, y, btnW, btnH,
                Component.literal(changeLabel),
                b -> { waitingForKeybind = !waitingForKeybind; rebuild(); },
                waitingForKeybind ? CompactButton.Type.DANGER : CompactButton.Type.SETTINGS);
        addContent(changeKeyBtn);
        y += 26;

        String closeKeyTitle = TranslationManager.get("confi.close_with_key.title", "Close with Open Key");
        String closeKeyDesc = TranslationManager.get("confi.close_with_key.desc", "When enabled, the same key that opens the GUI also closes it.");
        y = sectionHeader(left, y, width, closeKeyTitle, closeKeyDesc);
        y = boolRow(left, y, width, closeKeyTitle, NebulaConfig.closeWithOpenKey, v -> {
            NebulaConfig.closeWithOpenKey = v; NebulaConfig.save(); rebuild();
        });
        y += 4;

        String fontTitle = TranslationManager.get("confi.font.title", "Font");
        String fontDesc = TranslationManager.get("confi.font.fallback", "Fallback to default if unavailable");
        y = sectionHeader(left, y, width, fontTitle, fontDesc);
        String[] fontOptions = {
                TranslationManager.get("confi.font.arial", "Arial"),
                TranslationManager.get("confi.font.consolas", "Consolas")
        };
        for (int i = 0; i < fontOptions.length; i++) {
            final NebulaConfig.FontType ft = NebulaConfig.FontType.values()[i];
            boolean selected = NebulaConfig.fontType == ft;
            CompactButton fontBtn = new CompactButton(left + 6, y, width - 12, 20,
                    Component.literal(fontOptions[i]),
                    b -> { NebulaConfig.fontType = ft; NebulaConfig.save(); FontManager.clearCache(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            fontBtn.setSelected(selected);
            addContent(fontBtn);
            y += 22;
        }
        y += 4;

        String buttonStyleTitle = TranslationManager.get("confi.button_style.title", "Button Style");
        String buttonStyleDesc = TranslationManager.get("confi.button_style.desc", "Changes the appearance of buttons in NebulaCrakMod GUI.");
        y = sectionHeader(left, y, width, buttonStyleTitle, buttonStyleDesc);
        String[] styleOptions = {
                TranslationManager.get("confi.button_style.default", "Default (Minecraft)"),
                TranslationManager.get("confi.button_style.transparent", "Transparent"),
                TranslationManager.get("confi.button_style.meteor", "Meteor")
        };
        for (int i = 0; i < styleOptions.length; i++) {
            final NebulaConfig.ButtonStyle bs = NebulaConfig.ButtonStyle.values()[i];
            boolean selected = NebulaConfig.buttonStyle == bs;
            CompactButton styleBtn = new CompactButton(left + 6, y, width - 12, 20,
                    Component.literal(styleOptions[i]),
                    b -> { NebulaConfig.buttonStyle = bs; NebulaConfig.save(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            styleBtn.setSelected(selected);
            addContent(styleBtn);
            y += 22;
        }
        return y;
    }

    private int boolRow(int left, int y, int width, String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        String onLabel = TranslationManager.get("confi.state.on", "ON");
        String offLabel = TranslationManager.get("confi.state.off", "OFF");
        String state = value ? " [" + onLabel + "]" : " [" + offLabel + "]";
        CompactButton btn = new CompactButton(left + 6, y, width - 12, 20,
                Component.literal(label + state),
                b -> setter.accept(!value),
                CompactButton.Type.MODULE);
        btn.setActivated(value);
        addContent(btn);
        return y + 22;
    }

    private int renderEditionColorSettings(int left, int y, int width) {
        y = sectionHeader(left, y, width,
                TranslationManager.get("confi.edition.title", "EDICIÓN Colors"),
                TranslationManager.get("confi.edition.desc", "Customize EDICION style colors and transparency."));
        y = colorSettingRow(left, y, width, "confi.edition.module_bg", "Module background", NebulaConfig.editionModuleBgColor, c -> NebulaConfig.editionModuleBgColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.module_bg_hover", "Module hover", NebulaConfig.editionModuleBgHoverColor, c -> NebulaConfig.editionModuleBgHoverColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.module_active", "Module active", NebulaConfig.editionModuleActiveColor, c -> NebulaConfig.editionModuleActiveColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.module_inactive", "Module indicator inactive", NebulaConfig.editionModuleInactiveColor, c -> NebulaConfig.editionModuleInactiveColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.border", "Borders", NebulaConfig.editionBorderColor, c -> NebulaConfig.editionBorderColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.text", "Text", NebulaConfig.editionTextColor, c -> NebulaConfig.editionTextColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.button", "Buttons", NebulaConfig.editionButtonColor, c -> NebulaConfig.editionButtonColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.button_hover", "Button hover", NebulaConfig.editionButtonHoverColor, c -> NebulaConfig.editionButtonHoverColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.indicator", "Indicators / accent", NebulaConfig.editionIndicatorColor, c -> NebulaConfig.editionIndicatorColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.divider", "Divider lines", NebulaConfig.editionDividerColor, c -> NebulaConfig.editionDividerColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.panel_bg", "Panel background", NebulaConfig.editionPanelBgColor, c -> NebulaConfig.editionPanelBgColor = c);
        y = colorSettingRow(left, y, width, "confi.edition.topbar_bg", "Top bar background", NebulaConfig.editionTopBarBgColor, c -> NebulaConfig.editionTopBarBgColor = c);
        return y + 4;
    }

    private int colorSettingRow(int left, int y, int width, String key, String fallback, int color, java.util.function.IntConsumer setter) {
        String label = TranslationManager.get(key, fallback);
        CompactButton lbl = new CompactButton(left + 6, y, width - 12, 14, Component.literal(label), b -> {}, CompactButton.Type.LABEL);
        lbl.active = false;
        addContent(lbl);
        y += 16;
        ColorInputButton input = new ColorInputButton(left + 6, y, width - 12, 20, color, c -> { setter.accept(c); NebulaConfig.save(); });
        addContent(input);
        return y + 24;
    }

    private int addModuleRow(int left, int y, int width, Module module) {
        boolean active = module.isActive();
        String onLabel = TranslationManager.get("confi.state.on", "ON");
        String offLabel = TranslationManager.get("confi.state.off", "OFF");
        String state = active ? " [" + onLabel + "]" : " [" + offLabel + "]";
        String dispName = TranslationManager.getModuleName(module.name);
        CompactButton modBtn = new CompactButton(left, y, width, rowHModule(),
                Component.literal(dispName + state),
                b -> { module.toggle(); rebuild(); },
                CompactButton.Type.MODULE) {
            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                if (this.active && this.visible && event.button() == 1 && this.isMouseOver(event.x(), event.y())) {
                    if (module.settings != null && !module.settings.groups.isEmpty()) {
                        var screen = GuiThemes.get().moduleScreen(module);
                        Minecraft.getInstance().setScreen((Screen) screen);
                    }
                    return true;
                }
                return super.mouseClicked(event, doubleClick);
            }
        };
        modBtn.setActivated(active);
        addContent(modBtn);
        return y + rowHModule() + (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 1 : 2);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (languageDropdownOpen) languageDropdownOpen = false;
        if (maxScroll > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - deltaY * 18));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        AbstractWidget newHovered = null;
        for (AbstractWidget btn : contentButtons) {
            if (btn.visible && btn.isMouseOver(mouseX, mouseY)) { newHovered = btn; break; }
        }
        if (newHovered != hoveredWidget) {
            hoveredWidget = newHovered;
            hoverStartTime = System.currentTimeMillis();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (handleHudmcMouseClicked(event, doubleClick)) return true;
        if (currentTab == Tab.HUD && event.button() == 0) {
            HudmcPanelState p = hudmcManager.panelAt(mx, my);
            if (p != null && p.activeTab == HudmcPanelState.TabType.CHAT) hudmcFocusedChatPanelId = p.id;
        }
        if (languageDropdownOpen) {
            int dropW = (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 62 : 72), dropX = this.width - dropW - 8, dropY = topBarH() - 2, itemH = (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 16 : 18);
            if (languageButton != null && languageButton.isMouseOver(mx, my)) return super.mouseClicked(event, doubleClick);
            boolean insideDropdown = mx >= dropX && mx <= dropX + dropW && my >= dropY && my <= dropY + itemH * 2;
            if (insideDropdown) {
                if (my < dropY + itemH) changeLanguage(LanguageManager.Language.ES_MX);
                else changeLanguage(LanguageManager.Language.EN_US);
                return true;
            }
            languageDropdownOpen = false;
        }
        if (event.button() == 0 && NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION) {
            String panelId = panelIdAt(mx, my);
            if (panelId != null && isOnPanelHeader(panelId, mx, my)) {
                PanelRect r = panelRectById(panelId);
                int px = r != null ? r.x : panelX(panelId);
                int py = r != null ? r.y : panelY(panelId);
                draggingPanelId = panelId;
                dragOffX = (int) mx - px;
                dragOffY = (int) my - py;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }


    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (hudmcResizing != null && event.button() == 0) {
            HudmcPanelState p = hudmcResizing;
            p.maximized = false; p.minimized = false;
            p.width = clamp(hudmcResizeStartW + ((int) event.x() - hudmcResizeStartMouseX), 220, Math.max(220, this.width - p.x - 8));
            p.height = clamp(hudmcResizeStartH + ((int) event.y() - hudmcResizeStartMouseY), 120, Math.max(120, this.height - p.y - 8));
            p.captureRestore(); p.clampToScreen(this.width, this.height); syncHudmcToConfig(); rebuild(); return true;
        }
        if (hudmcDragging != null && event.button() == 0) {
            HudmcPanelState p = hudmcDragging;
            if (p.maximized) hudmcRenderer.restore(p, this.width, this.height);
            int h = p.minimized ? HudmcPanelRenderer.HEADER_H : p.height;
            p.x = clamp((int) event.x() - hudmcDragOffX, 4, Math.max(4, this.width - p.width - 4));
            p.y = clamp((int) event.y() - hudmcDragOffY, topBarH() + 2, Math.max(topBarH() + 2, this.height - h - 4));
            p.captureRestore(); syncHudmcToConfig(); rebuild(); return true;
        }
        if (draggingPanelId != null && event.button() == 0) {
            int pw = panelW();
            int ph = panelH(draggingPanelId);
            int nx = clamp((int) event.x() - dragOffX, 4, Math.max(4, this.width - pw - 4));
            int ny = clamp((int) event.y() - dragOffY, topBarH() + 2, Math.max(topBarH() + 2, this.height - ph - 4));
            NebulaConfig.dropdownPositions.put(draggingPanelId, new int[]{nx, ny});
            NebulaConfig.save(); rebuild(); return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (hudmcResizing != null && event.button() == 0) { hudmcResizing.captureRestore(); hudmcResizing = null; syncHudmcToConfig(); rebuild(); return true; }
        if (hudmcDragging != null && event.button() == 0) { hudmcDragging.captureRestore(); hudmcDragging = null; syncHudmcToConfig(); return true; }
        if (draggingPanelId != null && event.button() == 0) { draggingPanelId = null; NebulaConfig.save(); return true; }
        return super.mouseReleased(event);
    }

    private void changeLanguage(LanguageManager.Language lang) {
        LanguageManager.set(lang);
        TranslationManager.reload();
        languageDropdownOpen = false;
        clearWidgets();
        init();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (waitingForKeybind) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKeybind = false;
                rebuild();
                return true;
            }
            NebulaConfig.openKeyCode = key;
            NebulaConfig.save();
            NebulaKeybinds.updateKeyBinding();
            waitingForKeybind = false;
            rebuild();
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (currentTab == Tab.HUD && handleHudmcChatKey(event)) return true;
        if (key == NebulaConfig.openKeyCode && NebulaConfig.closeWithOpenKey) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Custom background rendered in render() to control layering.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderCustomBackground(g);
        if (NebulaConfig.uiStyle != NebulaConfig.UIStyle.EDICION || (currentTab != Tab.NEBULA && currentTab != Tab.METEOR)) {
            renderMainPanel(g);
        } else {
            renderEditionPanels(g);
        }
        renderTopBar(g);

        renderHudmcPanels(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, pt);

        renderTooltip(g, mouseX, mouseY);
        if (languageDropdownOpen) renderLanguageDropdown(g, mouseX, mouseY);
    }


    private void renderCustomBackground(GuiGraphics g) {
        if (ConfiColorsConfig.fondoMode == ConfiColorsConfig.FondoMode.COLOR) {
            g.fill(0, 0, this.width, this.height, ConfiColorsConfig.fondoColor);
        } else if (ConfiColorsConfig.fondoMode == ConfiColorsConfig.FondoMode.IMAGE) {
            if (ConfiColorsConfig.fondoImageBase64 != null && !ConfiColorsConfig.fondoImageBase64.isEmpty()) {
                Identifier texture = NebulaTextureManager.loadTextureFromBase64("gui_bg", ConfiColorsConfig.fondoImageBase64);
                if (texture != null) {
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
                    return;
                }
            }
            if (ConfiColorsConfig.fondoImagePath != null && !ConfiColorsConfig.fondoImagePath.isEmpty()) {
                Identifier texture = NebulaTextureManager.loadTextureFromFile("gui_bg_path", ConfiColorsConfig.fondoImagePath);
                if (texture != null) {
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
                    return;
                }
            }
            g.fill(0, 0, this.width, this.height, 0xFF0B0D12);
        } else {
            g.fill(0, 0, this.width, this.height, 0xCC080A0F);
        }
    }

    private void renderMainPanel(GuiGraphics g) {
        int panelW = Math.min(panelWMax(), this.width - 24);
        int panelLeft = (this.width - panelW) / 2;
        int panelTop = contentTop();
        int panelH = this.height - contentTop() - footerPad();
        boolean ed = NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION;

        int bg;
        if (ed) {
            bg = NebulaConfig.editionPanelBgColor;
        } else {
            bg = ConfiColorsConfig.panelCargasColor;
            if (!ConfiColorsConfig.panelCargasVisible) bg = 0xD0141822;
            if (ConfiColorsConfig.panelCargasTransparent) bg = (bg & 0x00FFFFFF) | 0x80000000;
        }
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, bg);

        int border = ed ? NebulaConfig.editionBorderColor : 0xFF2D3342;
        int accent = ed ? NebulaConfig.editionIndicatorColor : 0xFF8B5CF6;
        g.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + 1, accent);
        g.fill(panelLeft, panelTop + panelH - 1, panelLeft + panelW, panelTop + panelH, border);
        g.fill(panelLeft, panelTop, panelLeft + 1, panelTop + panelH, border);
        g.fill(panelLeft + panelW - 1, panelTop, panelLeft + panelW, panelTop + panelH, border);

        if (maxScroll > 0) {
            int trackX = panelLeft + panelW - 4;
            int trackTop = panelTop + 4;
            int trackBottom = panelTop + panelH - 4;
            int trackH = Math.max(1, trackBottom - trackTop);
            g.fill(trackX, trackTop, trackX + 2, trackBottom, ed ? NebulaConfig.editionDividerColor : 0xFF202633);
            int knobH = Math.max(18, trackH * trackH / Math.max(trackH, trackH + maxScroll));
            int knobY = trackTop + (int) ((trackH - knobH) * (scrollOffset / (double) maxScroll));
            g.fill(trackX, knobY, trackX + 2, knobY + knobH, accent);
        }
    }

    private void renderTopBar(GuiGraphics g) {
        boolean ed = NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION;
        int h = topBarH();
        int bg = ed ? NebulaConfig.editionTopBarBgColor : 0xF010131A;
        int line = ed ? NebulaConfig.editionDividerColor : 0xFF2A3140;
        g.fill(0, 0, this.width, h, bg);
        g.fill(0, h - 1, this.width, h, line);
    }

    private void renderLanguageDropdown(GuiGraphics g, int mouseX, int mouseY) {
        int dropW = (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 62 : 72);
        int dropX = this.width - dropW - 8;
        int dropY = topBarH() - 1;
        int itemH = (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? 14 : 16);
        int dropH = itemH * 2;

        g.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF0181C26);
        g.fill(dropX, dropY, dropX + dropW, dropY + 1, 0xFF8B5CF6);
        g.fill(dropX, dropY + dropH - 1, dropX + dropW, dropY + dropH, 0xFF2D3342);
        g.fill(dropX, dropY, dropX + 1, dropY + dropH, 0xFF2D3342);
        g.fill(dropX + dropW - 1, dropY, dropX + dropW, dropY + dropH, 0xFF2D3342);

        String[] items = {"ES_MX", "EN_US"};
        for (int i = 0; i < items.length; i++) {
            int itemY = dropY + i * itemH;
            boolean hovered = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= itemY && mouseY < itemY + itemH;
            if (hovered) g.fill(dropX + 1, itemY, dropX + dropW - 1, itemY + itemH, 0xFF262C3A);
            g.drawString(Minecraft.getInstance().font, items[i], dropX + 8, itemY + 5, hovered ? 0xFFFFFFFF : 0xFFD1D5DB, false);
        }
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredWidget == null || !hoveredWidget.visible) return;
        if (System.currentTimeMillis() - hoverStartTime < TOOLTIP_DELAY_MS) return;
        if (!(hoveredWidget instanceof CompactButton btn) || !btn.hasTooltip()) return;

        Font font = Minecraft.getInstance().font;
        String title = btn.getTooltipTitle();
        String desc = btn.getTooltipDesc();
        int padding = 6;
        int w = Math.max(font.width(title), font.width(desc)) + padding * 2;
        int h = (desc.isEmpty() ? 12 : 24) + padding * 2;
        int tx = mouseX + 10;
        int ty = mouseY - h - 8;
        if (tx + w > this.width - 4) tx = mouseX - w - 10;
        if (ty < 4) ty = mouseY + 12;

        g.fill(tx, ty, tx + w, ty + h, 0xF010131A);
        g.fill(tx, ty, tx + w, ty + 1, 0xFF8B5CF6);
        g.fill(tx, ty + h - 1, tx + w, ty + h, 0xFF2D3342);
        g.fill(tx, ty, tx + 1, ty + h, 0xFF2D3342);
        g.fill(tx + w - 1, ty, tx + w, ty + h, 0xFF2D3342);
        g.drawString(font, title, tx + padding, ty + padding, 0xFFFFFFFF, false);
        if (!desc.isEmpty()) g.drawString(font, desc, tx + padding, ty + padding + 12, 0xFFB0BEC5, false);
    }


    private int panelW() { return NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? Math.min(340, Math.max(220, (int)(this.width * 0.34))) : Math.min(panelWMax(), this.width - 24); }
    private int panelDefaultX() { int w = panelW(); return Math.max(8, (this.width - w) / 2); }
    private int panelDefaultY(String id) { return contentTop() + 8; }
    private int panelX(String id) { return panelX(id, panelDefaultX()); }
    private int panelX(String id, int fallbackX) {
        int[] p = NebulaConfig.dropdownPositions.get(id);
        int x = p != null ? p[0] : fallbackX;
        return clamp(x, 4, Math.max(4, this.width - panelW() - 4));
    }
    private int panelY(String id) { return panelY(id, panelDefaultY(id)); }
    private int panelY(String id, int fallbackY) {
        int[] p = NebulaConfig.dropdownPositions.get(id);
        int y = p != null ? p[1] : fallbackY;
        return clamp(y, topBarH() + 2, Math.max(topBarH() + 2, this.height - panelH(id, 0) - 4));
    }
    private int panelH(String id) { return panelH(id, -1); }
    private int panelH(String id, int moduleCountHint) {
        boolean expanded = NebulaConfig.dropdownExpanded.getOrDefault(id, id.equals("nebula_root") || id.startsWith("nebula.cat."));
        if (!expanded) return 22;
        int content = panelContentHeight(id, moduleCountHint);
        return Math.min(this.height - contentTop() - 12, Math.max(44, content + 26));
    }
    private int panelContentHeight(String id, int moduleCountHint) {
        if ("nebula_root".equals(id)) {
            int h = 0;
            for (NebulaModules.CDJCategory category : NebulaModules.CDJ_CATEGORIES) {
                h += rowHCategory() + 1;
                String catKey = "nebula.cat." + category.getId();
                boolean expanded = NebulaConfig.dropdownExpanded.getOrDefault(catKey, categoryExpandedStates.getOrDefault(catKey, true));
                if (expanded) h += category.getModules().size() * (rowHModule() + 1) + 2;
            }
            return h + 6;
        }
        if (moduleCountHint >= 0) return moduleCountHint * (rowHModule() + 1) + 4;
        return 120;
    }
    private PanelRect panelRectById(String id) {
        for (PanelRect p : panelRects) if (p.id.equals(id)) return p;
        return null;
    }
    private String panelIdAt(double mx, double my) {
        if (NebulaConfig.uiStyle != NebulaConfig.UIStyle.EDICION) return null;
        for (int i = panelRects.size() - 1; i >= 0; i--) {
            PanelRect p = panelRects.get(i);
            if (mx >= p.x && mx <= p.x + p.w && my >= p.y && my <= p.y + p.h) return p.id;
        }
        return null;
    }
    private boolean isOnPanelHeader(String id, double mx, double my) {
        for (PanelRect p : panelRects) {
            if (p.id.equals(id)) return mx >= p.x && mx <= p.x + p.w && my >= p.y && my <= p.y + 20;
        }
        return false;
    }
    private void drawDraggablePanel(String id, int x, int y, int w, int h, String title, boolean expanded, Runnable toggle) {
        panelRects.add(new PanelRect(id, x, y, w, h, expanded));
        CompactButton headerBtn = new CompactButton(x + 2, y + 2, w - 4, 16,
                Component.literal((expanded ? "v " : "> ") + title),
                b -> toggle.run(),
                CompactButton.Type.MASTER_HEADER);
        addContent(headerBtn);
    }

    private void renderEditionPanels(GuiGraphics g) {
        for (PanelRect p : panelRects) {
            int bg = NebulaConfig.editionPanelBgColor;
            int border = NebulaConfig.editionBorderColor;
            int accent = NebulaConfig.editionIndicatorColor;
            int headerBg = NebulaConfig.editionButtonColor;
            g.fill(p.x, p.y, p.x + p.w, p.y + p.h, bg);
            g.fill(p.x, p.y, p.x + p.w, p.y + 20, headerBg);
            g.fill(p.x, p.y, p.x + p.w, p.y + 1, accent);
            g.fill(p.x, p.y + p.h - 1, p.x + p.w, p.y + p.h, border);
            g.fill(p.x, p.y, p.x + 1, p.y + p.h, border);
            g.fill(p.x + p.w - 1, p.y, p.x + p.w, p.y + p.h, border);
            g.fill(p.x, p.y + 20, p.x + p.w, p.y + 21, NebulaConfig.editionDividerColor);
        }
    }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private void openFileChooser(java.util.function.Consumer<String> callback) {
        ImageFileUtil.openImagePicker(
                TranslationManager.get("gui.nebula.hud.image.select", "Seleccionar imagen PNG/JPG"),
                true,
                callback,
                ImageFileUtil::notifyClient
        );
    }

    private void openImageInViewer(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException ignored) {
        }
    }

    private String shortenPath(String path) {
        if (path == null || path.isEmpty()) return "";
        return new File(path).getName();
    }

    @Override
    public void onClose() {
        NebulaConfig.save();
        ConfiColorsConfig.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
