package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.translation.TranslationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de contenido de ConfiColors — se llama desde NebulaGUIScreen
 * cuando la pestaña activa es CONFI_COLORS.
 *
 * Renderiza tres secciones: Fondo · PanelCargas · Splash.
 * Devuelve la lista de botones al caller para que los registre
 * correctamente en el widget tree.
 */
public class ConfiColorsScreen {

    // Secciones expandibles
    private static boolean fondoExpanded      = true;
    private static boolean panelExpanded      = true;
    private static boolean splashExpanded     = true;

    // ── Paleta de colores rápidos ─────────────────────────────────────────────
    private static final int[] QUICK_COLORS = {
        0xFFEF4444, // rojo
        0xFF22C55E, // verde
        0xFF3B82F6, // azul
        0xFF8B5CF6, // púrpura
        0xFFF59E0B, // ámbar
        0xFF06B6D4, // cyan
        0xFF64748B, // gris
        0xFFFFFFFF, // blanco
        0xFF000000, // negro
    };

    /**
     * Construye y retorna todos los botones del panel ConfiColors.
     *
     * @param screen   pantalla padre (para addRenderableWidget)
     * @param left     X inicio del panel de contenido
     * @param startY   Y de partida (ya incluye offset de scroll)
     * @param width    ancho disponible
     * @param sink     lista donde depositar los botones creados
     */
    public static void build(Screen screen, int left, int startY, int width,
                             List<CompactButton> sink, Runnable rebuild) {
        int y = startY;
        int innerLeft  = left + 4;
        int innerWidth = width - 8;

        // ═══════════════════════════════════════════
        //  SECCIÓN: FONDO
        // ═══════════════════════════════════════════
        y = buildSection(sink, rebuild, left, y, width, innerLeft, innerWidth,
                         "fondo_section",
                         () -> fondoExpanded,
                         v  -> fondoExpanded = v,
                         TranslationManager.get("confi.fondo.title", "Fondo"));

        if (fondoExpanded) {
            // --- Modo: DISABLED ---
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
                // Info path actual
                String pathLabel = ConfiColorsConfig.fondoImagePath.isEmpty()
                    ? TranslationManager.get("confi.image.none", "Sin imagen")
                    : shortenPath(ConfiColorsConfig.fondoImagePath);

                CompactButton pathBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal("🖼 " + pathLabel), b -> {}, CompactButton.Type.MODULE);
                sink.add(pathBtn);
                y += 17;

                // Nota: selección de archivo mediante teclado/diálogo no soportado en MC vanilla.
                // Se muestra instrucción al usuario.
                CompactButton helpBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.help",
                                "Coloca imagen en config/nebulacrakmod/bg.png")),
                        b -> {}, CompactButton.Type.MODULE);
                sink.add(helpBtn);
                y += 17;

                // Botón: usar bg.png
                CompactButton setBgBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.use_default", "Usar bg.png")),
                        b -> {
                            String bgPath = net.fabricmc.loader.api.FabricLoader.getInstance()
                                    .getConfigDir().resolve("nebulacrakmod/bg.png").toString();
                            ConfiColorsConfig.fondoImagePath = bgPath;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY);
                sink.add(setBgBtn);
                y += 17;

                // Botón: quitar imagen
                CompactButton clearBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.clear", "Quitar imagen")),
                        b -> {
                            ConfiColorsConfig.fondoImagePath = "";
                            ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.DISABLED;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY);
                sink.add(clearBtn);
                y += 17;
            }
            y += 3;
        }

        // ═══════════════════════════════════════════
        //  SECCIÓN: PANEL CARGAS
        // ═══════════════════════════════════════════
        y = buildSection(sink, rebuild, left, y, width, innerLeft, innerWidth,
                         "panel_section",
                         () -> panelExpanded,
                         v  -> panelExpanded = v,
                         TranslationManager.get("confi.panel.title", "PanelCargas"));

        if (panelExpanded) {
            // Visible ON/OFF
            y = buildToggle(sink, rebuild, innerLeft, y, innerWidth,
                            TranslationManager.get("confi.panel.visible", "Visible"),
                            ConfiColorsConfig.panelCargasVisible,
                            v -> { ConfiColorsConfig.panelCargasVisible = v; ConfiColorsConfig.save(); });

            // Transparente ON/OFF
            y = buildToggle(sink, rebuild, innerLeft, y, innerWidth,
                            TranslationManager.get("confi.panel.transparent", "Transparente"),
                            ConfiColorsConfig.panelCargasTransparent,
                            v -> { ConfiColorsConfig.panelCargasTransparent = v; ConfiColorsConfig.save(); });

            // Color
            CompactButton colorLabel = new CompactButton(innerLeft, y, innerWidth, 14,
                    Component.literal(TranslationManager.get("confi.panel.color", "Color del panel:")),
                    b -> {}, CompactButton.Type.CATEGORY);
            sink.add(colorLabel);
            y += 16;

            y = buildColorPicker(sink, rebuild, innerLeft + 4, y, innerWidth - 4,
                                 ConfiColorsConfig.panelCargasColor,
                                 c -> { ConfiColorsConfig.panelCargasColor = c; ConfiColorsConfig.save(); });
            y += 3;
        }

        // ═══════════════════════════════════════════
        //  SECCIÓN: SPLASH
        // ═══════════════════════════════════════════
        y = buildSection(sink, rebuild, left, y, width, innerLeft, innerWidth,
                         "splash_section",
                         () -> splashExpanded,
                         v  -> splashExpanded = v,
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

                CompactButton pathBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal("🖼 " + pathLabel), b -> {}, CompactButton.Type.MODULE);
                sink.add(pathBtn);
                y += 17;

                CompactButton setSplashBtn = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.use_splash", "Usar splash.png")),
                        b -> {
                            String sp = net.fabricmc.loader.api.FabricLoader.getInstance()
                                    .getConfigDir().resolve("nebulacrakmod/splash.png").toString();
                            ConfiColorsConfig.splashImagePath = sp;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY);
                sink.add(setSplashBtn);
                y += 17;

                CompactButton clearSplash = new CompactButton(innerLeft + 8, y, innerWidth - 8, 15,
                        Component.literal(TranslationManager.get("confi.image.clear", "Quitar imagen")),
                        b -> {
                            ConfiColorsConfig.splashImagePath = "";
                            ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.DISABLED;
                            ConfiColorsConfig.save();
                            rebuild.run();
                        }, CompactButton.Type.CATEGORY);
                sink.add(clearSplash);
                y += 17;
            }
            y += 3;
        }
    }

    // ── Helpers privados de construcción de widgets ───────────────────────────

    /** Encabezado de sección desplegable. Retorna Y actualizada. */
    private static int buildSection(List<CompactButton> sink, Runnable rebuild,
                                    int left, int y, int width,
                                    int innerLeft, int innerWidth,
                                    String id,
                                    java.util.function.Supplier<Boolean> isExpanded,
                                    java.util.function.Consumer<Boolean> setExpanded,
                                    String label) {
        boolean expanded = isExpanded.get();
        String arrow = expanded ? "▼ " : "▶ ";
        CompactButton header = new CompactButton(left, y, width, 18,
                Component.literal(arrow + label), b -> {
                    setExpanded.accept(!isExpanded.get());
                    rebuild.run();
                }, CompactButton.Type.MASTER_HEADER);
        sink.add(header);
        return y + 20;
    }

    /** Radio button (selección única). Retorna Y actualizada. */
    private static int buildRadio(List<CompactButton> sink, Runnable rebuild,
                                   int left, int y, int width,
                                   String label, boolean selected,
                                   Runnable onSelect) {
        String prefix = selected ? "● " : "○ ";
        CompactButton btn = new CompactButton(left, y, width, 15,
                Component.literal(prefix + label), b -> { onSelect.run(); rebuild.run(); },
                CompactButton.Type.MODULE);
        btn.setActivated(selected);
        sink.add(btn);
        return y + 17;
    }

    /** Toggle ON/OFF. Retorna Y actualizada. */
    private static int buildToggle(List<CompactButton> sink, Runnable rebuild,
                                    int left, int y, int width,
                                    String label, boolean value,
                                    java.util.function.Consumer<Boolean> setter) {
        String state = value
            ? TranslationManager.get("confi.state.on", "ON")
            : TranslationManager.get("confi.state.off", "OFF");
        String text = label + ": " + state;
        CompactButton btn = new CompactButton(left, y, width, 15,
                Component.literal(text), b -> { setter.accept(!value); rebuild.run(); },
                CompactButton.Type.MODULE);
        btn.setActivated(value);
        sink.add(btn);
        return y + 17;
    }

    /**
     * Color picker con 9 colores rápidos en una fila.
     * Retorna Y actualizada.
     */
    private static int buildColorPicker(List<CompactButton> sink, Runnable rebuild,
                                         int left, int y, int width,
                                         int currentColor,
                                         java.util.function.Consumer<Integer> setter) {
        int swatchSize = 14;
        int gap = 3;
        int totalSwatches = QUICK_COLORS.length;
        int rowWidth = totalSwatches * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);

        for (int i = 0; i < totalSwatches; i++) {
            final int color = QUICK_COLORS[i];
            boolean isSelected = (currentColor == color);
            int sx = startX + i * (swatchSize + gap);

            // Creamos un botón tipo SETTINGS que actúa como swatch de color
            CompactButton swatch = new CompactButton(sx, y, swatchSize, swatchSize,
                    Component.empty(), b -> { setter.accept(color); rebuild.run(); },
                    CompactButton.Type.SETTINGS) {
                @Override
                protected void renderWidget(net.minecraft.client.gui.GuiGraphics g,
                                            int mx, int my, float pt) {
                    int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                    // Fondo del swatch con el color real
                    g.fill(x + 1, iy + 1, x + w - 1, iy + h - 1, color | 0xFF000000);
                    // Borde: blanco si seleccionado, gris si no
                    int border = isSelected ? 0xFFFFFFFF : 0xFF555565;
                    g.fill(x, iy, x + w, iy + 1, border);
                    g.fill(x, iy + h - 1, x + w, iy + h, border);
                    g.fill(x, iy + 1, x + 1, iy + h - 1, border);
                    g.fill(x + w - 1, iy + 1, x + w, iy + h - 1, border);
                    // Tick si está seleccionado
                    if (isSelected) {
                        g.fill(x + w / 2 - 1, iy + h / 2 - 1, x + w / 2 + 2, iy + h / 2 + 2, 0xFFFFFFFF);
                    }
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
