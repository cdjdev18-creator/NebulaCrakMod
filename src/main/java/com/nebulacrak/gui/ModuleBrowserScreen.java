package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ModuleBrowserScreen extends Screen {
    private final Screen parent;
    private Category selectedCategory;
    private Module selectedModule;
    private EditBox searchBox;

    private final List<Category> categories = new ArrayList<>();
    private final List<Module> filteredModules = new ArrayList<>();
    private final List<SettingRow> settingRows = new ArrayList<>();

    private double catScroll, modScroll, setScroll;

    private StringSetting editingStringSetting;
    private EditBox stringSettingBox;
    private KeybindSetting capturingKeybind;

    private static final int PAD = 10;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 28;
    private static final int ROW_H = 20;
    private static final int GAP = 6;

    public ModuleBrowserScreen(Screen parent) {
        super(Component.literal("NebulaCrakMod - Module Browser"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshCategories();
        if (selectedCategory == null && !categories.isEmpty()) selectedCategory = categories.get(0);
        if (selectedCategory != null && !categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? null : categories.get(0);
        }

        int searchW = Math.min(260, Math.max(120, this.width / 4));
        int searchX = leftPanelW() + PAD + 4;
        if (searchBox == null) {
            searchBox = new EditBox(this.font, searchX, 8, searchW, 18, Component.literal("Search"));
            searchBox.setMaxLength(64);
            searchBox.setResponder(v -> refreshFilteredModules());
        } else {
            searchBox.setX(searchX);
            searchBox.setY(8);
            searchBox.setWidth(searchW);
            searchBox.setHeight(18);
        }
        this.addRenderableWidget(searchBox);

        refreshFilteredModules();
        rebuildSettingRows();
        clampScrolls();
    }

    @Override
    public void resize(int width, int height) {
        String q = searchBox != null ? searchBox.getValue() : "";
        super.resize(width, height);
        if (searchBox != null) searchBox.setValue(q);
    }

    private void refreshCategories() {
        categories.clear();
        try {
            for (Category c : Modules.loopCategories()) {
                if (c != null) categories.add(c);
            }
        } catch (Throwable ignored) {}
        categories.sort(Comparator.comparing(c -> c.name == null ? "" : c.name.toLowerCase(Locale.ROOT)));
    }

    private void refreshFilteredModules() {
        filteredModules.clear();
        if (selectedCategory == null) {
            selectedModule = null;
            rebuildSettingRows();
            return;
        }
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        try {
            List<Module> mods = Modules.get().getGroup(selectedCategory);
            if (mods != null) {
                for (Module m : mods) {
                    if (m == null) continue;
                    String name = m.name == null ? "" : m.name.toLowerCase(Locale.ROOT);
                    String title = m.title == null ? "" : m.title.toLowerCase(Locale.ROOT);
                    if (q.isEmpty() || name.contains(q) || title.contains(q)) filteredModules.add(m);
                }
            }
        } catch (Throwable ignored) {}
        filteredModules.sort(Comparator.comparing(m -> (m.title != null ? m.title : m.name).toLowerCase(Locale.ROOT)));
        if (selectedModule != null) {
            boolean stillInCategory = false;
            try {
                List<Module> mods = Modules.get().getGroup(selectedCategory);
                stillInCategory = mods != null && mods.contains(selectedModule);
            } catch (Throwable ignored) {}
            if (!stillInCategory) {
                selectedModule = null;
                rebuildSettingRows();
            }
        }
        clampScrolls();
    }

    private void rebuildSettingRows() {
        settingRows.clear();
        editingStringSetting = null;
        capturingKeybind = null;
        if (stringSettingBox != null) {
            this.removeWidget(stringSettingBox);
            stringSettingBox = null;
        }
        if (selectedModule == null) return;
        try {
            for (SettingGroup sg : selectedModule.settings.groups) {
                if (sg == null) continue;
                for (Setting<?> setting : sg) {
                    if (setting == null) continue;
                    try { if (!setting.isVisible()) continue; } catch (Throwable ignored) {}
                    settingRows.add(new SettingRow(setting));
                }
            }
        } catch (Throwable ignored) {}
        clampScrolls();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refreshCategories();
        if (selectedCategory != null && !categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? null : categories.get(0);
            selectedModule = null;
            refreshFilteredModules();
        }

        this.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0101018);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        g.drawString(this.font, "Search:", leftPanelW() + PAD, 13, 0xFFB0B0C0, false);

        Layout l = layout();
        drawPanel(g, l.catX, l.top, l.catW, l.panelH, "Categories");
        drawPanel(g, l.modX, l.top, l.modW, l.panelH, "Modules");
        drawPanel(g, l.setX, l.top, l.setW, l.panelH, "Settings");

        renderCategories(g, l, mouseX, mouseY);
        renderModules(g, l, mouseX, mouseY);
        renderSettings(g, l, mouseX, mouseY);

        String hint = capturingKeybind != null ? "Keybind: press key to bind, ESC clear, BACKSPACE cancel"
                : "Click row: select | ON/OFF: toggle | Scroll panels independently";
        g.drawCenteredString(this.font, hint, this.width / 2, this.height - 18, 0xFF9090A8);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h, String title) {
        int bg = 0xCC171721;
        int border = 0xFF323246;
        g.fill(x, y, x + w, y + h, bg);
        drawBorder(g, x, y, w, h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + 18, 0xFF202032);
        g.drawString(this.font, title, x + 6, y + 5, 0xFFE0E0F0, false);
    }

    private void renderCategories(GuiGraphics g, Layout l, int mx, int my) {
        int cx = l.catX + 4, cy = l.top + 22, cw = l.catW - 8, ch = l.panelH - 26;
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        int y = cy - (int) catScroll;
        for (Category c : categories) {
            int count = safeGroupCount(c);
            String label = c.name + " (" + count + ")";
            boolean sel = c == selectedCategory;
            boolean hov = mx >= cx && mx < cx + cw && my >= y && my < y + ROW_H && inside(mx, my, cx, cy, cw, ch);
            int bg = sel ? 0xFF3B2D66 : (hov ? 0xFF2A2A3A : 0xFF1D1D29);
            int bd = sel ? 0xFF8B5CF6 : 0xFF353548;
            g.fill(cx, y, cx + cw, y + ROW_H - 2, bg);
            drawBorder(g, cx, y, cw, ROW_H - 2, bd);
            g.drawString(this.font, trim(label, cw - 8), cx + 4, y + 5, sel ? 0xFFFFFFFF : 0xFFD0D0DC, false);
            y += ROW_H;
        }
        g.disableScissor();
        drawScrollbar(g, l.catX + l.catW - 4, cy, 2, ch, catScroll, categories.size() * ROW_H, ch);
    }

    private void renderModules(GuiGraphics g, Layout l, int mx, int my) {
        int cx = l.modX + 4, cy = l.top + 22, cw = l.modW - 8, ch = l.panelH - 26;
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        if (filteredModules.isEmpty()) {
            String msg = (searchBox != null && !searchBox.getValue().trim().isEmpty())
                    ? "No se encontraron módulos" : "Categoría vacía";
            g.drawString(this.font, msg, cx + 4, cy + 8, 0xFF9999AA, false);
            g.disableScissor();
            return;
        }
        int y = cy - (int) modScroll;
        for (Module m : filteredModules) {
            boolean sel = m == selectedModule;
            boolean hov = mx >= cx && mx < cx + cw && my >= y && my < y + ROW_H && inside(mx, my, cx, cy, cw, ch);
            int bg = sel ? 0xFF2E2852 : (hov ? 0xFF242436 : 0xFF1B1B27);
            int bd = sel ? 0xFF8B5CF6 : 0xFF303044;
            g.fill(cx, y, cx + cw, y + ROW_H - 2, bg);
            drawBorder(g, cx, y, cw, ROW_H - 2, bd);

            boolean active = safeIsActive(m);
            int toggleW = 34;
            int tx = cx + cw - toggleW - 3;
            int ty = y + 2;
            int tbg = active ? 0xFF2E7D32 : 0xFF5A2A2A;
            int tbd = active ? 0xFF66BB6A : 0xFFAA5555;
            g.fill(tx, ty, tx + toggleW, ty + 14, tbg);
            drawBorder(g, tx, ty, toggleW, 14, tbd);
            String tlabel = active ? "ON" : "OFF";
            g.drawString(this.font, tlabel, tx + (toggleW - this.font.width(tlabel)) / 2, ty + 3, 0xFFFFFFFF, false);

            String name = trim(m.title != null ? m.title : m.name, cw - toggleW - 14);
            g.drawString(this.font, name, cx + 4, y + 5, active ? 0xFFEAEAF5 : 0xFFC0C0CC, false);
            y += ROW_H;
        }
        g.disableScissor();
        drawScrollbar(g, l.modX + l.modW - 4, cy, 2, ch, modScroll, filteredModules.size() * ROW_H, ch);
    }

    private void renderSettings(GuiGraphics g, Layout l, int mx, int my) {
        int cx = l.setX + 4, cy = l.top + 22, cw = l.setW - 8, ch = l.panelH - 26;
        if (selectedModule == null) {
            g.drawString(this.font, "Selecciona un módulo", cx, cy + 4, 0xFF9999AA, false);
            return;
        }

        String mName = selectedModule.title != null ? selectedModule.title : selectedModule.name;
        g.drawString(this.font, trim(mName, cw - 70), cx, cy, 0xFFFFFFFF, false);
        boolean active = safeIsActive(selectedModule);
        String st = active ? "ON" : "OFF";
        g.drawString(this.font, st, cx + cw - this.font.width(st) - 2, cy, active ? 0xFF66BB6A : 0xFFEF5350, false);
        String desc = selectedModule.description == null ? "" : selectedModule.description;
        if (!desc.isEmpty()) g.drawString(this.font, trim(desc, cw), cx, cy + 11, 0xFF9A9AB0, false);

        int listY = cy + 26;
        int listH = ch - 26;
        if (listH <= 0) return;
        g.enableScissor(cx, listY, cx + cw, listY + listH);
        int y = listY - (int) setScroll;
        if (settingRows.isEmpty()) g.drawString(this.font, "Sin configuraciones visibles", cx, y, 0xFF9999AA, false);
        for (SettingRow row : settingRows) {
            int rh = rowHeight();
            boolean hov = mx >= cx && mx < cx + cw && my >= y && my < y + rh - 2 && inside(mx, my, cx, listY, cw, listH);
            g.fill(cx, y, cx + cw, y + rh - 2, hov ? 0xFF232334 : 0xFF1C1C28);
            drawBorder(g, cx, y, cw, rh - 2, 0xFF333347);
            renderSettingRow(g, row, cx + 4, y + 3, cw - 8);
            y += rh;
        }
        g.disableScissor();
        drawScrollbar(g, l.setX + l.setW - 4, listY, 2, listH, setScroll, totalSettingsHeight(), listH);

        if (stringSettingBox != null && editingStringSetting != null) positionStringEditor(l, cx, listY, cw, listH);
    }

    private void renderSettingRow(GuiGraphics g, SettingRow row, int x, int y, int w) {
        Setting<?> s = row.setting;
        String name = trim(s.title != null ? s.title : s.name, w / 2);
        g.drawString(this.font, name, x, y + 2, 0xFFD8D8E6, false);
        int ctrlX = x + w / 2;
        int ctrlW = w - w / 2;
        try {
            if (s instanceof BoolSetting b) {
                drawToggle(g, ctrlX + ctrlW - 36, y, 34, 14, b.get());
            } else if (s instanceof IntSetting i) {
                drawValueBox(g, ctrlX, y, ctrlW, "[-] " + i.get() + " [+]", trim(i.min + ".." + i.max, ctrlW - 6));
            } else if (s instanceof DoubleSetting d) {
                drawValueBox(g, ctrlX, y, ctrlW, "[-] " + formatDouble(d.get(), d.decimalPlaces) + " [+]", null);
            } else if (s instanceof EnumSetting<?> e) {
                String val = e.get() == null ? "null" : ((Enum<?>) e.get()).name();
                drawValueBox(g, ctrlX, y, ctrlW, "< " + trim(val, Math.max(10, ctrlW - 14)) + " >", null);
            } else if (s instanceof StringSetting ss) {
                if (editingStringSetting == ss && stringSettingBox != null) {
                    g.drawString(this.font, "Editing...", ctrlX, y + 2, 0xFF8BB0FF, false);
                } else {
                    String v = ss.get() == null ? "" : ss.get();
                    drawValueBox(g, ctrlX, y, ctrlW, trim(v.isEmpty() ? "(empty)" : v, ctrlW - 6), null);
                }
            } else if (s instanceof ColorSetting cs) {
                SettingColor c = cs.get();
                int argb = c == null ? 0xFFFFFFFF : ((c.a & 0xFF) << 24) | ((c.r & 0xFF) << 16) | ((c.g & 0xFF) << 8) | (c.b & 0xFF);
                drawColorControl(g, ctrlX, y, ctrlW, argb, c != null && c.rainbow);
            } else if (s instanceof KeybindSetting ks) {
                Keybind kb = ks.get();
                String label = capturingKeybind == ks ? "..." : (kb == null || !kb.isSet() ? "None" : trim(kb.toString(), ctrlW - 6));
                drawValueBox(g, ctrlX, y, ctrlW, label, capturingKeybind == ks ? "capturing" : null);
            } else {
                g.drawString(this.font, trim("No soportado: " + s.getClass().getSimpleName(), ctrlW), ctrlX, y + 2, 0xFFAA8866, false);
            }
        } catch (Throwable t) {
            g.drawString(this.font, trim("Error al leer", ctrlW), ctrlX, y + 2, 0xFFEF5350, false);
        }
    }

    private void drawToggle(GuiGraphics g, int x, int y, int w, int h, boolean on) {
        int bg = on ? 0xFF2E7D32 : 0xFF555555;
        g.fill(x, y, x + w, y + h, bg);
        drawBorder(g, x, y, w, h, on ? 0xFF66BB6A : 0xFF777777);
        String t = on ? "ON" : "OFF";
        g.drawString(this.font, t, x + (w - this.font.width(t)) / 2, y + 3, 0xFFFFFFFF, false);
    }

    private void drawValueBox(GuiGraphics g, int x, int y, int w, String value, String hint) {
        g.fill(x, y, x + w, y + 16, 0xFF252536);
        drawBorder(g, x, y, w, 16, 0xFF45455A);
        g.drawString(this.font, trim(value, w - 6), x + 3, y + 4, 0xFFE6E6F0, false);
    }

    private void drawColorControl(GuiGraphics g, int x, int y, int w, int argb, boolean rainbow) {
        int sw = 16;
        g.fill(x, y, x + sw, y + sw, argb);
        drawBorder(g, x, y, sw, sw, 0xFF777788);
        String hex = String.format("#%02X%02X%02X%02X", (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        String label = rainbow ? "RB " + hex : hex;
        g.drawString(this.font, trim(label, w - sw - 6), x + sw + 4, y + 4, 0xFFE0E0EC, false);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout();
        double delta = -verticalAmount * 18;
        if (inside(mouseX, mouseY, l.catX, l.top + 22, l.catW, l.panelH - 26)) {
            catScroll = clampScroll(catScroll + delta, categories.size() * ROW_H, l.panelH - 26);
            return true;
        }
        if (inside(mouseX, mouseY, l.modX, l.top + 22, l.modW, l.panelH - 26)) {
            modScroll = clampScroll(modScroll + delta, filteredModules.size() * ROW_H, l.panelH - 26);
            return true;
        }
        int setListY = l.top + 22 + 26;
        int setListH = l.panelH - 26 - 26;
        if (inside(mouseX, mouseY, l.setX, setListY, l.setW, setListH)) {
            setScroll = clampScroll(setScroll + delta, totalSettingsHeight(), setListH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (capturingKeybind != null) {
            try {
                Keybind n = Keybind.fromButton(button);
                capturingKeybind.set(n);
            } catch (Throwable ignored) {}
            capturingKeybind = null;
            return true;
        }

        if (searchBox != null && searchBox.mouseClicked(event, doubleClick)) {
            this.setFocused(searchBox);
            return true;
        }
        if (stringSettingBox != null && stringSettingBox.visible && stringSettingBox.mouseClicked(event, doubleClick)) {
            this.setFocused(stringSettingBox);
            return true;
        }

        Layout l = layout();
        int cx = l.catX + 4, cy = l.top + 22, cw = l.catW - 8, ch = l.panelH - 26;
        if (inside(mx, my, cx, cy, cw, ch)) {
            int y = cy - (int) catScroll;
            for (Category c : categories) {
                if (inside(mx, my, cx, y, cw, ROW_H - 2)) {
                    if (selectedCategory != c) {
                        selectedCategory = c;
                        selectedModule = null;
                        modScroll = 0;
                        setScroll = 0;
                        refreshFilteredModules();
                    }
                    return true;
                }
                y += ROW_H;
            }
        }

        int mxp = l.modX + 4, myp = l.top + 22, mw = l.modW - 8, mh = l.panelH - 26;
        if (inside(mx, my, mxp, myp, mw, mh)) {
            int y = myp - (int) modScroll;
            for (Module m : filteredModules) {
                if (inside(mx, my, mxp, y, mw, ROW_H - 2)) {
                    int toggleW = 34;
                    int tx = mxp + mw - toggleW - 3;
                    int ty = y + 2;
                    if (inside(mx, my, tx, ty, toggleW, 14)) {
                        try { m.toggle(); } catch (Throwable ignored) {}
                        return true;
                    }
                    if (selectedModule != m) {
                        selectedModule = m;
                        setScroll = 0;
                        rebuildSettingRows();
                    }
                    return true;
                }
                y += ROW_H;
            }
        }

        int sx = l.setX + 4, sy = l.top + 22 + 26, sw = l.setW - 8, sh = l.panelH - 26 - 26;
        if (inside(mx, my, sx, sy, sw, sh)) {
            int y = sy - (int) setScroll;
            for (SettingRow row : settingRows) {
                int rh = rowHeight();
                if (inside(mx, my, sx, y, sw, rh - 2)) {
                    handleSettingClick(row, sx + 4, y + 3, sw - 8, mx, my);
                    return true;
                }
                y += rh;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleSettingClick(SettingRow row, int x, int y, int w, double mx, double my) {
        Setting<?> s = row.setting;
        int ctrlX = x + w / 2;
        int ctrlW = w - w / 2;
        if (mx < ctrlX) return;
        try {
            if (s instanceof BoolSetting b) {
                b.set(!b.get());
            } else if (s instanceof IntSetting i) {
                int step = Minecraft.getInstance().hasShiftDown() ? 10 : 1;
                i.set(i.get() + (mx < ctrlX + ctrlW / 2 ? -step : step));
            } else if (s instanceof DoubleSetting d) {
                double step = Minecraft.getInstance().hasShiftDown() ? 1.0 : 0.1;
                double nv = d.get() + (mx < ctrlX + ctrlW / 2 ? -step : step);
                nv = Math.round(nv * 100000.0) / 100000.0;
                d.set(nv);
            } else if (s instanceof EnumSetting<?> e) {
                cycleEnum(e, mx < ctrlX + ctrlW / 2 ? -1 : 1);
            } else if (s instanceof StringSetting ss) {
                openStringEditor(ss, ctrlX, y, ctrlW);
            } else if (s instanceof ColorSetting cs) {
                openColorEditor(cs);
            } else if (s instanceof KeybindSetting ks) {
                capturingKeybind = ks;
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cycleEnum(EnumSetting<?> setting, int dir) {
        Enum<?> val = (Enum<?>) setting.get();
        if (val == null) return;
        Enum<?>[] constants = val.getDeclaringClass().getEnumConstants();
        if (constants == null || constants.length == 0) return;
        int idx = val.ordinal() + dir;
        if (idx < 0) idx = constants.length - 1;
        if (idx >= constants.length) idx = 0;
        ((EnumSetting) setting).set(constants[idx]);
    }

    private void openStringEditor(StringSetting ss, int x, int y, int w) {
        if (stringSettingBox != null) { this.removeWidget(stringSettingBox); stringSettingBox = null; }
        editingStringSetting = ss;
        stringSettingBox = new EditBox(this.font, x, y, w, 16, Component.literal(ss.name));
        stringSettingBox.setMaxLength(512);
        stringSettingBox.setValue(ss.get() == null ? "" : ss.get());
        stringSettingBox.setResponder(v -> { if (editingStringSetting == ss) { try { ss.set(v); } catch (Throwable ignored) {} } });
        this.addRenderableWidget(stringSettingBox);
        this.setFocused(stringSettingBox);
    }

    private void positionStringEditor(Layout l, int cx, int listY, int cw, int listH) {
        int y = listY - (int) setScroll;
        for (SettingRow row : settingRows) {
            int rh = rowHeight();
            if (row.setting == editingStringSetting) {
                int ctrlX = cx + 4 + (cw - 8) / 2;
                int ctrlW = (cw - 8) - (cw - 8) / 2;
                int boxY = y + 3;
                boolean visible = boxY + 16 > listY && boxY < listY + listH;
                stringSettingBox.visible = visible;
                stringSettingBox.active = visible;
                stringSettingBox.setX(ctrlX);
                stringSettingBox.setY(boxY);
                stringSettingBox.setWidth(ctrlW);
                return;
            }
            y += rh;
        }
        stringSettingBox.visible = false;
        stringSettingBox.active = false;
    }

    private void openColorEditor(ColorSetting cs) {
        SettingColor c = cs.get();
        int initial = c == null ? 0xFFFFFFFF : ((c.a & 0xFF) << 24) | ((c.r & 0xFF) << 16) | ((c.g & 0xFF) << 8) | (c.b & 0xFF);
        Minecraft.getInstance().setScreen(new SimpleColorScreen(this, initial, argb -> {
            SettingColor cur = cs.get();
            if (cur != null) {
                cur.r = (argb >> 16) & 0xFF; cur.g = (argb >> 8) & 0xFF; cur.b = argb & 0xFF; cur.a = (argb >>> 24) & 0xFF;
                cur.rainbow = false; cur.validate();
            }
        }));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingKeybind != null) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) { try { capturingKeybind.set(Keybind.none()); } catch (Throwable ignored) {} capturingKeybind = null; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) { capturingKeybind = null; return true; }
            try { capturingKeybind.set(Keybind.fromKey(key)); } catch (Throwable ignored) {}
            capturingKeybind = null;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchBox != null && searchBox.isFocused()) { searchBox.setFocused(false); this.setFocused(null); return true; }
            onClose(); return true;
        }
        if (this.getFocused() != null && this.getFocused().keyPressed(event)) return true;
        if (searchBox != null && searchBox.isFocused() && searchBox.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (capturingKeybind != null) return true;
        if (this.getFocused() != null && this.getFocused().charTyped(event)) return true;
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    private int leftPanelW() { return Mth.clamp(this.width / 5, 120, 180); }
    private int rightPanelW() { return Mth.clamp(this.width / 3, 220, 360); }

    private Layout layout() {
        int leftW = leftPanelW(), rightW = rightPanelW();
        int top = HEADER_H, panelH = this.height - HEADER_H - FOOTER_H;
        int catX = PAD, catW = leftW, setX = this.width - PAD - rightW, setW = rightW;
        int modX = catX + catW + GAP, modW = setX - GAP - modX;
        if (modW < 160) {
            int deficit = 160 - modW;
            catW = Math.max(90, catW - deficit / 2);
            setW = Math.max(160, setW - (deficit - deficit / 2));
            modX = catX + catW + GAP; setX = this.width - PAD - setW; modW = Math.max(120, setX - GAP - modX);
        }
        return new Layout(catX, catW, modX, modW, setX, setW, top, panelH);
    }

    private int totalSettingsHeight() { int h = 0; for (SettingRow r : settingRows) h += rowHeight(); return Math.max(h, 1); }
    private int rowHeight() { return 24; }
    private void clampScrolls() { Layout l = layout(); catScroll = clampScroll(catScroll, categories.size() * ROW_H, l.panelH - 26); modScroll = clampScroll(modScroll, filteredModules.size() * ROW_H, l.panelH - 26); setScroll = clampScroll(setScroll, totalSettingsHeight(), l.panelH - 52); }
    private static double clampScroll(double v, int content, int view) { double max = Math.max(0, content - view); return Mth.clamp(v, 0, max); }
    private boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c); g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c); }
    private String trim(String s, int maxW) { if (s == null) return ""; if (this.font.width(s) <= maxW) return s; String ell = "..."; int ew = this.font.width(ell); StringBuilder sb = new StringBuilder(); for (int i = 0; i < s.length(); i++) { String next = sb.toString() + s.charAt(i); if (this.font.width(next) + ew > maxW) break; sb.append(s.charAt(i)); } return sb + ell; }
    private void drawScrollbar(GuiGraphics g, int x, int y, int w, int h, double scroll, int contentH, int viewH) { if (contentH <= viewH || viewH <= 0) return; g.fill(x, y, x + w, y + h, 0xFF1A1A24); int thumbH = Math.max(12, (int) (h * (viewH / (double) contentH))); double maxScroll = contentH - viewH; int thumbY = y + (int) ((h - thumbH) * (scroll / maxScroll)); g.fill(x, thumbY, x + w, thumbY + thumbH, 0xFF6A6A86); }
    private int safeGroupCount(Category c) { try { List<Module> m = Modules.get().getGroup(c); return m == null ? 0 : m.size(); } catch (Throwable t) { return 0; } }
    private boolean safeIsActive(Module m) { try { return m.isActive(); } catch (Throwable t) { return false; } }
    private static String formatDouble(double v, int places) { if (places <= 0) return String.valueOf((long) Math.round(v)); String fmt = "%." + Mth.clamp(places, 0, 6) + "f"; return String.format(Locale.ROOT, fmt, v); }

    private record Layout(int catX, int catW, int modX, int modW, int setX, int setW, int top, int panelH) {}
    private static class SettingRow { final Setting<?> setting; SettingRow(Setting<?> s) { this.setting = s; } }

    private static class SimpleColorScreen extends Screen {
        private final Screen parent;
        private final int initial;
        private final java.util.function.IntConsumer onChange;
        private int current;
        protected SimpleColorScreen(Screen parent, int initial, java.util.function.IntConsumer onChange) {
            super(Component.literal("Color")); this.parent = parent; this.initial = initial; this.current = initial; this.onChange = onChange;
        }
        @Override
        protected void init() {
            int w = 220, x = this.width / 2 - w / 2, y = this.height / 2 - 30;
            this.addRenderableWidget(new ColorInputButton(x, y, w, 22, current, c -> { current = c; onChange.accept(c); }));
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x, y + 30, w / 2 - 2, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> { onChange.accept(initial); Minecraft.getInstance().setScreen(parent); }).bounds(x + w / 2 + 2, y + 30, w / 2 - 2, 20).build());
        }
        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(this.font, "Editar color (#RRGGBB / #RRGGBBAA)", this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);
            super.render(g, mouseX, mouseY, partialTick);
        }
        @Override
        public void onClose() { Minecraft.getInstance().setScreen(parent); }
    }
}
