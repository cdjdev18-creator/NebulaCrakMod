package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSendViewModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/ChatSENDVIEW+");
    public static ChatSendViewModule INSTANCE;

    public static final class Bubble {
        public final UUID uuid;
        public String raw;
        public String display;
        public List<String> lines;
        public long createdMs;
        public double x, y, z;
        public double px, py, pz;
        public float width;
        public float height;

        public Bubble(UUID uuid, String raw, String display, List<String> lines, long now, double x, double y, double z, float width, float height) {
            this.uuid = uuid;
            this.raw = raw;
            this.display = display;
            this.lines = lines;
            this.createdMs = now;
            this.x = x; this.y = y; this.z = z;
            this.px = x; this.py = y; this.pz = z;
            this.width = width;
            this.height = height;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> debugBubble = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-bubble")
        .description("Render a test bubble above local player to verify rendering.")
        .defaultValue(false)
        .build());

    private final Setting<Integer> maxCharacters = sgGeneral.add(new IntSetting.Builder()
        .name("max-characters")
        .description("Maximum characters shown in bubble. Excess is truncated with ...")
        .defaultValue(50)
        .min(5)
        .max(500)
        .sliderRange(5, 500)
        .build());

    private final Setting<Integer> maxWidth = sgGeneral.add(new IntSetting.Builder()
        .name("max-width")
        .description("Maximum bubble width in pixels.")
        .defaultValue(180)
        .min(60)
        .max(500)
        .sliderRange(60, 400)
        .build());

    private final Setting<Boolean> autoHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-height")
        .description("Automatically adjust bubble height to text lines.")
        .defaultValue(true)
        .build());

    private final Setting<Double> displayDuration = sgGeneral.add(new DoubleSetting.Builder()
        .name("display-duration")
        .description("Seconds a bubble remains visible.")
        .defaultValue(5.0)
        .min(1.0)
        .max(30.0)
        .sliderRange(1.0, 15.0)
        .build());

    private final Setting<Boolean> fadeOut = sgGeneral.add(new BoolSetting.Builder()
        .name("fade-out")
        .description("Fade bubble out before disappearing.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> padding = sgGeneral.add(new IntSetting.Builder()
        .name("padding")
        .description("Padding between text and bubble edge.")
        .defaultValue(4)
        .min(1)
        .max(16)
        .sliderRange(1, 12)
        .build());

    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Text scale.")
        .defaultValue(1.0)
        .min(0.5)
        .max(2.5)
        .sliderRange(0.5, 2.0)
        .build());

    private final Map<UUID, Bubble> bubbles = new ConcurrentHashMap<>();
    private boolean renderLogged = false;
    private boolean tickLogged = false;

    public ChatSendViewModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "ChatSENDVIEW+", "Shows local chat bubbles above players.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        renderLogged = false;
        tickLogged = false;
        LOG.info("[ChatSENDVIEW+] activated");
    }

    @Override
    public void onDeactivate() {
        bubbles.clear();
        LOG.info("[ChatSENDVIEW+] deactivated, bubbles cleared");
    }

    public void onPlayerChat(UUID uuid, String name, String message) {
        if (!isActive() || uuid == null || message == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        Player player = mc.level.getPlayerByUUID(uuid);
        if (player == null) return;

        String trimmed = message.trim();
        if (trimmed.isEmpty()) return;

        int limit = Math.max(5, Math.min(500, maxCharacters.get()));
        String display = trimmed.length() > limit ? trimmed.substring(0, limit) + "..." : trimmed;

        LOG.info("[ChatSENDVIEW+] player identified uuid={} name={} len={}", uuid, name, trimmed.length());
        upsertBubble(player, uuid, trimmed, display, System.currentTimeMillis());
        LOG.info("[ChatSENDVIEW+] bubble created/updated for {}", name);
    }

    private void upsertBubble(Player player, UUID uuid, String raw, String display, long now) {
        List<String> lines = wrapText(display, Math.max(40, maxWidth.get() - padding.get() * 2), textScale.get());
        if (lines.isEmpty()) lines = List.of(display);

        TextRenderer tr = TextRenderer.get();
        float w = (float) Math.min(maxWidth.get(), measureMaxLine(lines, textScale.get()) + padding.get() * 2);
        double lineH = tr.getHeight(true) * textScale.get();
        float h = (float) (padding.get() * 2 + Math.max(lineH, lines.size() * lineH));
        if (!autoHeight.get()) h = (float) (padding.get() * 2 + lineH);

        double bx = player.getX();
        double by = player.getY() + player.getBbHeight() + 0.35;
        double bz = player.getZ();

        Bubble b = bubbles.get(uuid);
        if (b == null) {
            bubbles.put(uuid, new Bubble(uuid, raw, display, lines, now, bx, by, bz, w, h));
        } else {
            b.raw = raw;
            b.display = display;
            b.lines = lines;
            b.createdMs = now;
            b.width = w;
            b.height = h;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) {
            if (!bubbles.isEmpty()) bubbles.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            bubbles.clear();
            return;
        }

        if (!tickLogged) {
            LOG.info("[ChatSENDVIEW+] tick running, world loaded");
            tickLogged = true;
        }

        long now = System.currentTimeMillis();
        long durationMs = Math.max(1000L, (long) (displayDuration.get() * 1000.0));

        Iterator<Map.Entry<UUID, Bubble>> it = bubbles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Bubble> e = it.next();
            Bubble b = e.getValue();
            if (now - b.createdMs > durationMs) {
                it.remove();
                continue;
            }
            Player p = mc.level.getPlayerByUUID(e.getKey());
            if (p == null || p.isRemoved()) {
                it.remove();
                continue;
            }
            b.px = b.x; b.py = b.y; b.pz = b.z;
            b.x = p.getX();
            b.y = p.getY() + p.getBbHeight() + 0.35;
            b.z = p.getZ();
        }

        if (debugBubble.get() && mc.player != null) {
            UUID id = mc.player.getUUID();
            Bubble b = bubbles.get(id);
            if (b == null || !"ChatSENDVIEW TEST".equals(b.raw) || now - b.createdMs > durationMs - 250) {
                upsertBubble(mc.player, id, "ChatSENDVIEW TEST", "ChatSENDVIEW TEST", now);
            }
        } else if (mc.player != null) {
            Bubble b = bubbles.get(mc.player.getUUID());
            if (b != null && "ChatSENDVIEW TEST".equals(b.raw)) bubbles.remove(mc.player.getUUID());
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        if (bubbles.isEmpty()) return;

        if (!renderLogged) {
            LOG.info("[ChatSENDVIEW+] render3d executing, bubbles={}", bubbles.size());
            renderLogged = true;
        }

        long now = System.currentTimeMillis();
        long durationMs = Math.max(1000L, (long) (displayDuration.get() * 1000.0));
        long fadeMs = Math.min(800L, Math.max(200L, durationMs / 4));

        TextRenderer tr = TextRenderer.get();
        for (Bubble b : bubbles.values()) {
            float age = now - b.createdMs;
            if (age > durationMs) continue;

            float alphaMul = 1f;
            if (fadeOut.get() && age > durationMs - fadeMs) {
                alphaMul = Math.max(0f, 1f - (age - (durationMs - fadeMs)) / (float) fadeMs);
            }
            if (alphaMul <= 0.02f) continue;

            double x = b.px + (b.x - b.px) * event.tickDelta;
            double y = b.py + (b.y - b.py) * event.tickDelta;
            double z = b.pz + (b.z - b.pz) * event.tickDelta;

            renderBubble(tr, b, x, y, z, alphaMul);
        }
    }

    private void renderBubble(TextRenderer tr, Bubble b, double x, double y, double z, float alphaMul) {
        try {
            double scale = 0.025 * Math.max(0.5, Math.min(2.5, textScale.get()));
            float pad = padding.get();
            float w = b.width;
            float h = b.height;

            NametagUtils.begin(new Vector3d(x, y, z));
            NametagUtils.scale = scale;

            tr.beginBig();

            int bgA = Math.round(204 * alphaMul); // ~80% opacity
            int bg = (bgA << 24) | 0x00FFFFFF; // white
            int txtA = Math.round(255 * alphaMul);
            int txt = (txtA << 24) | 0x00000000; // black

            double x0 = -w / 2.0;
            double y0 = -h;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x0, y0, w, h, new Color(bg));
            Renderer2D.COLOR.render();

            double ty = y0 + pad;
            double lineH = tr.getHeight(true);
            for (String line : b.lines) {
                double lw = tr.getWidth(line, true);
                double tx = -lw / 2.0;
                tr.render(line, tx, ty, new Color(txt), true);
                ty += lineH;
            }

            tr.end();
            NametagUtils.end();
        } catch (Throwable t) {
            LOG.debug("[ChatSENDVIEW+] render failed: {}", t.toString());
            try { NametagUtils.end(); } catch (Throwable ignored) {}
        }
    }

    private List<String> wrapText(String text, int maxPx, double scale) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        TextRenderer tr = TextRenderer.get();
        double maxW = Math.max(20, maxPx / Math.max(0.5, scale));
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (tr.getWidth(candidate, true) <= maxW) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (tr.getWidth(word, true) <= maxW) {
                    line.append(word);
                } else {
                    String rem = word;
                    while (!rem.isEmpty()) {
                        int cut = rem.length();
                        while (cut > 1 && tr.getWidth(rem.substring(0, cut), true) > maxW) cut--;
                        out.add(rem.substring(0, cut));
                        rem = rem.substring(cut);
                    }
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private double measureMaxLine(List<String> lines, double scale) {
        TextRenderer tr = TextRenderer.get();
        double max = 0;
        for (String l : lines) max = Math.max(max, tr.getWidth(l, true) * scale);
        return max;
    }
}
