package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.gui.NebulaTextureManager;
import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.MeteorClient;
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
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Pantalla principal de NebulaCrakMod.
 *
 * Pestañas:
 *   [NebulaCrakMod]  — módulos propios, agrupados bajo un único desplegable
 *   [Módulos Meteor] — módulos originales de Meteor por sus categorías reales
 *   [ConfiColors]    — personalización visual (Fondo, PanelCargas, Splash)
 *
 * Firma tricolor en barra superior (UNA sola vez):
 *   "NebulaCrakMod" rojo · "by" blanco · "CDJ dev" verde
 */
public class NebulaGUIScreen extends Screen {

    // ── Tabs ──────────────────────────────────────────────────────────────────
    public enum Tab { NEBULA, METEOR, HUD, EDIT_MC, CONFI }

    private final Screen parent;
    private Tab currentTab = Tab.NEBULA;

    // ── Estado desplegable NebulaCrakMod (menú raíz propio) ──────────────────
    private boolean nebulaRootExpanded = true;

    // ── Selector de idioma ───────────────────────────────────────────────────
    private boolean languageDropdownOpen = false;

    // ── Estados desplegables de categorías (clave → expandido) ──────────────
    private final Map<String, Boolean> categoryExpandedStates = new HashMap<>();

    // ── Widgets ──────────────────────────────────────────────────────────────
    private final List<AbstractWidget> navButtons     = new ArrayList<>();
    private final List<AbstractWidget> contentButtons = new ArrayList<>();
    private CompactButton languageButton;

    // ── Scroll ───────────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    private int maxScroll    = 0;
    private static final int CONTENT_START_Y = 32;

    // ── CONFI tab state ──────────────────────────────────────────────────────
    private boolean waitingForKeybind = false;
    private int pendingKeyCode = -1;

    // ── Tooltip system ────────────────────────────────────────────────────────
    private AbstractWidget hoveredWidget = null;
    private long hoverStartTime = 0;
    private static final long TOOLTIP_DELAY_MS = 2000; // 2 segundos
    private String tooltipTitle = "";
    private String tooltipDesc = "";

    // ── Firma tricolor (calculada en init) ───────────────────────────────────
    private int sigEndX = 0;

    public NebulaGUIScreen(Screen parent) {
        super(Component.literal("NebulaCrakMod"));
        this.parent = parent;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    protected void init() {
        clearWidgets();
        navButtons.clear();
        contentButtons.clear();

        // ── Firma tricolor (dimensiones para layout de tabs) ─────────────────────
        String sigName   = "NebulaCrakMod";
        String sigBy     = " by ";
        String sigAuthor = "CDJ dev";
        int nW = this.font.width(sigName);
        int bW = this.font.width(sigBy);
        int aW = this.font.width(sigAuthor);
        // Con escalado 2.25x, el ancho del autor se multiplica
        float scale = 2.25f;
        sigEndX = 8 + nW + bW + (int)(aW * scale) + 4;

        // ── Tabs de navegación ───────────────────────────────────────────────
        int tabW = 108;
        int tabH = 18;
        int tabY = 5;
        // 5 pestañas: NebulaCrakMod, Meteor, HUD, EDIT,MC, CONFI
        int totalW  = tabW * 5 + 16;
        int langBtnW = 76;
        int langBtnX = this.width - langBtnW - 8;
        // Espacio disponible entre fin de firma y botón de idioma
        int tabsStartX = Math.max(sigEndX + 6, (this.width - totalW) / 2);
        // Si no cabe, colocar debajo de la barra (no implementado en este scope)
        if (tabsStartX + totalW > langBtnX - 4) {
            tabsStartX = sigEndX + 4;
        }

        addTab(tabsStartX,              tabY, tabW, tabH, Tab.NEBULA,
               TranslationManager.get("nebula.tab.nebula", "NebulaCrakMod"));
        addTab(tabsStartX + tabW + 4,   tabY, tabW, tabH, Tab.METEOR,
               TranslationManager.get("nebula.tab.meteor", "Módulos Meteor"));
        addTab(tabsStartX + tabW*2 + 8, tabY, tabW, tabH, Tab.HUD,
               TranslationManager.get("nebula.tab.hud", "HUD"));
        addTab(tabsStartX + tabW*3 + 12, tabY, tabW, tabH, Tab.EDIT_MC,
               TranslationManager.get("nebula.tab.edit_mc", "EDIT,MC"));
        addTab(tabsStartX + tabW*4 + 16, tabY, tabW, tabH, Tab.CONFI,
               TranslationManager.get("nebula.tab.conf", "CONFI"));

        // ── Botón de idioma (esquina superior derecha) ───────────────────────
        String langCode = LanguageManager.code().toUpperCase();
        languageButton = new CompactButton(
                langBtnX, tabY, langBtnW, tabH,
                Component.literal("🌐 " + langCode),
                b -> languageDropdownOpen = !languageDropdownOpen,
                CompactButton.Type.LANGUAGE);
        navButtons.add(languageButton);
        addRenderableWidget(languageButton);

        rebuild();
    }

    private void addTab(int x, int y, int w, int h, Tab tab, String label) {
        CompactButton btn = new CompactButton(x, y, w, h,
                Component.literal(label),
                b -> { currentTab = tab; scrollOffset = 0; rebuild(); },
                CompactButton.Type.TAB);
        btn.setSelected(currentTab == tab);
        navButtons.add(btn);
        addRenderableWidget(btn);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REBUILD
    // ══════════════════════════════════════════════════════════════════════════
    private void rebuild() {
        // Limpiar contenido anterior
        for (AbstractWidget b : contentButtons) removeWidget(b);
        contentButtons.clear();

        // Actualizar estado visual de tabs
        for (AbstractWidget w : navButtons) {
            if (w instanceof CompactButton b) {
                String msg = b.getMessage().getString();
                if (msg.equals(TranslationManager.get("nebula.tab.nebula", "NebulaCrakMod")))
                    b.setSelected(currentTab == Tab.NEBULA);
                else if (msg.equals(TranslationManager.get("nebula.tab.meteor", "Módulos Meteor")))
                    b.setSelected(currentTab == Tab.METEOR);
                else if (msg.equals(TranslationManager.get("nebula.tab.hud", "HUD")))
                    b.setSelected(currentTab == Tab.HUD);
                else if (msg.equals(TranslationManager.get("nebula.tab.edit_mc", "EDIT,MC")))
                    b.setSelected(currentTab == Tab.EDIT_MC);
                else if (msg.equals(TranslationManager.get("nebula.tab.conf", "CONFI")))
                    b.setSelected(currentTab == Tab.CONFI);
            }
        }

        int panelW    = Math.min(400, this.width - 24);
        int panelLeft = (this.width - panelW) / 2;
        int cLeft     = panelLeft + 8;
        int cWidth    = panelW - 16;
        int y         = CONTENT_START_Y + 6 - scrollOffset;

        switch (currentTab) {
            case NEBULA   -> renderNebulaModules(cLeft, y, cWidth);
            case METEOR   -> renderMeteorModules(cLeft, y, cWidth);
            case HUD      -> renderHUD(cLeft, y, cWidth);
            case EDIT_MC  -> renderEditMC(cLeft, y, cWidth);
            case CONFI    -> renderConfi(cLeft, y, cWidth);
        }

        calculateMaxScroll();
        updateWidgetVisibility();
    }

    private void addContent(AbstractWidget btn) {
        contentButtons.add(btn);
        addRenderableWidget(btn);
    }

    private void updateWidgetVisibility() {
        int bottom = this.height - 8;
        for (AbstractWidget b : contentButtons) {
            boolean vis = b.getY() >= CONTENT_START_Y - 2 && b.getY() + b.getHeight() <= bottom + 2;
            b.visible = vis;
            b.active  = vis;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TOOLTIP HELPER
    // ═══════════════════════════════════════════════════════════════════════════
    private void setTooltip(AbstractWidget widget, String title, String desc) {
        // Store tooltip data in the widget's user data via a custom field
        // Since we can't modify AbstractWidget, we track hover state in the screen
        // The tooltip will be shown based on which widget is hovered
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB: NebulaCrakMod (módulos propios)
    //  Un único desplegable raíz "NebulaCrakMod" con subcategorías
    // ═══════════════════════════════════════════════════════════════════════════
    private void renderNebulaModules(int left, int y, int width) {
        // ── Encabezado raíz ──────────────────────────────────────────────────
        String arrow = nebulaRootExpanded ? "▼ " : "▶ ";
        String rootTitle = arrow + TranslationManager.get("nebula.section.root", "NebulaCrakMod");
        CompactButton rootBtn = new CompactButton(left, y, width, 20,
                Component.literal(rootTitle),
                b -> { nebulaRootExpanded = !nebulaRootExpanded; rebuild(); },
                CompactButton.Type.MASTER_HEADER);
        addContent(rootBtn);
        y += 23;

        if (!nebulaRootExpanded) return;

        int catLeft  = left + 4;
        int catWidth = width - 8;

        for (NebulaModules.CDJCategory category : NebulaModules.CDJ_CATEGORIES) {
            String catKey      = "nebula.cat." + category.getId();
            boolean isExpanded = categoryExpandedStates.getOrDefault(catKey, true);

            String catArrow = isExpanded ? "▼ " : "▶ ";
            String catTitle = catArrow + category.getDisplayName();

            CompactButton catBtn = new CompactButton(catLeft, y, catWidth, 17,
                    Component.literal(catTitle),
                    b -> { categoryExpandedStates.put(catKey, !isExpanded); rebuild(); },
                    CompactButton.Type.CATEGORY);
            addContent(catBtn);
            y += 19;

            if (isExpanded) {
                int modLeft  = catLeft + 8;
                int modWidth = catWidth - 16;

                for (Module module : category.getModules()) {
                    y = addModuleRow(modLeft, y, modWidth, module);
                }
                y += 3;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TAB: Módulos Meteor (originales, excluye categorías nebula.*)
    // ═════════════════════════════════════════════════════════════════════════
    private void renderMeteorModules(int left, int y, int width) {
        if (MeteorClient.mc == null || Modules.get() == null) {
            addContent(new CompactButton(left, y, width, 18,
                    Component.literal(TranslationManager.get("gui.meteor.not_loaded", "Meteor Client not loaded")),
                    b -> {}, CompactButton.Type.CATEGORY));
            return;
        }

        // Agrupar módulos por categoría, excluyendo nebula.*
        Map<Category, List<Module>> byCategory = new LinkedHashMap<>();
        for (Module mod : Modules.get().getAll()) {
            if (mod.category == null) continue;
            String cname = mod.category.name;
            // Excluir todas las categorías propias de Nebula
            if (cname.startsWith("nebula.") || cname.startsWith("Nebula")) continue;
            byCategory.computeIfAbsent(mod.category, k -> new ArrayList<>()).add(mod);
        }

        List<Category> sorted = new ArrayList<>(byCategory.keySet());
        sorted.sort(Comparator.comparing(c -> c.name));

        for (Category cat : sorted) {
            String stateKey  = "meteor." + cat.name;
            boolean isExpanded = categoryExpandedStates.getOrDefault(stateKey, false);
            String arrow       = isExpanded ? "▼ " : "▶ ";
            String catName     = TranslationManager.getCategoryName(cat.name);

            CompactButton catBtn = new CompactButton(left, y, width, 17,
                    Component.literal(arrow + catName),
                    b -> { categoryExpandedStates.put(stateKey, !isExpanded); rebuild(); },
                    CompactButton.Type.CATEGORY);
            addContent(catBtn);
            y += 19;

            if (isExpanded) {
                int modLeft  = left + 8;
                int modWidth = width - 16;
                List<Module> mods = byCategory.get(cat);
                if (mods != null) {
                    for (Module mod : mods) {
                        y = addModuleRow(modLeft, y, modWidth, mod);
                    }
                }
                y += 3;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TAB: HUD
    // ══════════════════════════════════════════════════════════════════════════
    private void renderHUD(int left, int y, int width) {
        List<AbstractWidget> bucket = new ArrayList<>();
        HUDScreen.build(this, left, y, width, bucket, this::rebuild);
        for (AbstractWidget btn : bucket) addContent(btn);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TAB: EDIT_MC (Editor de Menús y Pantallas de Minecraft)
    // ══════════════════════════════════════════════════════════════════════════
    private void renderEditMC(int left, int y, int width) {
        // ── A. Menú Principal ──────────────────────────────────────────────────
        y = renderEditMCSection(left, y, width,
                TranslationManager.get("gui.nebula.edit_mc.main_menu.title", "Menú Principal"),
                TranslationManager.get("gui.nebula.edit_mc.main_menu.desc", "Personaliza el fondo del menú principal de Minecraft."),
                "mainMenu",
                new String[]{
                    TranslationManager.get("confi.fondo.disabled", "Original"),
                    TranslationManager.get("confi.fondo.color", "Color Sólido"),
                    TranslationManager.get("confi.fondo.image", "Imagen Personalizada")
                });

        y += 4;

        // ── B. Inicio de Mojang ────────────────────────────────────────────────
        y = renderEditMCSection(left, y, width,
                TranslationManager.get("gui.nebula.edit_mc.mojang.title", "Pantalla de Mojang"),
                TranslationManager.get("gui.nebula.edit_mc.mojang.desc", "Cambia el fondo rojo de la pantalla de inicio de Mojang (logo intacto)."),
                "mojang",
                new String[]{
                    TranslationManager.get("confi.fondo.disabled", "Original"),
                    TranslationManager.get("confi.fondo.color", "Color Sólido"),
                    TranslationManager.get("confi.fondo.image", "Imagen Personalizada")
                });

        y += 4;

        // ── C. Pantallas de Carga ──────────────────────────────────────────────
        y = renderEditMCSection(left, y, width,
                TranslationManager.get("gui.nebula.edit_mc.loading.title", "Pantallas de Carga"),
                TranslationManager.get("gui.nebula.edit_mc.loading.desc", "Personaliza el fondo de las pantallas de carga del juego y paquetes de recursos."),
                "loading",
                new String[]{
                    TranslationManager.get("confi.fondo.disabled", "Original"),
                    TranslationManager.get("confi.fondo.color", "Color Sólido"),
                    TranslationManager.get("confi.splash.transparent", "Transparente"),
                    TranslationManager.get("confi.fondo.image", "Imagen Personalizada")
                });
    }

    private int renderEditMCSection(int left, int y, int width, String title, String desc, String prefix, String[] modes) {
        // Encabezado de sección
        CompactButton header = new CompactButton(left, y, width, 18,
                Component.literal(title + "  " + desc),
                b -> {}, CompactButton.Type.SECTION);
        addContent(header);
        y += 22;

        // Obtener modo actual, color y ruta según prefix
        NebulaConfig.BackgroundMode currentMode;
        int currentColor;
        String currentPath;

        switch (prefix) {
            case "mainMenu" -> {
                currentMode = NebulaConfig.mainMenuBackgroundMode;
                currentColor = NebulaConfig.mainMenuBackgroundColor;
                currentPath = NebulaConfig.mainMenuImagePath;
            }
            case "mojang" -> {
                currentMode = NebulaConfig.mojangScreenBackgroundMode;
                currentColor = NebulaConfig.mojangScreenBackgroundColor;
                currentPath = NebulaConfig.mojangScreenImagePath;
            }
            case "loading" -> {
                currentMode = NebulaConfig.loadingScreenBackgroundMode;
                currentColor = NebulaConfig.loadingScreenBackgroundColor;
                currentPath = NebulaConfig.loadingScreenImagePath;
            }
            default -> { currentMode = NebulaConfig.BackgroundMode.ORIGINAL; currentColor = 0xFF121218; currentPath = ""; }
        }

        int finalCurrentModeIdx = currentMode.ordinal();
        int finalCurrentColor = currentColor;
        String finalCurrentPath = currentPath;
        String finalPrefix = prefix;

        // Selector de modo
        for (int i = 0; i < modes.length; i++) {
            final int idx = i;
            boolean selected = (finalCurrentModeIdx == idx);
            CompactButton modeBtn = new CompactButton(left + 4, y, width - 8, 20,
                    Component.literal(modes[i]),
                    b -> {
                        switch (finalPrefix) {
                            case "mainMenu" -> NebulaConfig.mainMenuBackgroundMode = NebulaConfig.BackgroundMode.values()[idx];
                            case "mojang" -> NebulaConfig.mojangScreenBackgroundMode = NebulaConfig.BackgroundMode.values()[idx];
                            case "loading" -> NebulaConfig.loadingScreenBackgroundMode = NebulaConfig.BackgroundMode.values()[idx];
                        }
                        NebulaConfig.save();
                        rebuild();
                    },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            addContent(modeBtn);
            y += 22;
        }
        y += 2;

        // Controles según modo seleccionado
        if (finalCurrentModeIdx == 1) { // COLOR
            y = buildColorPicker(left, y, width, finalCurrentColor, c -> {
                switch (finalPrefix) {
                    case "mainMenu" -> NebulaConfig.mainMenuBackgroundColor = c;
                    case "mojang" -> NebulaConfig.mojangScreenBackgroundColor = c;
                    case "loading" -> NebulaConfig.loadingScreenBackgroundColor = c;
                }
                NebulaConfig.save();
                rebuild();
            });
        } else if (finalCurrentModeIdx == 2 || (finalCurrentModeIdx == 3 && "loading".equals(finalPrefix))) { // IMAGE or TRANSPARENT
            y = buildEditMCImageSelector(left, y, width, finalPrefix, finalCurrentPath, p -> {
                switch (finalPrefix) {
                    case "mainMenu" -> NebulaConfig.mainMenuImagePath = p;
                    case "mojang" -> NebulaConfig.mojangScreenImagePath = p;
                    case "loading" -> NebulaConfig.loadingScreenImagePath = p;
                }
                NebulaConfig.save();
                rebuild();
            }, () -> {
                switch (finalPrefix) {
                    case "mainMenu" -> { NebulaConfig.mainMenuImagePath = ""; NebulaConfig.mainMenuBackgroundMode = NebulaConfig.BackgroundMode.ORIGINAL; }
                    case "mojang" -> { NebulaConfig.mojangScreenImagePath = ""; NebulaConfig.mojangScreenBackgroundMode = NebulaConfig.BackgroundMode.ORIGINAL; }
                    case "loading" -> { NebulaConfig.loadingScreenImagePath = ""; NebulaConfig.loadingScreenBackgroundMode = NebulaConfig.BackgroundMode.ORIGINAL; }
                }
                NebulaConfig.save();
                rebuild();
            });
        }
        return y;
    }

    private int buildColorPicker(int left, int y, int width, int currentColor, java.util.function.IntConsumer setter) {
        // Quick colors
        int[] quickColors = {
            0xFF121218, 0xFF1E1E1E, 0xFF0D1117, 0xFF1F1F2E,
            0xFF2D1B2D, 0xFF1B2D2D, 0xFF2D2D1B, 0xFF000000,
            0xFF8B0000, 0xFF006400, 0xFF00008B, 0xFF4B0082,
            0xFFFFD700, 0xFFFF8C00, 0xFFFF0000, 0xFF00FFFF
        };

        int swatchSize = 14;
        int gap = 3;
        int totalSwatches = quickColors.length;
        int rowWidth = totalSwatches * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);

        for (int i = 0; i < totalSwatches; i++) {
            final int color = quickColors[i];
            boolean isSelected = (currentColor == color);
            int sx = startX + i * (swatchSize + gap);

            CompactButton swatch = new CompactButton(sx, y, swatchSize, swatchSize,
                    Component.empty(), b -> { setter.accept(color); rebuild(); },
                    CompactButton.Type.SETTINGS) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                    g.fill(x + 1, iy + 1, x + w - 1, iy + h - 1, color | 0xFF000000);
                    int border = isSelected ? 0xFFFFFFFF : 0xFF555565;
                    g.fill(x, iy, x + w, iy + 1, border);
                    g.fill(x, iy + h - 1, x + w, iy + h, border);
                    g.fill(x, iy + 1, x + 1, iy + h - 1, border);
                    g.fill(x + w - 1, iy + 1, x + w, iy + h - 1, border);
                    if (isSelected) {
                        g.fill(x + w / 2 - 1, iy + h / 2 - 1, x + w / 2 + 2, iy + h / 2 + 2, 0xFFFFFFFF);
                    }
                }
            };
            addContent(swatch);
        }
        y += swatchSize + 8;

        // HEX/RGB Color Input
        ColorInputButton hexInput = new ColorInputButton(left + 4, y, width - 8, 22, currentColor, c -> {
            setter.accept(c);
            rebuild();
        });
        addContent(hexInput);
        y += 26;

        return y;
    }

    private int buildEditMCImageSelector(int left, int y, int width, String prefix, String currentPath,
                                          java.util.function.Consumer<String> onSelect, Runnable onClear) {
        // Botón Browse
        CompactButton browseBtn = new CompactButton(left + 4, y, width - 8, 22,
                Component.literal(TranslationManager.get("gui.nebula.hud.image.browse", "Seleccionar Imagen")),
                b -> openFileChooser(onSelect),
                CompactButton.Type.SETTINGS);
        addContent(browseBtn);
        y += 26;

        // Vista previa / ruta actual
        if (currentPath != null && !currentPath.isEmpty()) {
            String displayPath = shortenPath(currentPath);
            CompactButton previewBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.current", displayPath)),
                    b -> openImageInViewer(currentPath),
                    CompactButton.Type.SETTINGS);
            addContent(previewBtn);
            y += 26;

            // Botón eliminar
            CompactButton clearBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.remove", "Eliminar Imagen")),
                    b -> onClear.run(),
                    CompactButton.Type.DANGER);
            addContent(clearBtn);
            y += 26;
        } else {
            CompactButton noneBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.none", "Sin imagen seleccionada")),
                    b -> {}, CompactButton.Type.DISABLED);
            addContent(noneBtn);
            y += 26;
        }

        return y;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB: CONFI
    // ═══════════════════════════════════════════════════════════════════════════
    private void renderConfi(int left, int y, int width) {
        // ── Tecla de apertura ──────────────────────────────────────────────────
        String keyName = GLFW.glfwGetKeyName(NebulaConfig.openKeyCode, 0);
        if (keyName == null) keyName = "Unknown (" + NebulaConfig.openKeyCode + ")";
        keyName = keyName.toUpperCase();

        String keybindTitle = TranslationManager.get("confi.keybind.title", "Open Key");
        String keybindDesc = TranslationManager.get("confi.keybind.desc", "Key to open/close the NebulaCrakMod GUI.");
        String currentLabel = TranslationManager.get("confi.keybind.current", "Current: ");
        String changeLabel = waitingForKeybind ? TranslationManager.get("confi.keybind.waiting", "Press any key...") : TranslationManager.get("confi.keybind.change", "Change Key");

        // Título de sección
        CompactButton keybindHeader = new CompactButton(left, y, width, 18,
                Component.literal(keybindTitle + "  " + keybindDesc),
                b -> {}, CompactButton.Type.SECTION);
        addTooltip(keybindHeader, keybindTitle, keybindDesc);
        addContent(keybindHeader);
        y += 22;

        // Tecla actual + botón cambiar
        int btnW = 120, btnH = 20;
        int btnX = left + width - btnW - 4;

        CompactButton currentKeyBtn = new CompactButton(left, y, width - btnW - 8, btnH,
                Component.literal(currentLabel + keyName),
                b -> {}, CompactButton.Type.LABEL);
        addTooltip(currentKeyBtn, keybindTitle, keybindDesc);
        addContent(currentKeyBtn);

        CompactButton changeKeyBtn = new CompactButton(btnX, y, btnW, btnH,
                Component.literal(changeLabel),
                b -> { waitingForKeybind = true; rebuild(); },
                waitingForKeybind ? CompactButton.Type.DANGER : CompactButton.Type.SETTINGS);
        changeKeyBtn.setActivated(waitingForKeybind);
        addTooltip(changeKeyBtn, keybindTitle, keybindDesc);
        addContent(changeKeyBtn);
        y += 26;

        // ── Cerrar con tecla de apertura ───────────────────────────────────────
        String closeKeyTitle = TranslationManager.get("confi.close_with_key.title", "Close with Open Key");
        String closeKeyDesc = TranslationManager.get("confi.close_with_key.desc", "When enabled, the same key that opens the GUI also closes it.");

        CompactButton closeKeyHeader = new CompactButton(left, y, width, 18,
                Component.literal(closeKeyTitle + "  " + closeKeyDesc),
                b -> {}, CompactButton.Type.SECTION);
        addTooltip(closeKeyHeader, closeKeyTitle, closeKeyDesc);
        addContent(closeKeyHeader);
        y += 22;

        String onLabel = TranslationManager.get("confi.state.on", "ON");
        String offLabel = TranslationManager.get("confi.state.off", "OFF");
        String closeKeyState = NebulaConfig.closeWithOpenKey ? " [" + onLabel + "]" : " [" + offLabel + "]";

        CompactButton closeKeyBtn = new CompactButton(left, y, width, 20,
                Component.literal(closeKeyTitle + closeKeyState),
                b -> { NebulaConfig.closeWithOpenKey = !NebulaConfig.closeWithOpenKey; NebulaConfig.save(); rebuild(); },
                NebulaConfig.closeWithOpenKey ? CompactButton.Type.MODULE : CompactButton.Type.MODULE);
        closeKeyBtn.setActivated(NebulaConfig.closeWithOpenKey);
        addTooltip(closeKeyBtn, closeKeyTitle, closeKeyDesc);
        addContent(closeKeyBtn);
        y += 26;

        // ── Tipo de letra ──────────────────────────────────────────────────────
        String fontTitle = TranslationManager.get("confi.font.title", "Font");
        String fontDesc = TranslationManager.get("confi.font.fallback", "Fallback to default if unavailable");
        String arialLabel = TranslationManager.get("confi.font.arial", "Arial");
        String consolasLabel = TranslationManager.get("confi.font.consolas", "Consolas");

        CompactButton fontHeader = new CompactButton(left, y, width, 18,
                Component.literal(fontTitle + "  " + fontDesc),
                b -> {}, CompactButton.Type.SECTION);
        addTooltip(fontHeader, fontTitle, fontDesc);
        addContent(fontHeader);
        y += 22;

        String[] fontOptions = { arialLabel, consolasLabel };
        int currentFontIdx = NebulaConfig.fontType.ordinal();

        for (int i = 0; i < fontOptions.length; i++) {
            final int idx = i;
            boolean selected = (currentFontIdx == idx);
            CompactButton fontBtn = new CompactButton(left + 4, y, width - 8, 20,
                    Component.literal(fontOptions[i]),
                    b -> { NebulaConfig.fontType = NebulaConfig.FontType.values()[idx]; NebulaConfig.save(); FontManager.clearCache(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            String fontDescText = idx == 0 ? 
                TranslationManager.get("confi.font.arial.desc", "Usa la fuente Arial para la interfaz de NebulaCrakMod.") :
                TranslationManager.get("confi.font.consolas.desc", "Usa la fuente Consolas (monoespaciada) para la interfaz de NebulaCrakMod.");
            addTooltip(fontBtn, fontOptions[i], fontDescText);
            addContent(fontBtn);
            y += 22;
        }
        y += 4;

        // ── Estilo de botones ──────────────────────────────────────────────────
        String styleTitle = TranslationManager.get("confi.button_style.title", "Button Style");
        String styleDesc = TranslationManager.get("confi.button_style.desc", "Changes the appearance of buttons in NebulaCrakMod GUI.");
        String defaultLabel = TranslationManager.get("confi.button_style.default", "Default (Minecraft)");
        String transparentLabel = TranslationManager.get("confi.button_style.transparent", "Transparent");
        String meteorLabel = TranslationManager.get("confi.button_style.meteor", "Meteor");

        CompactButton styleHeader = new CompactButton(left, y, width, 18,
                Component.literal(styleTitle + "  " + styleDesc),
                b -> {}, CompactButton.Type.SECTION);
        addTooltip(styleHeader, styleTitle, styleDesc);
        addContent(styleHeader);
        y += 22;

        String[] styleOptions = { defaultLabel, transparentLabel, meteorLabel };
        int currentStyleIdx = NebulaConfig.buttonStyle.ordinal();

        for (int i = 0; i < styleOptions.length; i++) {
            final int idx = i;
            boolean selected = (currentStyleIdx == idx);
            CompactButton styleBtn = new CompactButton(left + 4, y, width - 8, 20,
                    Component.literal(styleOptions[i]),
                    b -> { NebulaConfig.buttonStyle = NebulaConfig.ButtonStyle.values()[idx]; NebulaConfig.save(); rebuild(); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            String styleDescText = switch (idx) {
                case 0 -> TranslationManager.get("confi.button_style.default.desc", "Estilo predeterminado de Minecraft con bordes y fondo sólido.");
                case 1 -> TranslationManager.get("confi.button_style.transparent.desc", "Botones transparentes con bordes sutiles.");
                case 2 -> TranslationManager.get("confi.button_style.meteor.desc", "Estilo inspirado en Meteor Client con acentos morados.");
                default -> "";
            };
            addTooltip(styleBtn, styleOptions[i], styleDescText);
            addContent(styleBtn);
            y += 22;
        }
    }

    private void addTooltip(AbstractWidget widget, String title, String desc) {
        // We track tooltip by checking hover state in render()
        // The tooltip data is stored in the screen and shown when hovering over this widget
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB: ConfiColors (Legacy - Redirige a HUD)
    // ═══════════════════════════════════════════════════════════════════════════
    private void renderConfiColors(int left, int y, int width) {
        renderHUD(left, y, width);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Fila de módulo (toggle ON/OFF)
    // ══════════════════════════════════════════════════════════════════════════
    private int addModuleRow(int left, int y, int width, Module module) {
        boolean active = module.isActive();
        String onLabel  = TranslationManager.get("confi.state.on",  "ON");
        String offLabel = TranslationManager.get("confi.state.off", "OFF");
        String state    = active ? " [" + onLabel + "]" : " [" + offLabel + "]";
        String dispName = TranslationManager.getModuleName(module.name);
        String label    = dispName + state;

        CompactButton modBtn = new CompactButton(left, y, width, 16,
                Component.literal(label),
                b -> { module.toggle(); rebuild(); },
                CompactButton.Type.MODULE);
        modBtn.setActivated(active);
        addContent(modBtn);
        return y + 18;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCROLL
    // ══════════════════════════════════════════════════════════════════════════
    private void calculateMaxScroll() {
        int totalH = 0;
        for (AbstractWidget b : contentButtons) {
            int bBottom = (b.getY() + scrollOffset) + b.getHeight();
            if (bBottom > totalH) totalH = bBottom;
        }
        int visibleArea = this.height - 8;
        maxScroll = Math.max(0, totalH - visibleArea);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (languageDropdownOpen) { languageDropdownOpen = false; }
        if (maxScroll > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - deltaY * 18));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MOUSE MOVED — para tooltips
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Find which content button is hovered
        AbstractWidget newHovered = null;
        for (AbstractWidget btn : contentButtons) {
            if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                newHovered = btn;
                break;
            }
        }

        if (newHovered != hoveredWidget) {
            hoveredWidget = newHovered;
            hoverStartTime = System.currentTimeMillis();
            tooltipTitle = "";
            tooltipDesc = "";
        }

        // If still hovering same widget, check if tooltip should show
        if (hoveredWidget != null && hoveredWidget == newHovered) {
            long hoverTime = System.currentTimeMillis() - hoverStartTime;
            if (hoverTime >= TOOLTIP_DELAY_MS && tooltipTitle.isEmpty()) {
                // Set tooltip based on which widget is hovered
                setTooltipContent(hoveredWidget);
            }
        }

        super.mouseMoved(mouseX, mouseY);
    }

    private void setTooltipContent(AbstractWidget widget) {
        // Determine tooltip content based on widget message
        String msg = widget.getMessage().getString();
        
        // Keybind section
        if (msg.contains(TranslationManager.get("confi.keybind.title", "Open Key"))) {
            tooltipTitle = TranslationManager.get("confi.keybind.title", "Open Key");
            tooltipDesc = TranslationManager.get("confi.keybind.desc", "Key to open/close the NebulaCrakMod GUI.");
        }
        // Close with open key
        else if (msg.contains(TranslationManager.get("confi.close_with_key.title", "Close with Open Key"))) {
            tooltipTitle = TranslationManager.get("confi.close_with_key.title", "Close with Open Key");
            tooltipDesc = TranslationManager.get("confi.close_with_key.desc", "When enabled, the same key that opens the GUI also closes it.");
        }
        // Font section
        else if (msg.contains(TranslationManager.get("confi.font.title", "Font"))) {
            tooltipTitle = TranslationManager.get("confi.font.title", "Font");
            tooltipDesc = TranslationManager.get("confi.font.fallback", "Fallback to default if unavailable");
        }
        else if (msg.contains(TranslationManager.get("confi.font.arial", "Arial"))) {
            tooltipTitle = TranslationManager.get("confi.font.arial", "Arial");
            tooltipDesc = TranslationManager.get("confi.font.arial.desc", "Usa la fuente Arial para la interfaz de NebulaCrakMod.");
        }
        else if (msg.contains(TranslationManager.get("confi.font.consolas", "Consolas"))) {
            tooltipTitle = TranslationManager.get("confi.font.consolas", "Consolas");
            tooltipDesc = TranslationManager.get("confi.font.consolas.desc", "Usa la fuente Consolas (monoespaciada) para la interfaz de NebulaCrakMod.");
        }
        // Button style section
        else if (msg.contains(TranslationManager.get("confi.button_style.title", "Button Style"))) {
            tooltipTitle = TranslationManager.get("confi.button_style.title", "Button Style");
            tooltipDesc = TranslationManager.get("confi.button_style.desc", "Changes the appearance of buttons in NebulaCrakMod GUI.");
        }
        else if (msg.contains(TranslationManager.get("confi.button_style.default", "Default (Minecraft)"))) {
            tooltipTitle = TranslationManager.get("confi.button_style.default", "Default (Minecraft)");
            tooltipDesc = TranslationManager.get("confi.button_style.default.desc", "Estilo predeterminado de Minecraft con bordes y fondo sólido.");
        }
        else if (msg.contains(TranslationManager.get("confi.button_style.transparent", "Transparent"))) {
            tooltipTitle = TranslationManager.get("confi.button_style.transparent", "Transparent");
            tooltipDesc = TranslationManager.get("confi.button_style.transparent.desc", "Botones transparentes con bordes sutiles.");
        }
        else if (msg.contains(TranslationManager.get("confi.button_style.meteor", "Meteor"))) {
            tooltipTitle = TranslationManager.get("confi.button_style.meteor", "Meteor");
            tooltipDesc = TranslationManager.get("confi.button_style.meteor.desc", "Estilo inspirado en Meteor Client con acentos morados.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MOUSE CLICK — manejo del dropdown de idioma
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        if (languageDropdownOpen) {
            double mx = event.x(), my = event.y();
            int dropW  = 76, dropX = this.width - dropW - 8, dropY = 25, itemH = 17;

            if (mx >= dropX && mx <= dropX + dropW) {
                if (my >= dropY + 2 && my <= dropY + 2 + itemH) {
                    changeLanguage(LanguageManager.Language.ES_MX); return true;
                }
                if (my >= dropY + 2 + itemH && my <= dropY + 2 + itemH * 2) {
                    changeLanguage(LanguageManager.Language.EN_US); return true;
                }
            }
            languageDropdownOpen = false;
            if (languageButton != null && languageButton.isMouseOver(mx, my)) {
                // deja que el botón cierre el dropdown normalmente
            } else {
                return true;
            }
        }
        return super.mouseClicked(event, isFocused);
    }

    private void changeLanguage(LanguageManager.Language lang) {
        LanguageManager.set(lang);
        TranslationManager.reload();
        languageDropdownOpen = false;
        clearWidgets();
        init();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  KEY PRESS
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        // Handle keybind change mode
        if (waitingForKeybind) {
            // Ignore ESC - it cancels the keybind change
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKeybind = false;
                rebuild();
                return true;
            }
            // Set the new keybind
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
        // Close with open key (if enabled in CONFI)
        if (key == NebulaConfig.openKeyCode && NebulaConfig.closeWithOpenKey) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        // ── ConfiColors: Fondo personalizado ─────────────────────────────────────
        renderCustomBackground(g);

        // Panel de contenido (detrás de todo)
        int panelW    = Math.min(400, this.width - 24);
        int panelLeft = (this.width - panelW) / 2;
        int panelH    = this.height - CONTENT_START_Y - 8;

        // ── ConfiColors: PanelCargas ────────────────────────────────────────────
        renderPanelCargas(g, panelLeft, CONTENT_START_Y, panelW, panelH);

        // Barra superior
        g.fill(0, 0, this.width, CONTENT_START_Y - 1, 0xF0101016);
        g.fill(0, CONTENT_START_Y - 1, this.width, CONTENT_START_Y, 0xFF2A2A38);

        // ── FIRMA TRICOLOR (una sola vez, en la barra superior izquierda) ────
        // NebulaCrakMod (rojo #EF4444) + " by " (blanco) + CDJ dev (verde #22C55E)
        String sigName   = "NebulaCrakMod";
        String sigBy     = " by ";
        String sigAuthor = "CDJ dev";

        int nameW   = this.font.width(sigName);
        int byW     = this.font.width(sigBy);
        int authorW = this.font.width(sigAuthor);

        int sigNameX   = 8;
        int sigByX     = sigNameX + nameW;
        int sigAuthorX = sigByX + byW;
        int sigY       = 8;

        // NebulaCrakMod en rojo (#EF4444)
        g.drawString(this.font, sigName, sigNameX, sigY, 0xFFEF4444, false);
        // " by " en blanco (#FFFFFF)
        g.drawString(this.font, sigBy, sigByX, sigY, 0xFFFFFFFF, false);
        // CDJ dev en verde (#22C55E)
        g.drawString(this.font, sigAuthor, sigAuthorX, sigY, 0xFF22C55E, false);

        // Botones y widgets (tabs, contenido)
        super.render(g, mouseX, mouseY, pt);

        // Dropdown de idioma (encima de todo)
        if (languageDropdownOpen) renderLanguageDropdown(g, mouseX, mouseY);

        // ── ConfiColors: Splash ────────────────────────────────────────────────
        renderSplash(g);

        // ── TOOLTIP ────────────────────────────────────────────────────────────
        if (!tooltipTitle.isEmpty() && hoveredWidget != null) {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        Font font = FontManager.getCurrentFont();
        int padding = 6;
        int titleWidth = font.width(tooltipTitle);
        int descWidth = font.width(tooltipDesc);
        int maxWidth = Math.max(titleWidth, descWidth);
        int width = maxWidth + padding * 2;
        int height = 20 + padding * 2; // title (10) + desc (10) + padding

        int x = mouseX + 16;
        int y = mouseY - height - 8;

        // Keep tooltip on screen
        if (x + width > this.width) x = this.width - width - 4;
        if (y < 4) y = mouseY + 16;

        // Background
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF1E1E2E);
        g.fill(x, y, x + width, y + height, 0xFF141418);
        // Border
        g.fill(x, y, x + width, y + 1, 0xFF8B5CF6);
        g.fill(x, y + height - 1, x + width, y + height, 0xFF8B5CF6);
        g.fill(x, y, x + 1, y + height, 0xFF8B5CF6);
        g.fill(x + width - 1, y, x + width, y + height, 0xFF8B5CF6);

        // Title
        g.drawString(font, tooltipTitle, x + padding, y + padding, 0xFFFFFFFF, false);
        // Description
        g.drawString(font, tooltipDesc, x + padding, y + padding + 10, 0xFF94A3B8, false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONFICOLORS: Fondo personalizado
    // ═══════════════════════════════════════════════════════════════════════════
    private void renderCustomBackground(GuiGraphics g) {
        ConfiColorsConfig.FondoMode mode = ConfiColorsConfig.fondoMode;

        if (mode == ConfiColorsConfig.FondoMode.DISABLED) {
            // Fondo por defecto de Minecraft
            renderBackground(g, 0, 0, 0);
            return;
        }

        if (mode == ConfiColorsConfig.FondoMode.COLOR) {
            // Color sólido en toda la pantalla
            g.fill(0, 0, this.width, this.height, ConfiColorsConfig.fondoColor | 0xFF000000);
            return;
        }

        if (mode == ConfiColorsConfig.FondoMode.IMAGE) {
            // Cargar y renderizar imagen personalizada
            Object texture = NebulaTextureManager.getTexture("fondo");
            if (texture == null && !ConfiColorsConfig.fondoImageBase64.isEmpty()) {
                texture = NebulaTextureManager.loadTextureFromBase64("fondo", ConfiColorsConfig.fondoImageBase64);
            } else if (texture == null && !ConfiColorsConfig.fondoImagePath.isEmpty()) {
                texture = NebulaTextureManager.loadTextureFromFile("fondo", ConfiColorsConfig.fondoImagePath);
            }

            if (texture != null) {
                // Renderizar imagen escalada a toda la pantalla
                NebulaTextureManager.blit(g, texture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
            } else {
                // Fallback a color
                g.fill(0, 0, this.width, this.height, ConfiColorsConfig.fondoColor | 0xFF000000);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONFICOLORS: PanelCargas
    // ═══════════════════════════════════════════════════════════════════════════
    private void renderPanelCargas(GuiGraphics g, int x, int y, int w, int h) {
        if (!ConfiColorsConfig.panelCargasVisible) return;

        int color = ConfiColorsConfig.panelCargasColor;
        if (ConfiColorsConfig.panelCargasTransparent) {
            // Panel transparente con color tintado
            int alpha = 0x80; // 50% transparencia
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, (color & 0x00FFFFFF) | (alpha << 24));
            g.fill(x, y, x + w, y + h, (color & 0x00FFFFFF) | (alpha << 24));
        } else {
            // Panel opaco
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, color | 0xFF000000);
            g.fill(x, y, x + w, y + h, color | 0xFF000000);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONFICOLORS: Splash
    // ══════════════════════════════════════════════════════════════════════════
    private void renderSplash(GuiGraphics g) {
        ConfiColorsConfig.SplashMode mode = ConfiColorsConfig.splashMode;

        if (mode == ConfiColorsConfig.SplashMode.DISABLED) return;

        int splashSize = Math.min(this.width, this.height) / 3;
        int splashX = (this.width - splashSize) / 2;
        int splashY = (this.height - splashSize) / 2;

        if (mode == ConfiColorsConfig.SplashMode.COLOR) {
            // Cuadrado de color sólido centrado
            g.fill(splashX, splashY, splashX + splashSize, splashY + splashSize,
                   ConfiColorsConfig.splashColor | 0xFF000000);
            return;
        }

        if (mode == ConfiColorsConfig.SplashMode.IMAGE) {
            // Imagen: por compatibilidad con Mojang mappings, usar color de fallback
            // TODO: Implementar carga de imagen cuando se use mapeo Yarn
            g.fill(splashX, splashY, splashX + splashSize, splashY + splashSize,
                   ConfiColorsConfig.splashColor | 0xFF000000);
        }
    }

    private void renderPanelBg(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A2A38);
        g.fill(x, y, x + w, y + h, 0xF0101016);
    }

    private void renderLanguageDropdown(GuiGraphics g, int mouseX, int mouseY) {
        int dropW = 76, dropX = this.width - dropW - 8, dropY = 25, itemH = 17;
        int dropH = itemH * 2 + 4;

        g.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + dropH + 1, 0xFF8B5CF6);
        g.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF5141420);

        LanguageManager.Language cur = LanguageManager.get();
        renderLangItem(g, mouseX, mouseY, dropX, dropY + 2, dropW, itemH,
                       "ES_MX", cur == LanguageManager.Language.ES_MX);
        renderLangItem(g, mouseX, mouseY, dropX, dropY + 2 + itemH, dropW, itemH,
                       "EN_US", cur == LanguageManager.Language.EN_US);
    }

    private void renderLangItem(GuiGraphics g, int mx, int my,
                                 int dx, int dy, int dw, int ih,
                                 String code, boolean selected) {
        boolean hover   = mx >= dx && mx <= dx + dw && my >= dy && my <= dy + ih;
        int     bg      = hover ? 0xFF2A2A3A : (selected ? 0xFF261C3C : 0x00000000);
        if (bg != 0) g.fill(dx + 2, dy, dx + dw - 2, dy + ih, bg);
        String text  = (selected ? "✓ " : "  ") + code;
        int    color = selected ? 0xFFA78BFA : (hover ? 0xFFFFFFFF : 0xFFCBD5E1);
        g.drawString(this.font, text, dx + 8, dy + (ih - 8) / 2, color, false);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER METHODS (shared with HUDScreen)
    // ══════════════════════════════════════════════════════════════════════════
    private void openFileChooser(java.util.function.Consumer<String> callback) {
        new Thread(() -> {
            try {
                java.awt.FileDialog dialog = new java.awt.FileDialog(
                        (java.awt.Frame) null,
                        TranslationManager.get("gui.nebula.hud.image.select", "Seleccionar Imagen"),
                        java.awt.FileDialog.LOAD
                );
                dialog.setFilenameFilter((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                });
                dialog.setVisible(true);
                String file = dialog.getFile();
                String dir = dialog.getDirectory();
                if (file != null && dir != null) {
                    String fullPath = dir + file;
                    Path path = Paths.get(fullPath);
                    if (Files.exists(path) && Files.size(path) > 0) {
                        String savedPath = copyImageToModFolder(path);
                        if (savedPath != null) {
                            Minecraft.getInstance().execute(() -> callback.accept(savedPath));
                        }
                    }
                }
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> {
                    try {
                        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                                "Images (PNG, JPG)", "png", "jpg", "jpeg"));
                        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                            Path path = chooser.getSelectedFile().toPath();
                            String savedPath = copyImageToModFolder(path);
                            if (savedPath != null) {
                                callback.accept(savedPath);
                            }
                        }
                    } catch (Exception ex) {
                    }
                });
            }
        }).start();
    }

    private String copyImageToModFolder(Path source) {
        try {
            Path modDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                    .resolve("nebulacrakmod").resolve("images");
            Files.createDirectories(modDir);

            String fileName = source.getFileName().toString();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String ext = fileName.substring(fileName.lastIndexOf('.'));
            Path dest = modDir.resolve(baseName + "_" + System.currentTimeMillis() + ext);

            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void openImageInViewer(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException e) {
        }
    }

    private String shortenPath(String path) {
        if (path == null || path.isEmpty()) return "";
        File f = new File(path);
        return f.getName();
    }
}