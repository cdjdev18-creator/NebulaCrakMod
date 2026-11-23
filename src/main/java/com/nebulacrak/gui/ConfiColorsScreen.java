package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.translation.TranslationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ConfiColorsScreen {

    private static boolean fondoExpanded      = true;
    private static boolean panelExpanded      = true;
    private static boolean splashExpanded     = true;

    private static final int[] QUICK_COLORS = {
        0xFFEF4444, 0xFF22C55E, 0xFF3B82F6, 0xFF8B5CF6,
        0xFFF59E0B, 0xFF06B6D4, 0xFF64748B, 0xFFFFFFFF, 0xFF000000
    };

    public static void build(Screen screen, int left, int startY, int width,
                             List<CompactButton> sink, Runnable rebuild) {
        int y = startY;
        int innerLeft  = left + 4;
        int innerWidth = width - 8;

        y = buildSection(sink, rebuild, left, y, width,
                         () -> fondoExpanded, v -> fondoExpanded = v,
                         TranslationManager.get("confi.fondo.title", "Fondo"));

        if (fondoExpanded) {
            ConfiColorsConfig.FondoMode fMode = ConfiColorsConfig.fondoMode;
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.fondo.disabled", "Desactivado"),
                           fMode == ConfiColorsConfig.FondoMode.DISABLED,
                           () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.DISABLED; ConfiColorsConfig.save(); });
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.fondo.color", "Color sólido"),
                           fMode == ConfiColorsConfig.FondoMode.COLOR,
                           () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.COLOR; ConfiColorsConfig.save(); });
            if (fMode == ConfiColorsConfig.FondoMode.COLOR) {
                y = buildColorPicker(sink, rebuild, innerLeft + 8, y, innerWidth - 8,
                                     ConfiColorsConfig.fondoColor,
                                     c -> { ConfiColorsConfig.fondoColor = c; ConfiColorsConfig.save(); });
            }
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.fondo.image", "Imagen personalizada"),
                           fMode == ConfiColorsConfig.FondoMode.IMAGE,
                           () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.IMAGE; ConfiColorsConfig.save(); });
            if (fMode == ConfiColorsConfig.FondoMode.IMAGE) {
                String pathLabel = ConfiColorsConfig.fondoImagePath.isEmpty()
                    ? TranslationManager.get("confi.image.none", "Sin imagen")
                    : shortenPath(ConfiColorsConfig.fondoImagePath);
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15, Component.literal("Img: " + pathLabel), b -> {}, CompactButton.Type.MODULE));
                y += 17;
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.help", "Coloca imagen en config/nebulacrakmod/bg.png")),
                        b -> {}, CompactButton.Type.MODULE));
                y += 17;
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.use_default", "Usar bg.png")),
                        b -> {
                            String bgPath = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("nebulacrakmod/bg.png").toString();
                            ConfiColorsConfig.fondoImagePath = bgPath;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY));
                y += 17;
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.clear", "Quitar imagen")),
                        b -> {
                            ConfiColorsConfig.fondoImagePath = "";
                            ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.DISABLED;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY));
                y += 17;
            }
            y += 3;
        }

        y = buildSection(sink, rebuild, left, y, width,
                         () -> panelExpanded, v -> panelExpanded = v,
                         TranslationManager.get("confi.panel.title", "PanelCargas"));

        if (panelExpanded) {
            y = buildToggle(sink, rebuild, innerLeft, y, innerWidth,
                            TranslationManager.get("confi.panel.visible", "Visible"),
                            ConfiColorsConfig.panelCargasVisible,
                            v -> { ConfiColorsConfig.panelCargasVisible = v; ConfiColorsConfig.save(); });
            y = buildToggle(sink, rebuild, innerLeft, y, innerWidth,
                            TranslationManager.get("confi.panel.transparent", "Transparente"),
                            ConfiColorsConfig.panelCargasTransparent,
                            v -> { ConfiColorsConfig.panelCargasTransparent = v; ConfiColorsConfig.save(); });
            sink.add(new CompactButton(innerLeft, y, innerWidth, 14,
                    Component.literal(TranslationManager.get("confi.panel.color", "Color del panel:")),
                    b -> {}, CompactButton.Type.CATEGORY));
            y += 16;
            y = buildColorPicker(sink, rebuild, innerLeft + 4, y, innerWidth - 4,
                                 ConfiColorsConfig.panelCargasColor,
                                 c -> { ConfiColorsConfig.panelCargasColor = c; ConfiColorsConfig.save(); });
            y += 3;
        }

        y = buildSection(sink, rebuild, left, y, width,
                         () -> splashExpanded, v -> splashExpanded = v,
                         TranslationManager.get("confi.splash.title", "Splash"));

        if (splashExpanded) {
            ConfiColorsConfig.SplashMode sMode = ConfiColorsConfig.splashMode;
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.splash.disabled", "Desactivado"),
                           sMode == ConfiColorsConfig.SplashMode.DISABLED,
                           () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.DISABLED; ConfiColorsConfig.save(); });
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.splash.color", "Color sólido"),
                           sMode == ConfiColorsConfig.SplashMode.COLOR,
                           () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.COLOR; ConfiColorsConfig.save(); });
            if (sMode == ConfiColorsConfig.SplashMode.COLOR) {
                y = buildColorPicker(sink, rebuild, innerLeft + 8, y, innerWidth - 8,
                                     ConfiColorsConfig.splashColor,
                                     c -> { ConfiColorsConfig.splashColor = c; ConfiColorsConfig.save(); });
            }
            y = buildRadio(sink, rebuild, innerLeft, y, innerWidth,
                           TranslationManager.get("confi.splash.image", "Imagen personalizada"),
                           sMode == ConfiColorsConfig.SplashMode.IMAGE,
                           () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.IMAGE; ConfiColorsConfig.save(); });
            if (sMode == ConfiColorsConfig.SplashMode.IMAGE) {
                String pathLabel = ConfiColorsConfig.splashImagePath.isEmpty()
                    ? TranslationManager.get("confi.image.none", "Sin imagen")
                    : shortenPath(ConfiColorsConfig.splashImagePath);
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15, Component.literal("Img: " + pathLabel), b -> {}, CompactButton.Type.MODULE));
                y += 17;
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.use_splash", "Usar splash.png")),
                        b -> {
                            String sp = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("nebulacrakmod/splash.png").toString();
                            ConfiColorsConfig.splashImagePath = sp;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY));
                y += 17;
                sink.add(new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.clear", "Quitar imagen")),
                        b -> {
                            ConfiColorsConfig.splashImagePath = "";
                            ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.DISABLED;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY));
                y += 17;
            }
        }
    }

    private static int buildSection(List<CompactButton> sink, Runnable rebuild,
                                    int left, int y, int width,
                                    java.util.function.Supplier<Boolean> isExpanded,
                                    java.util.function.Consumer<Boolean> setExpanded,
                                    String label) {
        boolean expanded = isExpanded.get();
        String arrow = expanded ? "v " : "> ";
        CompactButton header = new CompactButton(left, y, width, 18,
                Component.literal(arrow + label), b -> { setExpanded.accept(!isExpanded.get()); rebuild.run(); },
                CompactButton.Type.MASTER_HEADER);
        sink.add(header);
        return y + 20;
    }

    private static int buildRadio(List<CompactButton> sink, Runnable rebuild,
                                   int left, int y, int width,
                                   String label, boolean selected, Runnable onSelect) {
        String prefix = selected ? "(*) " : "( ) ";
        CompactButton btn = new CompactButton(left, y, width, 15,
                Component.literal(prefix + label), b -> { onSelect.run(); rebuild.run(); },
                CompactButton.Type.MODULE);
        btn.setActivated(selected);
        sink.add(btn);
        return y + 17;
    }

    private static int buildToggle(List<CompactButton> sink, Runnable rebuild,
                                    int left, int y, int width,
                                    String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        String state = value ? TranslationManager.get("confi.state.on", "ON") : TranslationManager.get("confi.state.off", "OFF");
        CompactButton btn = new CompactButton(left, y, width, 15,
                Component.literal(label + ": " + state), b -> { setter.accept(!value); rebuild.run(); },
                CompactButton.Type.MODULE);
        btn.setActivated(value);
        sink.add(btn);
        return y + 17;
    }

    private static int buildColorPicker(List<CompactButton> sink, Runnable rebuild,
                                         int left, int y, int width, int currentColor,
                                         java.util.function.Consumer<Integer> setter) {
        int swatchSize = 14;
        int gap = 3;
        int rowWidth = QUICK_COLORS.length * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);
        for (int i = 0; i < QUICK_COLORS.length; i++) {
            final int color = QUICK_COLORS[i];
            boolean isSelected = (currentColor == color);
            int sx = startX + i * (swatchSize + gap);
            CompactButton swatch = new CompactButton(sx, y, swatchSize, swatchSize, Component.empty(), b -> { setter.accept(color); rebuild.run(); }, CompactButton.Type.SETTINGS) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                    g.fill(x + 1, iy + 1, x + w - 1, iy + h - 1, color | 0xFF000000);
                    int border = isSelected ? 0xFFFFFFFF : 0xFF555565;
                    g.fill(x, iy, x + w, iy + 1, border);
                    g.fill(x, iy + h - 1, x + w, iy + h, border);
                    g.fill(x, iy + 1, x + 1, iy + h - 1, border);
                    g.fill(x + w - 1, iy + 1, x + w, iy + h - 1, border);
                    if (isSelected) g.fill(x + w / 2 - 1, iy + h / 2 - 1, x + w / 2 + 2, iy + h / 2 + 2, 0xFFFFFFFF);
                }
            };
            sink.add(swatch);
        }
        return y + swatchSize + 4;
    }

    private static String shortenPath(String path) {
        if (path == null || path.isEmpty()) return "";
        java.io.File f = new java.io.File(path);
        return f.getName();
    }
}
