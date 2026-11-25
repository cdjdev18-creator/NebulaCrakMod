package com.nebulacrak.modules;

import com.nebulacrak.gui.ImageFileUtil;
import com.nebulacrak.gui.NebulaModules;
import com.nebulacrak.skinsync.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.UUID;

public class NebulaSkinSyncModule extends Module {
    public static NebulaSkinSyncModule INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> shareMySkin = sgGeneral.add(new BoolSetting.Builder()
        .name("share-my-skin")
        .description("Publica tu skin local a clientes compatibles (requiere servicio externo).")
        .defaultValue(false)
        .onChanged(v -> SkinSyncService.get().setShareMySkin(v))
        .build());

    public final Setting<Boolean> showCompatibleSkins = sgGeneral.add(new BoolSetting.Builder()
        .name("show-compatible-skins")
        .description("Muestra skins personalizadas recibidas de usuarios compatibles.")
        .defaultValue(true)
        .onChanged(v -> SkinSyncService.get().setShowCompatibleSkins(v))
        .build());

    public final Setting<Boolean> showStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("show-status")
        .description("Mostrar estado de conexión en chat al cambiar.")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> autoSync = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-sync")
        .description("Conectar y sincronizar automáticamente al activar.")
        .defaultValue(false)
        .build());

    public final Setting<String> serviceUrl = sgGeneral.add(new StringSetting.Builder()
        .name("service-url")
        .description("URL base del servicio externo Nebula SkinSync. Vacío = no conectar.")
        .defaultValue("")
        .build());

    public final Setting<String> statusSetting = sgGeneral.add(new StringSetting.Builder()
        .name("status")
        .description("Estado actual de SkinSync.")
        .defaultValue("Desconectado")
        .build());

    public final Setting<Boolean> clearCache;

    private long lastAutoConnectMs = 0L;

    public NebulaSkinSyncModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "Nebula SkinSync", "Sincroniza skins personalizadas entre clientes Nebula compatibles mediante servicio externo.");
        INSTANCE = this;

        clearCache = sgGeneral.add(new BoolSetting.Builder()
            .name("clear-cache")
            .description("Limpiar caché local de skins SkinSync.")
            .defaultValue(false)
            .build());

        SkinSyncService.get().setListener(new SkinSyncService.Listener() {
            @Override public void onStatus(SkinSyncStatus st, String detail) {
                statusSetting.set(detail == null ? st.label : detail);
                if (showStatus.get()) info("[" + st.label + "] " + detail);
            }
            @Override public void onRecord(SkinSyncRecord record) { /* cache applied; module hook point */ }
        });
    }

    @Override
    public void onActivate() {
        SkinSyncService.get().setShareMySkin(shareMySkin.get());
        SkinSyncService.get().setShowCompatibleSkins(showCompatibleSkins.get());
        SkinSyncService.get().configure(serviceUrl.get());
        if (autoSync.get()) tryConnect();
        statusSetting.set(SkinSyncStatus.DISCONNECTED.label);
    }

    @Override
    public void onDeactivate() {
        SkinSyncService.get().disconnect();
        statusSetting.set(SkinSyncStatus.DISCONNECTED.label);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (clearCache.get()) {
            SkinSyncService.get().clearCache();
            clearCache.set(false);
            info("Caché SkinSync limpiada.");
        }
        if (!autoSync.get()) return;
        SkinSyncStatus st = SkinSyncService.get().status();
        if (st == SkinSyncStatus.CONNECTED || st == SkinSyncStatus.CONNECTING || st == SkinSyncStatus.SYNCING) return;
        long now = System.currentTimeMillis();
        if (now - lastAutoConnectMs < 15000) return;
        lastAutoConnectMs = now;
        tryConnect();
    }

    private void tryConnect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        UUID self = mc.player.getUUID();
        if (self == null) return;
        String url = serviceUrl.get() == null ? "" : serviceUrl.get().trim();
        if (url.isBlank()) {
            statusSetting.set("URL de servicio vacía");
            return;
        }
        SkinSyncService.get().configure(url);
        SkinSyncService.get().connect(self);
    }

    public void publishIfEnabled(Path png, String model) {
        if (!isActive() || !shareMySkin.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        SkinSyncService.get().publishLocalSkin(png, model, mc.player.getUUID());
    }

    private void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> { if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("[SkinSync] " + msg), false); });
    }
}
