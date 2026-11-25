package com.nebulacrak.modules;

import com.nebulacrak.gui.ImageFileUtil;
import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.gui.NebulaTextureManager;
import com.nebulacrak.modules.NebulaSkinSyncModule;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Modifier;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class SkinCharPlusModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/SkinChar+");
    public static SkinCharPlusModule INSTANCE;

    private static final Set<String> SIXB6T_HOSTS = Set.of(
        "6b6t.org", "play.6b6t.org", "anarchy.6b6t.org", "6b6t.com", "play.6b6t.com"
    );
    private static final long SYNC_MIN_INTERVAL_MS = 10_000L;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("SpeedChangeMulti");
    private final SettingGroup sgSync = settings.createGroup("6b6t Sync");
    private final SettingGroup sgStatus = settings.createGroup("Status");

    private final Setting<String> mainSkinPath = sgGeneral.add(new StringSetting.Builder()
        .name("main-skin-path").description("Ruta PNG de skin principal (64x64, 64x32 o 128x128).").defaultValue("").build());
    private final Setting<String> extraSkin1 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-1").description("Skin adicional 2").defaultValue("").build());
    private final Setting<String> extraSkin2 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-2").description("Skin adicional 3").defaultValue("").build());
    private final Setting<String> extraSkin3 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-3").description("Skin adicional 4").defaultValue("").build());
    private final Setting<String> extraSkin4 = sgGeneral.add(new StringSetting.Builder().name("extra-skin-4").description("Skin adicional 5").defaultValue("").build());

    private final Setting<String> mainSkinPlayer = sgSync.add(new StringSetting.Builder().name("main-skin-player").description("Nombre de jugador para /skin en 6b6t (opcional).").defaultValue("").build());
    private final Setting<String> extraSkin1Player = sgSync.add(new StringSetting.Builder().name("extra-skin-1-player").description("Nombre de jugador para slot 2 en 6b6t (opcional).").defaultValue("").build());
    private final Setting<String> extraSkin2Player = sgSync.add(new StringSetting.Builder().name("extra-skin-2-player").description("Nombre de jugador para slot 3 en 6b6t (opcional).").defaultValue("").build());
    private final Setting<String> extraSkin3Player = sgSync.add(new StringSetting.Builder().name("extra-skin-3-player").description("Nombre de jugador para slot 4 en 6b6t (opcional).").defaultValue("").build());
    private final Setting<String> extraSkin4Player = sgSync.add(new StringSetting.Builder().name("extra-skin-4-player").description("Nombre de jugador para slot 5 en 6b6t (opcional).").defaultValue("").build());

    private final Setting<Boolean> sync6b6tEnabled = sgSync.add(new BoolSetting.Builder()
        .name("sync-6b6t-enabled").description("Habilita solicitudes /skin en 6b6t. Desactivado por defecto. No envía comandos sin activación explícita.").defaultValue(false).build());
    private final Setting<Boolean> syncRotationEnabled = sgSync.add(new BoolSetting.Builder()
        .name("sync-rotation-enabled").description("Durante rotación, solicita /skin para slots con nombre asociado. Requiere sync-6b6t-enabled.").defaultValue(false).visible(sync6b6tEnabled::get).build());
    private final Setting<Double> syncIntervalSeconds = sgSync.add(new DoubleSetting.Builder()
        .name("sync-interval-seconds").description("Intervalo mínimo entre solicitudes /skin (anti-spam).").defaultValue(30.0).min(10.0).max(300.0).sliderRange(10.0, 120.0).visible(sync6b6tEnabled::get).build());

    private final Setting<Integer> skinCount = sgSpeed.add(new IntSetting.Builder()
        .name("skin-count")
        .description("Cuántas skins rotar (2-5). Usa las primeras que estén configuradas.")
        .defaultValue(5)
        .min(2)
        .max(5)
        .sliderRange(2, 5)
        .build());

    private final Setting<Boolean> selectMainSkin = sgGeneral.add(new BoolSetting.Builder().name("select-main-skin").description("Abre selector PNG para skin principal.").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(mainSkinPath, "Seleccionar skin principal", 0); resetBool("select-main-skin"); } }).build());
    private final Setting<Boolean> selectExtra1 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-1").description("Seleccionar skin adicional 2").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin1, "Seleccionar skin adicional 2", 1); resetBool("select-extra-1"); } }).build());
    private final Setting<Boolean> selectExtra2 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-2").description("Seleccionar skin adicional 3").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin2, "Seleccionar skin adicional 3", 2); resetBool("select-extra-2"); } }).build());
    private final Setting<Boolean> selectExtra3 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-3").description("Seleccionar skin adicional 4").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin3, "Seleccionar skin adicional 4", 3); resetBool("select-extra-3"); } }).build());
    private final Setting<Boolean> selectExtra4 = sgGeneral.add(new BoolSetting.Builder().name("select-extra-4").description("Seleccionar skin adicional 5").defaultValue(false)
        .onChanged(v -> { if (v) { pickSkin(extraSkin4, "Seleccionar skin adicional 5", 4); resetBool("select-extra-4"); } }).build());

    private final Setting<Boolean> clearMain = sgGeneral.add(new BoolSetting.Builder().name("clear-main-skin").description("Quitar skin principal").defaultValue(false)
        .onChanged(v -> { if (v) { mainSkinPath.set(""); info("Skin principal eliminada."); resetBool("clear-main-skin"); } }).build());
    private final Setting<Boolean> clearExtra1 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-1").description("Quitar skin adicional 2").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin1.set(""); resetBool("clear-extra-1"); } }).build());
    private final Setting<Boolean> clearExtra2 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-2").description("Quitar skin adicional 3").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin2.set(""); resetBool("clear-extra-2"); } }).build());
    private final Setting<Boolean> clearExtra3 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-3").description("Quitar skin adicional 4").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin3.set(""); resetBool("clear-extra-3"); } }).build());
    private final Setting<Boolean> clearExtra4 = sgGeneral.add(new BoolSetting.Builder().name("clear-extra-4").description("Quitar skin adicional 5").defaultValue(false)
        .onChanged(v -> { if (v) { extraSkin4.set(""); resetBool("clear-extra-4"); } }).build());

    private final Setting<Boolean> applyMainLocal = sgGeneral.add(new BoolSetting.Builder().name("apply-main-local").description("Aplica la skin principal localmente al jugador.").defaultValue(false)
        .onChanged(v -> { if (v) { applySlot(0, true, true); resetBool("apply-main-local"); } }).build());
    private final Setting<Boolean> restoreOriginal = sgGeneral.add(new BoolSetting.Builder().name("restore-original").description("Vuelve a la skin original local.").defaultValue(false)
        .onChanged(v -> { if (v) { restoreOriginalSkin(); resetBool("restore-original"); } }).build());

    private final Setting<Boolean> syncSelectedSlot = sgSync.add(new BoolSetting.Builder()
        .name("sync-selected-slot").description("Solicita /skin para el último slot aplicado localmente (si tiene nombre asociado y sync habilitado en 6b6t).").defaultValue(false)
        .build());

    private final Setting<Boolean> speedChangeMulti = sgSpeed.add(new BoolSetting.Builder().name("enabled").description("Alterna entre las 5 skins configuradas.").defaultValue(false)
        .onChanged(v -> { if (v) startRotation(); else stopRotation(false); }).build());
    private final Setting<Double> intervalSeconds = sgSpeed.add(new DoubleSetting.Builder().name("interval-seconds").description("Tiempo entre cambios de skin (segundos).").defaultValue(0.5).min(0.2).max(10.0).sliderRange(0.2, 10.0).build());

    private final Setting<String> syncStatus = sgStatus.add(new StringSetting.Builder()
        .name("sync-status").description("Estado: LOCAL / SOLICITUD ENVIADA / CONFIRMADO POR SERVIDOR / NO COMPATIBLE / REQUISITOS NO CUMPLIDOS / IMAGEN INVÁLIDA.").defaultValue("LOCAL").build());

    private final List<Integer> rotationSlots = new ArrayList<>();
    private int rotationIndex = 0;
    private long nextChangeMs = 0L;
    private String lastAppliedPath = "";
    private int lastAppliedSlot = -1;
    private Supplier<PlayerSkin> originalSkinLookup;
    private boolean originalCaptured;
    private boolean resetting;
    private long lastSyncMs = 0L;
    private String lastSyncPlayer = "";
    private long lastLocalReapplyMs = 0L;

    public SkinCharPlusModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "SkinChar+", "5 skins locales con rotación. Sync remoto 6b6t opcional vía /skin <jugador> (requiere requisitos del servidor).");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        captureOriginalIfNeeded();
        updateStatusForContext();
        if (speedChangeMulti.get()) startRotation();
        else if (!sanitizePath(mainSkinPath.get()).isBlank()) applySlot(0, true, false);
    }

    @Override
    public void onDeactivate() {
        stopRotation(true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;
        captureOriginalIfNeeded();

        // Reaplicar skin local periódicamente: el juego puede resetear skinLookup.
        if (lastAppliedSlot >= 0 && !lastAppliedPath.isBlank()) {
            long now = System.currentTimeMillis();
            if (now - lastLocalReapplyMs > 2500L) {
                lastLocalReapplyMs = now;
                applySlot(lastAppliedSlot, true, false);
            }
        }

        if (syncSelectedSlot.get()) {
            requestSyncForSlot(lastAppliedSlot >= 0 ? lastAppliedSlot : 0, true);
            resetBool("sync-selected-slot");
        }
        if (!speedChangeMulti.get()) return;
        if (rotationSlots.isEmpty()) buildRotation();
        if (rotationSlots.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now < nextChangeMs) return;
        int slot = rotationSlots.get(rotationIndex % rotationSlots.size());
        rotationIndex = (rotationIndex + 1) % rotationSlots.size();
        nextChangeMs = now + (long) (intervalSeconds.get() * 1000.0);
        applySlot(slot, true, false);
        if (sync6b6tEnabled.get() && syncRotationEnabled.get()) requestSyncForSlot(slot, false);
    }

    private Setting<String> pathSetting(int slot) {
        return switch (slot) { case 0 -> mainSkinPath; case 1 -> extraSkin1; case 2 -> extraSkin2; case 3 -> extraSkin3; case 4 -> extraSkin4; default -> mainSkinPath; };
    }
    private Setting<String> playerSetting(int slot) {
        return switch (slot) { case 0 -> mainSkinPlayer; case 1 -> extraSkin1Player; case 2 -> extraSkin2Player; case 3 -> extraSkin3Player; case 4 -> extraSkin4Player; default -> mainSkinPlayer; };
    }

    private void startRotation() {
        buildRotation();
        rotationIndex = 0;
        nextChangeMs = 0L;
        if (rotationSlots.isEmpty()) {
            info("SpeedChangeMulti: no hay skins válidas configuradas.");
            speedChangeMulti.set(false);
            return;
        }
        updateStatusForContext();
        info("SpeedChangeMulti activado. Intervalo local: " + String.format(Locale.ROOT, "%.2f", intervalSeconds.get()) + "s. Slots: " + rotationSlots.size());
    }

    private void stopRotation(boolean restoreOriginalOnDeactivate) {
        rotationSlots.clear();
        rotationIndex = 0;
        nextChangeMs = 0L;
        if (restoreOriginalOnDeactivate) restoreOriginalSkin();
        updateStatusForContext();
    }

    private void buildRotation() {
        rotationSlots.clear();
        int max = Math.max(2, Math.min(5, skinCount.get()));
        for (int i = 0; i < 5 && rotationSlots.size() < max; i++) {
            String p = sanitizePath(pathSetting(i).get());
            if (p.isBlank()) continue;
            String err = ImageFileUtil.validateSkinPng(Path.of(p));
            if (err == null) rotationSlots.add(i);
        }
    }

    private void pickSkin(Setting<String> target, String title, int slot) {
        ImageFileUtil.openPngPicker(title, selected -> {
            String clean = sanitizePath(selected);
            String err = ImageFileUtil.validateSkinPng(Path.of(clean));
            if (err != null) { syncStatus.set("IMAGEN INVÁLIDA"); info(err); return; }
            target.set(clean);
            info("Skin seleccionada: " + new File(clean).getName());
            applySlot(slot, true, true);
        }, err -> { syncStatus.set("IMAGEN INVÁLIDA"); info(err); });
    }

    private void applySlot(int slot, boolean localApply, boolean notify) {
        String p = sanitizePath(pathSetting(slot).get());
        if (p.isBlank()) { if (notify) info("No hay skin configurada en el slot " + (slot + 1) + "."); return; }
        String err = ImageFileUtil.validateSkinPng(Path.of(p));
        if (err != null) { syncStatus.set("IMAGEN INVÁLIDA"); if (notify) info("Skin inválida: " + err); return; }
        if (p.equals(lastAppliedPath) && localApply) {
            lastAppliedSlot = slot;
            if (notify) info("Skin ya aplicada localmente: " + new File(p).getName());
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            try {
                captureOriginalIfNeeded();
                Identifier tex = NebulaTextureManager.loadTextureFromFile("skinchar_slot_" + slot, p);
                if (tex == null) { info("No se pudo cargar la textura de skin."); return; }
                PlayerModelType model = detectModel(p);
                boolean applied = localApply && applyLocalSkin(mc, tex, model);
                lastAppliedPath = p;
                lastAppliedSlot = slot;
                syncStatus.set("LOCAL");
                try {
                    NebulaSkinSyncModule sync = NebulaSkinSyncModule.INSTANCE;
                    if (sync != null) sync.publishIfEnabled(Path.of(p), model.name());
                } catch (Throwable ignored) {}
                if (notify) {
                    if (applied) info("Skin aplicada localmente: " + new File(p).getName() + " (" + model.name().toLowerCase(Locale.ROOT) + ", slot " + (slot + 1) + "). No se sincroniza PNG automáticamente; 6b6t usa /skin <jugador> si habilitas sync.");
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
            if (mc.player == null || mc.getConnection() == null) {
                info("Debug: player o connection es null");
                return false;
            }

            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info == null) {
                info("Debug: PlayerInfo es null");
                return false;
            }

            ClientAsset.ResourceTexture bodyTex;
            try {
                bodyTex = new ClientAsset.ResourceTexture(tex, tex);
            } catch (Throwable t) {
                bodyTex = new ClientAsset.ResourceTexture(tex);
            }

            PlayerSkin custom = PlayerSkin.insecure(bodyTex, null, null, model);

            Field f = findSkinLookupField(info);
            if (f == null) {
                info("Error: no se encontró campo skinLookup en PlayerInfo.");
                return false;
            }

            f.setAccessible(true);
            removeFinalIfPossible(f);
            f.set(info, (Supplier<PlayerSkin>) () -> custom);
            try { info.getSkin(); } catch (Throwable ignored) {}
            return true;
        } catch (Throwable t) {
            LOG.error("[SkinChar+] Fallo al aplicar skin local", t);
            info("Error exacto: " + t.getClass().getSimpleName() + " -> " + (t.getMessage() != null ? t.getMessage() : "sin mensaje"));
            return false;
        }
    }

    private void captureOriginalIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) return;
        try {
            Field f = findSkinLookupField(info);
            if (f == null) return;
            f.setAccessible(true);
            Object val = f.get(info);
            if (!originalCaptured && val instanceof Supplier<?> s) {
                originalSkinLookup = (Supplier<PlayerSkin>) s;
                originalCaptured = true;
            }
        } catch (Throwable t) {
            LOG.debug("[SkinChar+] capture original failed: {}", t.toString());
        }
    }

    private void restoreOriginalSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) { info("No se puede restaurar: Minecraft no disponible."); return; }
        mc.execute(() -> {
            try {
                if (mc.player == null || mc.getConnection() == null) { info("No se puede restaurar: jugador no disponible."); return; }
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info == null) { info("No se puede restaurar: PlayerInfo no disponible."); return; }
                Field f = findSkinLookupField(info);
                if (f == null) { info("No se puede restaurar: campo skinLookup no encontrado."); return; }
                f.setAccessible(true);
                removeFinalIfPossible(f);
                if (originalCaptured && originalSkinLookup != null) {
                    f.set(info, originalSkinLookup);
                    lastAppliedPath = "";
                    lastAppliedSlot = -1;
                    syncStatus.set("LOCAL");
                    info("Skin original restaurada localmente.");
                } else {
                    info("No hay skin original capturada para restaurar.");
                }
            } catch (Throwable t) {
                LOG.error("[SkinChar+] restore failed", t);
                info("Error restaurando skin original: " + t.getClass().getSimpleName());
            }
        });
    }

    private static Field findSkinLookupField(PlayerInfo info) {
        if (info == null) return null;
        String[] names = {"skinLookup", "skinSupplier", "skin", "textureSupplier"};
        for (String name : names) {
            try {
                Field f = PlayerInfo.class.getDeclaredField(name);
                if (Supplier.class.isAssignableFrom(f.getType())) return f;
            } catch (NoSuchFieldException ignored) {}
        }
        for (Field f : PlayerInfo.class.getDeclaredFields()) {
            if (!Supplier.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(info);
                if (val instanceof Supplier<?> s) {
                    Object got = s.get();
                    if (got instanceof PlayerSkin) return f;
                }
            } catch (Throwable ignored) {}
        }
        for (Field f : PlayerInfo.class.getDeclaredFields()) {
            if (Supplier.class.isAssignableFrom(f.getType())) return f;
        }
        return null;
    }

    private static void removeFinalIfPossible(Field f) {
        if (f == null) return;
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (Throwable ignored) {}
    }

    private void requestSyncForSlot(int slot, boolean manual) {
        if (!sync6b6tEnabled.get()) {
            if (manual) info("Sync 6b6t desactivado. Activa sync-6b6t-enabled para solicitar /skin.");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null || mc.player == null) {
            if (manual) info("No hay conexión activa para enviar /skin.");
            return;
        }
        String host = getCurrentServerHost();
        if (!is6b6tHost(host)) {
            syncStatus.set("NO COMPATIBLE");
            if (manual) info("Servidor no compatible con sync 6b6t (/skin). Modo local disponible. Host: " + host);
            return;
        }
        String playerName = sanitizePlayerName(playerSetting(slot).get());
        if (playerName.isBlank()) {
            if (manual) info("El slot " + (slot + 1) + " no tiene nombre de jugador asociado para /skin.");
            return;
        }
        if (!isValidPlayerName(playerName)) {
            syncStatus.set("REQUISITOS NO CUMPLIDOS");
            if (manual) info("Nombre de jugador inválido para /skin: " + playerName);
            return;
        }
        long now = System.currentTimeMillis();
        long minInterval = Math.max(SYNC_MIN_INTERVAL_MS, (long) (syncIntervalSeconds.get() * 1000.0));
        if (now - lastSyncMs < minInterval) {
            if (manual) info("Anti-spam: espera " + String.format(Locale.ROOT, "%.1f", (minInterval - (now - lastSyncMs)) / 1000.0) + "s antes de otra solicitud /skin.");
            return;
        }
        if (!manual && playerName.equalsIgnoreCase(lastSyncPlayer) && now - lastSyncMs < minInterval) return;

        lastSyncMs = now;
        lastSyncPlayer = playerName;
        syncStatus.set("SOLICITUD ENVIADA");
        info("Solicitud enviada al servidor: /skin " + playerName + " (requiere requisitos 6b6t: voto/rango; puede ser denegada).");
        try {
            mc.getConnection().sendCommand("skin " + playerName);
            // No confirmamos éxito real; el servidor puede responder por chat. El usuario debe verificar.
            syncStatus.set("SOLICITUD ENVIADA");
        } catch (Throwable t) {
            LOG.warn("[SkinChar+] Failed to send /skin", t);
            syncStatus.set("REQUISITOS NO CUMPLIDOS");
            info("No se pudo enviar /skin: " + t.getClass().getSimpleName());
        }
    }

    private void updateStatusForContext() {
        String host = getCurrentServerHost();
        if (host.isBlank()) { syncStatus.set("LOCAL"); return; }
        if (is6b6tHost(host)) syncStatus.set(sync6b6tEnabled.get() ? "SERVIDOR COMPATIBLE (6b6t)" : "LOCAL");
        else syncStatus.set("LOCAL / NO COMPATIBLE");
    }

    private String getCurrentServerHost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getCurrentServer() == null) return "";
        String ip = mc.getCurrentServer().ip;
        return ip == null ? "" : ip.toLowerCase(Locale.ROOT).trim();
    }

    private boolean is6b6tHost(String host) {
        if (host == null || host.isBlank()) return false;
        String h = host.toLowerCase(Locale.ROOT).trim();
        int colon = h.indexOf(':');
        if (colon >= 0) h = h.substring(0, colon);
        if (SIXB6T_HOSTS.contains(h)) return true;
        for (String d : SIXB6T_HOSTS) if (h.endsWith("." + d) || h.equals(d)) return true;
        return h.contains("6b6t");
    }

    private static boolean isValidPlayerName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    private static String sanitizePlayerName(String s) {
        if (s == null) return "";
        String v = s.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() > 1) v = v.substring(1, v.length() - 1).trim();
        return v;
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
