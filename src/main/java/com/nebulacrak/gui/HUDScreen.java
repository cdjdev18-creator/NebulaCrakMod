package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.translation.TranslationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class HUDScreen {

    private static boolean fondoExpanded      = true;
    private static boolean panelExpanded      = true;
    private static boolean splashExpanded     = true;

    private static final int[] QUICK_COLORS = {
        0xFFEF4444, 0xFF22C55E, 0xFF3B82F6, 0xFF8B5CF6,
        0xFFF59E0B, 0xFF06B6D4, 0xFF64748B, 0xFFFFFFFF, 0xFF000000
    };

    public static void build(Screen parent, int left, int top, int width,
                             List<AbstractWidget> sink, Runnable rebuild) {
        int y = top;
        int innerLeft = left + 4;
        int innerWidth = width - 8;

        y = section(sink, rebuild, left, y, width, () -> fondoExpanded, v -> fondoExpanded = v, TranslationManager.get("confi.fondo.title", "Fondo"));
        if (fondoExpanded) {
            ConfiColorsConfig.FondoMode fMode = ConfiColorsConfig.fondoMode;
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.fondo.disabled", "Desactivado"), fMode == ConfiColorsConfig.FondoMode.DISABLED, () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.DISABLED; ConfiColorsConfig.save(); });
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.fondo.color", "Color sólido"), fMode == ConfiColorsConfig.FondoMode.COLOR, () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.COLOR; ConfiColorsConfig.save(); });
            if (fMode == ConfiColorsConfig.FondoMode.COLOR) y = colorPicker(sink, rebuild, innerLeft + 8, y, innerWidth - 8, ConfiColorsConfig.fondoColor, c -> { ConfiColorsConfig.fondoColor = c; ConfiColorsConfig.save(); });
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.fondo.image", "Imagen personalizada"), fMode == ConfiColorsConfig.FondoMode.IMAGE, () -> { ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.IMAGE; ConfiColorsConfig.save(); });
            if (fMode == ConfiColorsConfig.FondoMode.IMAGE) y = imageControls(sink, rebuild, innerLeft + 8, y, innerWidth - 8, ConfiColorsConfig.fondoImagePath, p -> { ConfiColorsConfig.fondoImagePath = p; ConfiColorsConfig.save(); }, () -> { ConfiColorsConfig.fondoImagePath = ""; ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.DISABLED; ConfiColorsConfig.save(); });
            y += 3;
        }

        y = section(sink, rebuild, left, y, width, () -> panelExpanded, v -> panelExpanded = v, TranslationManager.get("confi.panel.title", "PanelCargas"));
        if (panelExpanded) {
            y = toggle(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.panel.visible", "Visible"), ConfiColorsConfig.panelCargasVisible, v -> { ConfiColorsConfig.panelCargasVisible = v; ConfiColorsConfig.save(); });
            y = toggle(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.panel.transparent", "Transparente"), ConfiColorsConfig.panelCargasTransparent, v -> { ConfiColorsConfig.panelCargasTransparent = v; ConfiColorsConfig.save(); });
            sink.add(new CompactButton(innerLeft, y, innerWidth, 14, Component.literal(TranslationManager.get("confi.panel.color", "Color del panel:")), b -> {}, CompactButton.Type.CATEGORY));
            y += 16;
            y = colorPicker(sink, rebuild, innerLeft + 4, y, innerWidth - 4, ConfiColorsConfig.panelCargasColor, c -> { ConfiColorsConfig.panelCargasColor = c; ConfiColorsConfig.save(); });
            y += 3;
        }

        y = section(sink, rebuild, left, y, width, () -> splashExpanded, v -> splashExpanded = v, TranslationManager.get("confi.splash.title", "Splash"));
        if (splashExpanded) {
            ConfiColorsConfig.SplashMode sMode = ConfiColorsConfig.splashMode;
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.splash.disabled", "Desactivado"), sMode == ConfiColorsConfig.SplashMode.DISABLED, () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.DISABLED; ConfiColorsConfig.save(); });
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.splash.color", "Color sólido"), sMode == ConfiColorsConfig.SplashMode.COLOR, () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.COLOR; ConfiColorsConfig.save(); });
            if (sMode == ConfiColorsConfig.SplashMode.COLOR) y = colorPicker(sink, rebuild, innerLeft + 8, y, innerWidth - 8, ConfiColorsConfig.splashColor, c -> { ConfiColorsConfig.splashColor = c; ConfiColorsConfig.save(); });
            y = radio(sink, rebuild, innerLeft, y, innerWidth, TranslationManager.get("confi.splash.image", "Imagen personalizada"), sMode == ConfiColorsConfig.SplashMode.IMAGE, () -> { ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.IMAGE; ConfiColorsConfig.save(); });
            if (sMode == ConfiColorsConfig.SplashMode.IMAGE) y = imageControls(sink, rebuild, innerLeft + 8, y, innerWidth - 8, ConfiColorsConfig.splashImagePath, p -> { ConfiColorsConfig.splashImagePath = p; ConfiColorsConfig.save(); }, () -> { ConfiColorsConfig.splashImagePath = ""; ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.DISABLED; ConfiColorsConfig.save(); });
        }
    }

    private static int section(List<AbstractWidget> sink, Runnable rebuild, int left, int y, int width,
                               java.util.function.Supplier<Boolean> isExpanded, java.util.function.Consumer<Boolean> setExpanded, String label) {
        boolean expanded = isExpanded.get();
        String arrow = expanded ? "v " : "> ";
        sink.add(new CompactButton(left, y, width, 18, Component.literal(arrow + label), b -> { setExpanded.accept(!isExpanded.get()); rebuild.run(); }, CompactButton.Type.MASTER_HEADER));
        return y + 20;
    }

    private static int radio(List<AbstractWidget> sink, Runnable rebuild, int left, int y, int width, String label, boolean selected, Runnable onSelect) {
        CompactButton btn = new CompactButton(left, y, width, 15, Component.literal((selected ? "(*) " : "( ) ") + label), b -> { onSelect.run(); rebuild.run(); }, CompactButton.Type.MODULE);
        btn.setActivated(selected); sink.add(btn); return y + 17;
    }

    private static int toggle(List<AbstractWidget> sink, Runnable rebuild, int left, int y, int width, String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        String state = value ? TranslationManager.get("confi.state.on", "ON") : TranslationManager.get("confi.state.off", "OFF");
        CompactButton btn = new CompactButton(left, y, width, 15, Component.literal(label + ": " + state), b -> { setter.accept(!value); rebuild.run(); }, CompactButton.Type.MODULE);
        btn.setActivated(value); sink.add(btn); return y + 17;
    }

    private static int colorPicker(List<AbstractWidget> sink, Runnable rebuild, int left, int y, int width, int currentColor, java.util.function.Consumer<Integer> setter) {
        int swatchSize = 14, gap = 3;
        int rowWidth = QUICK_COLORS.length * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);
        for (int color : QUICK_COLORS) {
            final int c = color;
            boolean isSelected = (currentColor == c);
            int sx = startX;
            sink.add(new CompactButton(sx, y, swatchSize, swatchSize, Component.empty(), b -> { setter.accept(c); rebuild.run(); }, CompactButton.Type.SETTINGS) {
                @Override protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                    g.fill(x + 1, iy + 1, x + w - 1, iy + h - 1, c | 0xFF000000);
                    int border = isSelected ? 0xFFFFFFFF : 0xFF555565;
                    g.fill(x, iy, x + w, iy + 1, border); g.fill(x, iy + h - 1, x + w, iy + h, border);
                    g.fill(x, iy + 1, x + 1, iy + h - 1, border); g.fill(x + w - 1, iy + 1, x + w, iy + h - 1, border);
                    if (isSelected) g.fill(x + w / 2 - 1, iy + h / 2 - 1, x + w / 2 + 2, iy + h / 2 + 2, 0xFFFFFFFF);
                }
            });
            startX += swatchSize + gap;
        }
        y += swatchSize + 4;
        sink.add(new ColorInputButton(left, y, width, 22, currentColor, c -> { setter.accept(c); rebuild.run(); }));
        y += 26;
        return y;
    }

    private static int imageControls(List<AbstractWidget> sink, Runnable rebuild, int left, int y, int width, String currentPath, Consumer<String> onSet, Runnable onClear) {
        String pathLabel = (currentPath == null || currentPath.isEmpty()) ? TranslationManager.get("confi.image.none", "Sin imagen") : shortenPath(currentPath);
        sink.add(new CompactButton(left, y, width, 15, Component.literal("Img: " + pathLabel), b -> {}, CompactButton.Type.MODULE));
        y += 17;
        sink.add(new CompactButton(left, y, width, 20, Component.literal(TranslationManager.get("confi.image.browse", "Buscar imagen...")), b -> openFileDialog(onSet, rebuild), CompactButton.Type.SETTINGS));
        y += 22;
        sink.add(new CompactButton(left, y, width, 20, Component.literal(TranslationManager.get("confi.image.replace", "Reemplazar imagen...")), b -> openFileDialog(onSet, rebuild), CompactButton.Type.SETTINGS));
        y += 22;
        sink.add(new CompactButton(left, y, width, 20, Component.literal(TranslationManager.get("confi.image.clear", "Quitar imagen")), b -> { onClear.run(); rebuild.run(); }, CompactButton.Type.DANGER));
        y += 22;
        return y;
    }

    private static void openFileDialog(Consumer<String> callback, Runnable rebuild) {
        ImageFileUtil.openPngPicker(
                TranslationManager.get("confi.image.select", "Seleccionar imagen PNG"),
                path -> { callback.accept(path); rebuild.run(); },
                err -> ImageFileUtil.notifyClient(err)
        );
    }

    private static String shortenPath(String path) {
        if (path == null || path.isEmpty()) return "";
        return new File(path).getName();
    }
}
