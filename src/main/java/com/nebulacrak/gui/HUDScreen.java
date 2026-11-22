package com.nebulacrak.gui;

import com.nebulacrak.config.ConfiColorsConfig;
import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.translation.TranslationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractWidget;
import net.minecraft.client.input.mouse.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

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

/**
 * Panel de contenido de HUD — se llama desde NebulaGUIScreen
 * cuando la pestaña activa es HUD.
 *
 * Permite personalizar:
 * - Fondo de la interfaz (Original, Color sólido, Imagen personalizada)
 * - Paneles (Visibilidad, Transparencia, Color)
 * - Splash (Color, Imagen)
 */
public class HUDScreen {

    // Secciones expandibles
    private static boolean fondoExpanded      = true;
    private static boolean panelExpanded      = true;
    private static boolean splashExpanded     = true;

    // Paleta de colores rápidos
    private static final int[] QUICK_COLORS = {
        0xFFEF4444, // rojo
        0xFF22C55E, // verde
        0xFF3B82F6, // azul
        0xFFF59E0B, // ámbar
        0xFF8B5CF6, // violeta
        0xFFEC4899, // rosa
        0xFF06B6D4, // cian
        0xFFF97316, // naranja
        0xFF64748B  // slate
    };

    /**
     * Construye y devuelve la lista de botones/widgets para el panel HUD.
     * El caller (NebulaGUIScreen) los añade al árbol de widgets.
     */
    public static void build(Screen parent, int left, int top, int width,
                             List<AbstractWidget> widgets, Runnable rebuild) {
        int y = top + 4;

        // ── SECCIÓN 1: FONDO DE INTERFAZ ──────────────────────────────────────
        y = buildSectionHeader(widgets, rebuild, left, y, width,
                TranslationManager.get("gui.nebula.hud.fondo.title", "Fondo de Interfaz"),
                "fondo", fondoExpanded,
                v -> { fondoExpanded = v; rebuild.run(); });

        if (fondoExpanded) {
            // Selector de modo
            String[] fondoModes = {
                TranslationManager.get("gui.nebula.hud.fondo.mode.original", "Original"),
                TranslationManager.get("gui.nebula.hud.fondo.mode.color", "Color Sólido"),
                TranslationManager.get("gui.nebula.hud.fondo.mode.image", "Imagen Personalizada")
            };
            y = buildModeSelector(widgets, rebuild, left, y, width,
                    ConfiColorsConfig.fondoMode.ordinal(), fondoModes,
                    idx -> {
                        ConfiColorsConfig.fondoMode = ConfiColorsConfig.FondoMode.values()[idx];
                        if (idx == 1) { // COLOR mode - open color picker
                            // Color picker handled by button
                        }
                        ConfiColorsConfig.save();
                        rebuild.run();
                    });
            y += 2;

            // Controles según modo
            if (ConfiColorsConfig.fondoMode == ConfiColorsConfig.FondoMode.COLOR) {
                y = buildColorPicker(widgets, rebuild, left, y, width,
                        ConfiColorsConfig.fondoColor,
                        c -> { ConfiColorsConfig.fondoColor = c; ConfiColorsConfig.save(); });
            } else if (ConfiColorsConfig.fondoMode == ConfiColorsConfig.FondoMode.IMAGE) {
                y = buildImageSelector(widgets, rebuild, left, y, width,
                        "fondo",
                        ConfiColorsConfig.fondoImagePath,
                        path -> {
                            ConfiColorsConfig.fondoImagePath = path;
                            if (path != null && !path.isEmpty()) {
                                try {
                                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                                    ConfiColorsConfig.fondoImageBase64 = Base64.getEncoder().encodeToString(bytes);
                                } catch (Exception e) {
                                    ConfiColorsConfig.fondoImageBase64 = "";
                                }
                            }
                            ConfiColorsConfig.save();
                            rebuild.run();
                        },
                        () -> {
                            ConfiColorsConfig.fondoImagePath = "";
                            ConfiColorsConfig.fondoImageBase64 = "";
                            ConfiColorsConfig.save();
                            rebuild.run();
                        });
            }
        }

        y += 4;

        // ── SECCIÓN 2: PANELES ────────────────────────────────────────────────
        y = buildSectionHeader(widgets, rebuild, left, y, width,
                TranslationManager.get("gui.nebula.hud.panels.title", "Paneles"),
                "panel", panelExpanded,
                v -> { panelExpanded = v; rebuild.run(); });

        if (panelExpanded) {
            // Visibilidad
            y = buildToggle(widgets, rebuild, left, y, width,
                    TranslationManager.get("gui.nebula.hud.panels.visible", "Visibilidad"),
                    ConfiColorsConfig.panelCargasVisible,
                    v -> { ConfiColorsConfig.panelCargasVisible = v; ConfiColorsConfig.save(); });

            // Transparencia (solo si visible)
            if (ConfiColorsConfig.panelCargasVisible) {
                y = buildToggle(widgets, rebuild, left, y, width,
                        TranslationManager.get("gui.nebula.hud.panels.transparent", "Transparente"),
                        ConfiColorsConfig.panelCargasTransparent,
                        v -> { ConfiColorsConfig.panelCargasTransparent = v; ConfiColorsConfig.save(); });

                // Color de panel
                y = buildColorPicker(widgets, rebuild, left, y, width,
                        ConfiColorsConfig.panelCargasColor,
                        c -> { ConfiColorsConfig.panelCargasColor = c; ConfiColorsConfig.save(); });
            }
        }

        y += 4;

        // ── SECCIÓN 3: SPLASH ─────────────────────────────────────────────────
        y = buildSectionHeader(widgets, rebuild, left, y, width,
                TranslationManager.get("gui.nebula.hud.splash.title", "Pantalla de Carga (Splash)"),
                "splash", splashExpanded,
                v -> { splashExpanded = v; rebuild.run(); });

        if (splashExpanded) {
            String[] splashModes = {
                TranslationManager.get("gui.nebula.hud.splash.mode.disabled", "Desactivado"),
                TranslationManager.get("gui.nebula.hud.splash.mode.color", "Color Sólido"),
                TranslationManager.get("gui.nebula.hud.splash.mode.image", "Imagen Personalizada")
            };
            y = buildModeSelector(widgets, rebuild, left, y, width,
                    ConfiColorsConfig.splashMode.ordinal(), splashModes,
                    idx -> {
                        ConfiColorsConfig.splashMode = ConfiColorsConfig.SplashMode.values()[idx];
                        ConfiColorsConfig.save();
                        rebuild.run();
                    });
            y += 2;

            if (ConfiColorsConfig.splashMode == ConfiColorsConfig.SplashMode.COLOR) {
                y = buildColorPicker(widgets, rebuild, left, y, width,
                        ConfiColorsConfig.splashColor,
                        c -> { ConfiColorsConfig.splashColor = c; ConfiColorsConfig.save(); });
            } else if (ConfiColorsConfig.splashMode == ConfiColorsConfig.SplashMode.IMAGE) {
                y = buildImageSelector(widgets, rebuild, left, y, width,
                        "splash",
                        ConfiColorsConfig.splashImagePath,
                        path -> {
                            ConfiColorsConfig.splashImagePath = path;
                            if (path != null && !path.isEmpty()) {
                                try {
                                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                                    ConfiColorsConfig.splashImageBase64 = Base64.getEncoder().encodeToString(bytes);
                                } catch (Exception e) {
                                    ConfiColorsConfig.splashImageBase64 = "";
                                }
                            }
                            ConfiColorsConfig.save();
                            rebuild.run();
                        },
                        () -> {
                            ConfiColorsConfig.splashImagePath = "";
                            ConfiColorsConfig.splashImageBase64 = "";
                            ConfiColorsConfig.save();
                            rebuild.run();
                        });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HELPERS DE CONSTRUCCIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    private static int buildSectionHeader(List<AbstractWidget> sink, Runnable rebuild,
                                           int left, int y, int width,
                                           String title, String key, boolean expanded,
                                           Consumer<Boolean> toggle) {
        // Botón de sección (expandible)
        CompactButton sectionBtn = new CompactButton(left, y, width, 20,
                Component.literal((expanded ? "▼ " : "▶ ") + title),
                b -> toggle.accept(!expanded),
                CompactButton.Type.SECTION) {
            @Override
            protected void renderWidget(DrawContext g, int mx, int my, float pt) {
                int x = getX(), iy = getY(), w = getWidth(), h = getHeight();
                // Fondo
                g.fill(x, iy, x + w, iy + h, 0xFF1E1E2A);
                // Texto
                String text = (expanded ? "▼ " : "▶ ") + title;
                int tx = x + 8;
                int ty = iy + (h - 8) / 2;
                g.drawText(MinecraftClient.getInstance().textRenderer, text, tx, ty, 0xFFFFFFFF, false);
            }
        };
        sink.add(sectionBtn);
        return y + 22;
    }

    private static int buildToggle(List<AbstractWidget> sink, Runnable rebuild,
                                          int left, int y, int width,
                                          String label, boolean value,
                                          Consumer<Boolean> setter) {
        int btnW = 40, btnH = 18;
        int btnX = left + width - btnW - 4;

        CompactButton btn = new CompactButton(btnX, y, btnW, btnH,
                Component.literal(value ? TranslationManager.get("gui.common.on", "On")
                                           : TranslationManager.get("gui.common.off", "Off")),
                b -> { setter.accept(!value); rebuild.run(); },
                value ? CompactButton.Type.MODULE : CompactButton.Type.MODULE);
        btn.setActivated(value);
        sink.add(btn);

        // Label
        sink.add(new CompactButton(left, y, width - btnW - 8, btnH,
                Component.literal(label), b -> {}, CompactButton.Type.LABEL));

        return y + 20;
    }

    private static int buildModeSelector(List<AbstractWidget> sink, Runnable rebuild,
                                            int left, int y, int width,
                                            int current, String[] options,
                                            Consumer<Integer> setter) {
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            boolean selected = (current == idx);
            CompactButton btn = new CompactButton(left + 4, y, width - 8, 20,
                    Component.literal(options[i]),
                    b -> { setter.accept(idx); },
                    selected ? CompactButton.Type.SELECTED : CompactButton.Type.SETTINGS);
            sink.add(btn);
            y += 22;
        }
        return y;
    }

    private static int buildColorPicker(List<AbstractWidget> sink, Runnable rebuild,
                                           int left, int y, int width,
                                           int currentColor,
                                           Consumer<Integer> setter) {
        int swatchSize = 14;
        int gap = 3;
        int totalSwatches = QUICK_COLORS.length;
        int rowWidth = totalSwatches * (swatchSize + gap) - gap;
        int startX = left + Math.max(0, (width - rowWidth) / 2);

        for (int i = 0; i < totalSwatches; i++) {
            final int color = QUICK_COLORS[i];
            boolean isSelected = (currentColor == color);
            int sx = startX + i * (swatchSize + gap);

            CompactButton swatch = new CompactButton(sx, y, swatchSize, swatchSize,
                    Component.empty(), b -> { setter.accept(color); rebuild.run(); },
                    CompactButton.Type.SETTINGS) {
                @Override
                protected void renderWidget(DrawContext g, int mx, int my, float pt) {
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
            sink.add(swatch);
        }
        y += swatchSize + 8;

        // HEX/RGB Color Input
        String hexLabel = TranslationManager.get("gui.nebula.hud.color.hex", "HEX: #RRGGBBAA");
        ColorInputButton hexInput = new ColorInputButton(left + 4, y, width - 8, 22, currentColor, c -> {
            setter.accept(c);
            rebuild.run();
        });
        sink.add(hexInput);
        y += 26;

        return y;
    }

    private static int buildImageSelector(List<AbstractWidget> sink, Runnable rebuild,
                                           int left, int y, int width,
                                           String type, // "fondo" o "splash"
                                           String currentPath,
                                           Consumer<String> onSelect,
                                           Runnable onClear) {
        // Botón Browse
        CompactButton browseBtn = new CompactButton(left + 4, y, width - 8, 22,
                Component.literal(TranslationManager.get("gui.nebula.hud.image.browse", "Seleccionar Imagen")),
                b -> openFileChooser(onSelect),
                CompactButton.Type.SETTINGS);
        sink.add(browseBtn);
        y += 26;

        // Vista previa / ruta actual
        if (currentPath != null && !currentPath.isEmpty()) {
            String displayPath = shortenPath(currentPath);
            CompactButton previewBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.current", displayPath)),
                    b -> openImageInViewer(currentPath),
                    CompactButton.Type.SETTINGS);
            sink.add(previewBtn);
            y += 26;

            // Botón eliminar
            CompactButton clearBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.remove", "Eliminar Imagen")),
                    b -> onClear.run(),
                    CompactButton.Type.DANGER);
            sink.add(clearBtn);
            y += 26;
        } else {
            CompactButton noneBtn = new CompactButton(left + 4, y, width - 8, 22,
                    Component.literal(TranslationManager.get("gui.nebula.hud.image.none", "Sin imagen seleccionada")),
                    b -> {}, CompactButton.Type.DISABLED);
            sink.add(noneBtn);
            y += 26;
        }

        return y;
    }

    private static void openFileChooser(Consumer<String> callback) {
        // Usar AWT FileDialog en el hilo de Swing/AWT
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
                    // Validar que es una imagen válida
                    Path path = Paths.get(fullPath);
                    if (Files.exists(path) && Files.size(path) > 0) {
                        // Copiar a la carpeta del mod para persistencia
                        String savedPath = copyImageToModFolder(path);
                        if (savedPath != null) {
                            MinecraftClient.getInstance().execute(() -> callback.accept(savedPath));
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback: usar JFileChooser si FileDialog falla
                MinecraftClient.getInstance().execute(() -> {
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
                        // Silencioso
                    }
                });
            }
        }).start();
    }

    private static String copyImageToModFolder(Path source) {
        try {
            Path modDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                    .resolve("nebulacrakmod").resolve("images");
            Files.createDirectories(modDir);

            String fileName = source.getFileName().toString();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String ext = fileName.substring(fileName.lastIndexOf('.'));
            Path dest = modDir.resolve(baseName + "_" + System.currentTimeMillis() + ext);

            Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void openImageInViewer(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException e) {
            // Silencioso
        }
    }

    private static String shortenPath(String path) {
        if (path == null || path.isEmpty()) return "";
        File f = new File(path);
        return f.getName();
    }
}