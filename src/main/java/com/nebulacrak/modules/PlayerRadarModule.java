package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRadarModule extends Module {
    public static PlayerRadarModule INSTANCE;

    private static final int MAX_LAST_SEEN = 24;

    public static final class LastSeenEntry {
        public final String name;
        public final UUID uuid;
        public final double x, y, z;
        public final long seenAtMs;
        public final String dimension;
        public LastSeenEntry(String name, UUID uuid, double x, double y, double z, long seenAtMs, String dimension) {
            this.name = name; this.uuid = uuid; this.x = x; this.y = y; this.z = z; this.seenAtMs = seenAtMs; this.dimension = dimension;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgEntities = settings.createGroup("Entities");
    private final SettingGroup sgXaero = settings.createGroup("Xaero");

    private final Setting<Boolean> showPlayers = sgPlayers.add(new BoolSetting.Builder()
        .name("show-players").description("Mostrar jugadores visibles.").defaultValue(true).build());
    private final Setting<Boolean> showLastSeen = sgPlayers.add(new BoolSetting.Builder()
        .name("show-last-seen").description("Guardar/mostrar última posición conocida de jugadores vistos.").defaultValue(true).build());
    private final Setting<Boolean> clearLastSeen = sgPlayers.add(new BoolSetting.Builder()
        .name("clear-last-seen").description("Limpiar registros de última posición.").defaultValue(false).build());
    private final Setting<Boolean> showEntities = sgEntities.add(new BoolSetting.Builder()
        .name("show-entities").description("Mostrar entidades vivas (no jugadores).").defaultValue(false).build());
    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance").description("Distancia máxima de representación para entidades visibles. No aumenta datos enviados por el servidor.").defaultValue(128.0).min(8.0).max(512.0).sliderRange(8.0, 256.0).build());
    private final Setting<Boolean> showBox = sgGeneral.add(new BoolSetting.Builder()
        .name("show-box").description("Mostrar caja ESP.").defaultValue(true).build());
    private final Setting<Boolean> showTracers = sgGeneral.add(new BoolSetting.Builder()
        .name("show-position-indicator").description("Mostrar línea indicadora de posición.").defaultValue(false).build());
    private final Setting<SettingColor> playerColor = sgPlayers.add(new ColorSetting.Builder()
        .name("player-color").description("Color para jugadores visibles.").defaultValue(new SettingColor(255, 80, 80, 255)).build());
    private final Setting<SettingColor> lastSeenColor = sgPlayers.add(new ColorSetting.Builder()
        .name("last-seen-color").description("Color para última posición conocida.").defaultValue(new SettingColor(255, 200, 80, 120)).visible(showLastSeen::get).build());
    private final Setting<SettingColor> entityColor = sgEntities.add(new ColorSetting.Builder()
        .name("entity-color").description("Color para entidades.").defaultValue(new SettingColor(80, 255, 120, 255)).build());

    private final Setting<Boolean> detectXaero = sgXaero.add(new BoolSetting.Builder()
        .name("detect-xaero").description("Detecta Xaero's Minimap/World Map si están instalados.").defaultValue(true).build());
    private final Setting<String> xaeroStatus = sgXaero.add(new StringSetting.Builder()
        .name("xaero-status").description("Estado de compatibilidad Xaero (solo lectura informativa).").defaultValue("No comprobado").build());

    private final Map<UUID, LastSeenEntry> lastSeen = new ConcurrentHashMap<>();
    private ResourceKey<Level> lastDimension;
    private String lastServer;
    private boolean resetting;
    private long lastXaeroCheckMs = 0L;

    public PlayerRadarModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "PlayerRadar", "Radar/ESP de jugadores y entidades usando solo datos que el cliente recibe del servidor.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        checkXaero();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        if (clearLastSeen.get()) {
            resetBool(clearLastSeen);
            lastSeen.clear();
        }

        String server = mc.getCurrentServer() != null && mc.getCurrentServer().ip != null ? mc.getCurrentServer().ip : "";
        ClientLevel level = mc.level;
        ResourceKey<Level> dim = level != null ? level.dimension() : null;
        if (!Objects.equals(server, lastServer) || !Objects.equals(dim, lastDimension)) {
            lastServer = server;
            lastDimension = dim;
            lastSeen.clear(); // avoid stale cross-dimension/server markers
        }

        long now = System.currentTimeMillis();
        if (detectXaero.get() && now - lastXaeroCheckMs > 10000L) {
            lastXaeroCheckMs = now;
            checkXaero();
        }

        if (level == null || mc.player == null || !showLastSeen.get()) return;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Player p) || p == mc.player || p.isRemoved()) continue;
            lastSeen.put(p.getUUID(), new LastSeenEntry(p.getGameProfile().name(), p.getUUID(), p.getX(), p.getY(), p.getZ(), now, dimName(dim)));
        }
        pruneLastSeen(now);
    }

    private void pruneLastSeen(long now) {
        if (lastSeen.size() <= MAX_LAST_SEEN) return;
        List<Map.Entry<UUID, LastSeenEntry>> entries = new ArrayList<>(lastSeen.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue().seenAtMs));
        for (int i = 0; i < entries.size() - MAX_LAST_SEEN; i++) lastSeen.remove(entries.get(i).getKey());
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        double maxDistSq = maxDistance.get() * maxDistance.get();
        ResourceKey<Level> dim = mc.level.dimension();

        Set<UUID> visiblePlayers = new HashSet<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity == mc.player || entity.isRemoved()) continue;
            boolean isPlayer = entity instanceof Player;
            if (isPlayer && !showPlayers.get()) continue;
            if (!isPlayer) {
                if (!showEntities.get() || !(entity instanceof LivingEntity)) continue;
            }
            double distSq = mc.player.distanceToSqr(entity);
            if (distSq > maxDistSq) continue;

            if (isPlayer) visiblePlayers.add(entity.getUUID());
            SettingColor sc = isPlayer ? playerColor.get() : entityColor.get();
            drawEntity(event, entity.getBoundingBox(), entity.position().add(0, entity.getBbHeight() * 0.5, 0), sc, 40);
        }

        if (showLastSeen.get()) {
            long now = System.currentTimeMillis();
            SettingColor sc = lastSeenColor.get();
            for (LastSeenEntry e : lastSeen.values()) {
                if (e == null || visiblePlayers.contains(e.uuid)) continue;
                if (!Objects.equals(e.dimension, dimName(dim))) continue;
                double dx = e.x - mc.player.getX(); double dy = e.y - mc.player.getY(); double dz = e.z - mc.player.getZ();
                if ((dx * dx + dy * dy + dz * dz) > maxDistSq) continue;
                AABB box = new AABB(e.x - 0.3, e.y, e.z - 0.3, e.x + 0.3, e.y + 1.8, e.z + 0.3);
                drawEntity(event, box, new Vec3(e.x, e.y + 0.9, e.z), sc, 20);
            }
        }
    }

    private void drawEntity(Render3DEvent event, AABB box, Vec3 center, SettingColor sc, int sideAlpha) {
        Color line = new Color(sc.r, sc.g, sc.b, sc.a);
        Color side = new Color(sc.r, sc.g, sc.b, sideAlpha);
        if (showBox.get()) event.renderer.box(box, side, line, ShapeMode.Both, 0);
        if (showTracers.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, center.x, center.y, center.z, line);
    }

    private void checkXaero() {
        boolean minimap = isClassPresent("xaero.common.HudMod") || isClassPresent("xaero.minimap.XaeroMinimap");
        boolean worldMap = isClassPresent("xaero.map.WorldMap") || isClassPresent("xaero.map.mods.SupportMods");
        String status;
        if (minimap && worldMap) status = "Xaero Minimap + World Map detectados (solo compatibilidad informativa; sin integración de API interna)";
        else if (minimap) status = "Xaero Minimap detectado (sin integración de API interna)";
        else if (worldMap) status = "Xaero World Map detectado (sin integración de API interna)";
        else status = "Xaero no detectado";
        xaeroStatus.set(status);
    }

    private static boolean isClassPresent(String name) {
        try { Class.forName(name, false, PlayerRadarModule.class.getClassLoader()); return true; } catch (Throwable ignored) { return false; }
    }

    private static String dimName(ResourceKey<Level> dim) {
        if (dim == null) return "unknown";
        return String.valueOf(dim.identifier());
    }

    private void resetBool(Setting<Boolean> s) {
        if (resetting || s == null) return;
        resetting = true;
        try { s.set(false); } finally { resetting = false; }
    }

    public List<LastSeenEntry> getLastSeenEntries() {
        return new ArrayList<>(lastSeen.values());
    }
}
