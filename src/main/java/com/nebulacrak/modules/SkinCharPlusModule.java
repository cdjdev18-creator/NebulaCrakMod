package com.nebulacrak.modules;

import com.nebulacrak.gui.ImageFileUtil;
import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.gui.NebulaTextureManager;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SkinCharPlusModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/SkinChar+");
    public static SkinCharPlusModule INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("SpeedChangeMulti");
    private final SettingGroup sgStatus = settings.createGroup("Status");

    private final Setting<String> mainSkinPath = sgGeneral.add(new StringSetting.Builder()
        .name("main-skin-path").description("Ruta PNG de skin principal (64x64, 64x32 o 128x128).").defaultValue("").build());
    private final Setting<String> extraSkin1 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-1").description("Skin adicional 1").defaultValue("").build());
    private final Setting<String> extraSkin2 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-2").description("Skin adicional 2").defaultValue("").build());
    private final Setting<String> extraSkin3 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-3").description("Skin adicional 3").defaultValue("").build());
    private final Setting<String> extraSkin4 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-4").description("Skin adicional 4").defaultValue("").build());

    private final Setting<Boolean> selectMainSkin = sgGeneral.add(new BoolSetting.Builder().name("select-main-skin").description("Abre selector PNG para skin principal.").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(mainSkinPath, "Seleccionar skin principal"); resetBool("select-main-skin"); } }).build());
    private final Setting<Boolean> selectExtra1 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-1").description("Seleccionar skin adicional 1").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin1, "Seleccionar skin adicional 1"); resetBool("select-extra-1"); } }).build());
    private final Setting<Boolean> selectExtra2 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-2").description("Seleccionar skin adicional 2").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin2, "Seleccionar skin adicional 2"); resetBool("select-extra-2"); } }).build());
    private final Setting<Boolean> selectExtra3 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-3").description("Seleccionar skin adicional 3").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin3, "Seleccionar skin adicional 3"); resetBool("select-extra-3"); } }).build());
    private final Setting<Boolean> selectExtra4 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-4").description("Seleccionar skin adicional 4").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin4, "Seleccionar skin adicional 4"); resetBool("select-extra-4"); } }).build());

    private final Setting<Boolean> clearMain = sgGeneral.add(new BoolSetting.Builder().name("clear-main-skin").description("Quitar skin principal").defaultValue(false)
        .onChanged(v -> { if (v) { mainSkinPath.set(""); info("Skin principal eliminada."); resetBool("clear-main-skin"); } }).build());
    private final Setting<Boolean> clearExtra1 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-1").description("Quitar skin adicional 1").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin1.set(""); resetBool("clear-extra-1"); } }).build());
    private final Setting<Boolean> clearExtra2 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-2").description("Quitar skin adicional 2").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin2.set(""); resetBool("clear-extra-2"); } }).build());
    private final Setting<Boolean> clearExtra3 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-3").description("Quitar skin adicional 3").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin3.set(""); resetBool("clear-extra-3"); } }).build());
    private final Setting<Boolean> clearExtra4 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-4").description("Quitar skin adicional 4").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin4.set(""); resetBool("clear-extra-4"); } }).build());

    private final Setting<Boolean> applyMainLocal = sgGeneral.add(new BoolSetting.Builder().name("apply-main-local").description("Aplica la skin principal localmente al jugador.").defaultValue(false)
        .onChanged(v -> { if (v) { applyMainPreview(); resetBool("apply-main-local"); } }).build());
    private final Setting<Boolean> restoreOriginal = sgGeneral.add(new BoolSetting.Builder().name("restore-original").description("Vuelve a la skin original local.").defaultValue(false)
        .onChanged(v -> { if (v) { restoreOriginalSkin(); resetBool("restore-original"); } }).build());

    private final Setting<Boolean> speedChangeMulti = sgSpeed.add(new BoolSetting.Builder().name("enabled").description("Alterna entre skin principal y skins adicionales configuradas.").defaultValue(false)
        .onChanged(v -> { if (v) startRotation(); else stopRotation(true); }).build());
    private final Setting<Double> intervalSeconds = sgSpeed.add(new DoubleSetting.Builder().name("interval-seconds").description("Tiempo entre cambios de skin (segundos).").defaultValue(0.5).min(0.2).max(10.0).sliderRange(0.2, 10.0).build());

    private final Setting<String> syncStatus = sgStatus.add(new StringSetting.Builder().name("sync-status").description("Estado de visibilidad: LOCAL / SERVIDOR COMPATIBLE / NO COMPATIBLE.").defaultValue("LOCAL").visible(() -> false).build());

    private final List<String> rotation = new ArrayList<>();
    private int rotationIndex = 0;
    private long nextChangeMs = 0L;
    private String lastApplied = "";
    private Supplier<PlayerSkin> originalSkinLookup;
    private boolean originalCaptured;

    public SkinCharPlusModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "SkinChar+", "Skins locales con validación. Visibilidad remota requiere servidor compatible; 6b6t usa /skin y no se envía automáticamente.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        captureOriginalIfNeeded();
    }

    @Override
    public void onDeactivate() {
        stopRotation(true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;
        captureOriginalIfNeeded();
        if (!speedChangeMulti.get()) return;
        if (rotation.isEmpty()) buildRotation();
        if (rotation.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now < nextChangeMs) return;
        String path = rotation.get(rotationIndex % rotation.size());
        rotationIndex = (rotationIndex + 1) % rotation.size();
        nextChangeMs = now + (long) (intervalSeconds.get() * 1000.0);
        applySkin(path, true, false);
    }

    private void startRotation() {
        buildRotation();
        rotationIndex = 0;
        nextChangeMs = 0L;
        if (rotation.isEmpty()) {
            info("SpeedChangeMulti: no hay skins válidas configuradas.");
            speedChangeMulti.set(false);
            return;
        }
        info("SpeedChangeMulti activado. Intervalo: " + String.format("%.2f", intervalSeconds.get()) + "s. Estado: LOCAL (sincronización remota requiere servidor compatible).");
    }

    private void stopRotation(boolean restoreMain) {
        rotation.clear();
        rotationIndex = 0;
        nextChangeMs = 0L;
        if (restoreMain) {
            String main = sanitizePath(mainSkinPath.get());
            if (!main.isBlank()) applySkin(main, true, false);
            else restoreOriginalSkin();
        }
    }

    private void buildRotation() {
        rotation.clear();
        addValidSkin(mainSkinPath.get());
        addValidSkin(extraSkin1.get());
        addValidSkin(extraSkin2.get());
        addValidSkin(extraSkin3.get());
        addValidSkin(extraSkin4.get());
    }

    private void addValidSkin(String path) {
        String p = sanitizePath(path);
        if (p.isBlank()) return;
        try {
            String err = ImageFileUtil.validateSkinPng(Path.of(p));
            if (err == null) rotation.add(p);
        } catch (Exception e) {
            LOG.warn("[SkinChar+] Invalid skin path {}", p, e);
        }
    }

    private void pickSkin(Setting<String> target, String title) {
        ImageFileUtil.openPngPicker(title, selected -> {
            String clean = sanitizePath(selected);
            String err = ImageFileUtil.validateSkinPng(Path.of(clean));
            if (err != null) { info(err); return; }
            target.set(clean);
            info("Skin seleccionada: " + new File(clean).getName());
            if (target == mainSkinPath) applySkin(clean, true, true);
        }, this::info);
    }

    private void applyMainPreview() {
        String main = sanitizePath(mainSkinPath.get());
        if (main.isBlank()) { info("No hay skin principal seleccionada."); return; }
        applySkin(main, true, true);
    }

    private void applySkin(String path, boolean localApply, boolean notify) {
        String p = sanitizePath(path);
        if (p.isBlank()) return;
        String err = ImageFileUtil.validateSkinPng(Path.of(p));
        if (err != null) { info("Skin inválida: " + err); return; }
        if (p.equals(lastApplied) && localApply) {
            if (notify) info("Skin ya aplicada localmente: " + new File(p).getName());
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            try {
                captureOriginalIfNeeded();
                Identifier tex = NebulaTextureManager.loadTextureFromFile("skinchar_current", p);
                if (tex == null) { info("No se pudo cargar la textura de skin."); return; }
                PlayerModelType model = detectModel(p);
                boolean applied = localApply && applyLocalSkin(mc, tex, model);
                lastApplied = p;
                syncStatus.set("LOCAL");
                if (notify) {
                    if (applied) info("Skin aplicada localmente: " + new File(p).getName() + " (" + model.name().toLowerCase() + "). Otros jugadores NO la verán sin soporte de servidor (6b6t: usar /skin según reglas del servidor; no se envía automáticamente).");
                    else info("Skin validada/cargada: " + new File(p).getName() + ". No se pudo aplicar localmente en esta sesión.");
                }
            } catch (Throwable t) {
                LOG.error("[SkinChar+] Failed to apply skin {}", p, t);
                info("Error aplicando skin: " + t.getClass().getSimpleName());
            }
        });
    }

    private boolean applyLocalSkin(Minecraft mc, Identifier tex, PlayerModelType model) {
        try {
            if (mc.player == null || mc.getConnection() == null) return false;
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info == null) return false;
            PlayerSkin old = info.getSkin();
            PlayerSkin custom = new PlayerSkin(new ClientAsset.ResourceTexture(tex), null, null, model, old.secure());
            Field f = PlayerInfo.class.getDeclaredField("skinLookup");
            f.setAccessible(true);
            f.set(info, (Supplier<PlayerSkin>) () -> custom);
            return true;
        } catch (Throwable t) {
            LOG.warn("[SkinChar+] Local skin apply failed", t);
            return false;
        }
    }

    private void captureOriginalIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) return;
        try {
            Field f = PlayerInfo.class.getDeclaredField("skinLookup");
            f.setAccessible(true);
            Object val = f.get(info);
            if (!originalCaptured && val instanceof Supplier<?> s) {
                originalSkinLookup = (Supplier<PlayerSkin>) s;
                originalCaptured = true;
            }
        } catch (Throwable ignored) {}
    }

    private void restoreOriginalSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) { info("No se puede restaurar: Minecraft no disponible."); return; }
        mc.execute(() -> {
            try {
                if (mc.player == null || mc.getConnection() == null) { info("No se puede restaurar: jugador no disponible."); return; }
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info == null) { info("No se puede restaurar: PlayerInfo no disponible."); return; }
                Field f = PlayerInfo.class.getDeclaredField("skinLookup");
                f.setAccessible(true);
                if (originalCaptured && originalSkinLookup != null) {
                    f.set(info, originalSkinLookup);
                    lastApplied = "";
                    info("Skin original restaurada localmente.");
                } else {
                    info("No hay skin original capturada para restaurar.");
                }
            } catch (Throwable t) {
                LOG.error("[SkinChar+] restore failed", t);
                info("Error restaurando skin original.");
            }
        });
    }

    private PlayerModelType detectModel(String path) {
        try {
            var img = javax.imageio.ImageIO.read(Path.of(path).toFile());
            if (img == null) return PlayerModelType.WIDE;
            int w = img.getWidth();
            int h = img.getHeight();
            if (w == 64 && h == 64) {
                int argb = img.getRGB(54, 20);
                int alpha = (argb >>> 24) & 0xFF;
                return alpha < 10 ? PlayerModelType.SLIM : PlayerModelType.WIDE;
            }
            return PlayerModelType.WIDE;
        } catch (Throwable t) {
            return PlayerModelType.WIDE;
        }
    }

    private String sanitizePath(String path) {
        if (path == null) return "";
        String p = path.trim();
        if (p.length() >= 2 && p.startsWith("\"") && p.endsWith("\"")) p = p.substring(1, p.length() - 1).trim();
        return p;
    }

    private boolean resetting;
    private void resetBool(String name) {
        if (resetting) return;
        Setting<?> s = settings.get(name);
        if (s instanceof BoolSetting b && b.get()) {
            resetting = true;
            try { b.set(false); } finally { resetting = false; }
        }
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[SkinChar+] " + msg), false);
            LOG.info("[SkinChar+] {}", msg);
        });
    }
}
