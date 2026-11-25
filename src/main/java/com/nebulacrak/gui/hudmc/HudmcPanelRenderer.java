package com.nebulacrak.gui.hudmc;

import com.nebulacrak.config.NebulaConfig;
import com.nebulacrak.discord.NebulaChatMessage;
import com.nebulacrak.discord.NebulaDiscordSession;
import com.nebulacrak.modules.DiscordConnectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HudmcPanelRenderer {
    public static final int HEADER_H = 22;
    public static final int TAB_H = 16;
    public static final int BTN_W = 14;

    public interface Actions {
        void rebuild();
        void save();
        void bringToFront(HudmcPanelState panel);
        void openContextMenu(int x, int y);
        void openConfig(HudmcPanelState panel);
        void closePanel(HudmcPanelState panel);
        void loginDiscord(HudmcPanelState panel);
        void logoutDiscord(HudmcPanelState panel);
        void sendChat(HudmcPanelState panel);
        void setChatChannel(HudmcPanelState panel, String channel);
    }

    public void renderPanel(GuiGraphics g, HudmcPanelState p, boolean active, int mouseX, int mouseY) {
        int h = p.minimized ? HEADER_H : p.height;
        int bg = NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? NebulaConfig.editionPanelBgColor : 0xD0121620;
        int border = active ? 0xFF8B5CF6 : (NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? NebulaConfig.editionBorderColor : 0xFF2D3342);
        int headerBg = NebulaConfig.uiStyle == NebulaConfig.UIStyle.EDICION ? NebulaConfig.editionButtonColor : 0xF0181D28;

        g.fill(p.x, p.y, p.x + p.width, p.y + h, bg);
        g.fill(p.x, p.y, p.x + p.width, p.y + HEADER_H, headerBg);
        g.fill(p.x, p.y, p.x + p.width, p.y + 1, border);
        g.fill(p.x, p.y + h - 1, p.x + p.width, p.y + h, border);
        g.fill(p.x, p.y, p.x + 1, p.y + h, border);
        g.fill(p.x + p.width - 1, p.y, p.x + p.width, p.y + h, border);
        g.fill(p.x, p.y + HEADER_H - 1, p.x + p.width, p.y + HEADER_H, 0x332D3342);

        String title = p.name == null ? "Panel" : p.name;
        g.drawString(Minecraft.getInstance().font, title, p.x + 6, p.y + 7, 0xFFE5E7EB, false);

        drawHeaderButtons(g, p, mouseX, mouseY);
        if (p.minimized) return;

        drawTabs(g, p, mouseX, mouseY);
        int contentY = p.y + HEADER_H + TAB_H + 4;
        int contentH = Math.max(20, p.y + h - contentY - 6);
        drawContent(g, p, p.x + 6, contentY, p.width - 12, contentH);
        drawResizeHandle(g, p);
    }

    private void drawHeaderButtons(GuiGraphics g, HudmcPanelState p, int mx, int my) {
        int y = p.y + 4;
        int closeX = p.x + p.width - (BTN_W + 4);
        int maxX = closeX - (BTN_W + 2);
        int minX = maxX - (BTN_W + 2);
        drawBtn(g, minX, y, BTN_W, BTN_W, p.minimized ? "+" : "-", mx, my);
        drawBtn(g, maxX, y, BTN_W, BTN_W, p.maximized ? "R" : "[]", mx, my);
        drawBtn(g, closeX, y, BTN_W, BTN_W, "X", mx, my);
    }

    private void drawBtn(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hover ? 0xFF303847 : 0xFF232A36);
        g.fill(x, y, x + w, y + 1, 0xFF3B4557);
        int tw = Minecraft.getInstance().font.width(label);
        g.drawString(Minecraft.getInstance().font, label, x + (w - tw) / 2, y + 3, hover ? 0xFFFFFFFF : 0xFFD1D5DB, false);
    }

    private void drawTabs(GuiGraphics g, HudmcPanelState p, int mx, int my) {
        int x = p.x + 4;
        int y = p.y + HEADER_H;
        HudmcPanelState.TabType[] tabs = HudmcPanelState.TabType.values();
        String[] labels = {"Chat", "Miembros", "Configuración"};
        for (int i = 0; i < tabs.length; i++) {
            int w = tabWidth(i);
            boolean active = p.activeTab == tabs[i];
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + TAB_H;
            g.fill(x, y + 1, x + w, y + TAB_H, active ? 0xFF2D3748 : (hover ? 0xFF252C38 : 0xFF1A202B));
            if (active) g.fill(x, y + TAB_H - 1, x + w, y + TAB_H, 0xFF8B5CF6);
            g.drawString(Minecraft.getInstance().font, labels[i], x + 4, y + 4, active ? 0xFFFFFFFF : 0xFFCBD5E1, false);
            x += w + 2;
        }
    }

    public static int tabWidth(int idx) { return switch (idx) { case 0 -> 40; case 1 -> 62; default -> 86; }; }

    private void drawContent(GuiGraphics g, HudmcPanelState p, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x33101820);
        switch (p.activeTab) {
            case CHAT -> drawChat(g, p, x, y, w, h);
            case MIEMBROS -> drawMembers(g, p, x, y, w, h);
            case CONFIGURACION -> drawConfig(g, p, x, y, w, h);
        }
    }

    private void drawChat(GuiGraphics g, HudmcPanelState p, int x, int y, int w, int h) {
        var font = Minecraft.getInstance().font;
        NebulaDiscordSession s = NebulaDiscordSession.get();
        DiscordConnectModule mod = DiscordConnectModule.INSTANCE;
        if (mod != null) s.poll(mod.backendUrl());
        String status = s.status().name() + (s.statusDetail().isBlank() ? "" : " - " + s.statusDetail());
        g.drawString(font, trim(font, status, w - 12), x + 4, y + 4, statusColor(s.status()), false);

        int inputH = 18;
        int btnH = 16;
        int channelsH = 16;
        int top = y + 16;
        int msgBottom = y + h - inputH - btnH - channelsH - 10;
        int msgY = top;
        List<NebulaChatMessage> msgs = s.messages();
        int maxLines = Math.max(1, (msgBottom - top) / 10);
        int start = Math.max(0, msgs.size() - maxLines);
        for (int i = start; i < msgs.size(); i++) {
            NebulaChatMessage m = msgs.get(i);
            String line = "[" + shortTime(m.timestampMs()) + "] " + m.author() + ": " + m.content();
            g.drawString(font, trim(font, line, w - 10), x + 4, msgY, 0xFFDDE3EA, false);
            msgY += 10;
        }
        if (msgs.isEmpty()) g.drawString(font, trim(font, "Sin mensajes. Configura backend y autentica con Discord OAuth2.", w - 10), x + 4, top, 0xFF94A3B8, false);

        int chY = y + h - inputH - btnH - channelsH - 6;
        int cx = x + 2;
        for (String ch : s.channels()) {
            int cw = Math.min(72, font.width(ch) + 8);
            if (cx + cw > x + w - 2) break;
            boolean active = ch.equals(s.currentChannel());
            g.fill(cx, chY, cx + cw, chY + 14, active ? 0xFF334155 : 0xFF1F2937);
            g.drawString(font, trim(font, ch, cw - 6), cx + 4, chY + 3, active ? 0xFFFFFFFF : 0xFFCBD5E1, false);
            cx += cw + 2;
        }

        int inY = y + h - inputH - btnH - 4;
        g.fill(x + 2, inY, x + w - 62, inY + inputH, 0xFF111827);
        g.fill(x + 2, inY, x + w - 62, inY + 1, 0xFF334155);
        String draft = p.chatDraft == null ? "" : p.chatDraft;
        String shown = draft.isBlank() ? "Escribe mensaje..." : draft;
        g.drawString(font, trim(font, shown, w - 74), x + 6, inY + 5, draft.isBlank() ? 0xFF64748B : 0xFFE5E7EB, false);
        drawActionBtn(g, x + w - 58, inY, 56, inputH, "Enviar");

        int btnY = y + h - btnH - 2;
        String auth = s.isConnected() ? "Cerrar sesión" : "Iniciar Discord";
        drawActionBtn(g, x + 2, btnY, Math.min(110, w / 2 - 3), btnH, auth);
        drawActionBtn(g, x + 2 + Math.min(110, w / 2 - 3) + 4, btnY, Math.min(90, w - (Math.min(110, w / 2 - 3) + 8)), btnH, "Actualizar");
    }

    private void drawMembers(GuiGraphics g, HudmcPanelState p, int x, int y, int w, int h) {
        var font = Minecraft.getInstance().font;
        NebulaDiscordSession s = NebulaDiscordSession.get();
        g.drawString(font, trim(font, "Miembros conectados - " + s.currentChannel(), w - 8), x + 4, y + 4, 0xFFE5E7EB, false);
        g.drawString(font, trim(font, "Perfil: " + s.profile().displayName(), w - 8), x + 4, y + 16, 0xFF94A3B8, false);
        int yy = y + 30;
        for (String u : s.onlineUsers()) {
            if (yy > y + h - 12) break;
            g.drawString(font, trim(font, "- " + u, w - 10), x + 6, yy, 0xFFDDE3EA, false);
            yy += 10;
        }
        if (s.onlineUsers().isEmpty()) g.drawString(font, trim(font, "Sin miembros / backend no disponible.", w - 10), x + 6, yy, 0xFF64748B, false);
    }

    private void drawConfig(GuiGraphics g, HudmcPanelState p, int x, int y, int w, int h) {
        var font = Minecraft.getInstance().font;
        NebulaDiscordSession s = NebulaDiscordSession.get();
        DiscordConnectModule mod = DiscordConnectModule.INSTANCE;
        g.drawString(font, "Configuración DiscordConnect", x + 4, y + 4, 0xFFE5E7EB, false);
        g.drawString(font, trim(font, "Backend: " + (mod == null ? "módulo no disponible" : mod.backendUrl()), w - 8), x + 4, y + 18, 0xFF94A3B8, false);
        g.drawString(font, trim(font, "Estado: " + s.status() + " - " + s.statusDetail(), w - 8), x + 4, y + 30, statusColor(s.status()), false);
        g.drawString(font, trim(font, "Usuario: " + s.profile().displayName(), w - 8), x + 4, y + 42, 0xFFDDE3EA, false);
        g.drawString(font, trim(font, "OAuth2 oficial vía backend; sin bot/token en mod.", w - 8), x + 4, y + 58, 0xFF94A3B8, false);
        g.drawString(font, trim(font, "GitHub Pages: solo frontend estático HTTPS/WSS.", w - 8), x + 4, y + 70, 0xFF94A3B8, false);
        drawActionBtn(g, x + 4, y + h - 20, 110, 16, s.isConnected() ? "Cerrar sesión" : "Iniciar Discord");
        drawActionBtn(g, x + 118, y + h - 20, 90, 16, "Actualizar");
    }

    private void drawActionBtn(GuiGraphics g, int x, int y, int w, int h, String label) {
        var font = Minecraft.getInstance().font;
        g.fill(x, y, x + w, y + h, 0xFF263244);
        g.fill(x, y, x + w, y + 1, 0xFF3B4A61);
        g.drawString(font, trim(font, label, w - 6), x + 4, y + (h - 8) / 2, 0xFFE5E7EB, false);
    }

    private int statusColor(NebulaDiscordSession.Status st) {
        return switch (st) {
            case CONNECTED -> 0xFF4ADE80;
            case CONNECTING, AUTHENTICATING -> 0xFFFBBF24;
            case ERROR -> 0xFFF87171;
            case CONFIG_REQUIRED -> 0xFFF59E0B;
            default -> 0xFF94A3B8;
        };
    }

    private static String shortTime(long ts) {
        try { return new SimpleDateFormat("HH:mm").format(new Date(ts)); } catch (Exception e) { return "--:--"; }
    }

    private static String trim(net.minecraft.client.gui.Font font, String s, int w) {
        if (s == null) return "";
        if (font.width(s) <= w) return s;
        String out = s;
        while (out.length() > 1 && font.width(out + "...") > w) out = out.substring(0, out.length() - 1);
        return out + "...";
    }

    private void drawResizeHandle(GuiGraphics g, HudmcPanelState p) {
        if (p.minimized || p.maximized) return;
        int x = p.x + p.width - 10;
        int y = p.y + p.height - 10;
        g.fill(x, y, x + 8, y + 8, 0x55303A4D);
        g.fill(x + 5, y + 2, x + 6, y + 6, 0xFF94A3B8);
        g.fill(x + 2, y + 5, x + 6, y + 6, 0xFF94A3B8);
    }

    public boolean mouseClicked(HudmcPanelState p, MouseButtonEvent event, Actions actions, int screenW, int screenH) {
        double mx = event.x(), my = event.y();
        if (event.button() == 1) {
            int h = p.minimized ? HEADER_H : p.height;
            if (mx >= p.x && mx <= p.x + p.width && my >= p.y && my <= p.y + h) {
                actions.bringToFront(p);
                return true;
            }
            return false;
        }
        if (event.button() != 0) return false;
        int closeX = p.x + p.width - (BTN_W + 4);
        int maxX = closeX - (BTN_W + 2);
        int minX = maxX - (BTN_W + 2);
        int by = p.y + 4;
        if (in(mx, my, minX, by, BTN_W, BTN_W)) { toggleMinimize(p, screenW, screenH); actions.save(); actions.rebuild(); return true; }
        if (in(mx, my, maxX, by, BTN_W, BTN_W)) { toggleMaximize(p, screenW, screenH); actions.save(); actions.rebuild(); return true; }
        if (in(mx, my, closeX, by, BTN_W, BTN_W)) { actions.closePanel(p); actions.save(); actions.rebuild(); return true; }

        if (!p.minimized) {
            int tabX = p.x + 4;
            int tabY = p.y + HEADER_H;
            HudmcPanelState.TabType[] tabs = HudmcPanelState.TabType.values();
            for (int i = 0; i < tabs.length; i++) {
                int tw = tabWidth(i);
                if (in(mx, my, tabX, tabY, tw, TAB_H)) { p.activeTab = tabs[i]; actions.save(); actions.rebuild(); return true; }
                tabX += tw + 2;
            }
            if (handleContentClick(p, mx, my, actions)) return true;
        }
        return in(mx, my, p.x, p.y, p.width, HEADER_H);
    }

    private boolean handleContentClick(HudmcPanelState p, double mx, double my, Actions actions) {
        int x = p.x + 6;
        int y = p.y + HEADER_H + TAB_H + 4;
        int w = p.width - 12;
        int h = Math.max(20, p.y + p.height - y - 6);
        NebulaDiscordSession s = NebulaDiscordSession.get();
        if (p.activeTab == HudmcPanelState.TabType.CHAT) {
            int inputH = 18; int btnH = 16; int channelsH = 16;
            int chY = y + h - inputH - btnH - channelsH - 6;
            int cx = x + 2;
            var font = Minecraft.getInstance().font;
            for (String ch : s.channels()) {
                int cw = Math.min(72, font.width(ch) + 8);
                if (cx + cw > x + w - 2) break;
                if (in(mx, my, cx, chY, cw, 14)) { actions.setChatChannel(p, ch); return true; }
                cx += cw + 2;
            }
            int inY = y + h - inputH - btnH - 4;
            if (in(mx, my, x + w - 58, inY, 56, inputH)) { actions.sendChat(p); return true; }
            int btnY = y + h - btnH - 2;
            int authW = Math.min(110, w / 2 - 3);
            if (in(mx, my, x + 2, btnY, authW, btnH)) { if (s.isConnected()) actions.logoutDiscord(p); else actions.loginDiscord(p); return true; }
            int refX = x + 2 + authW + 4;
            int refW = Math.min(90, w - (authW + 8));
            if (in(mx, my, refX, btnY, refW, btnH)) { if (DiscordConnectModule.INSTANCE != null) s.poll(DiscordConnectModule.INSTANCE.backendUrl()); actions.rebuild(); return true; }
            if (in(mx, my, x + 2, inY, w - 64, inputH)) { actions.bringToFront(p); actions.rebuild(); return true; }
        } else if (p.activeTab == HudmcPanelState.TabType.MIEMBROS) {
            if (in(mx, my, x, y, w, h)) return true;
        } else if (p.activeTab == HudmcPanelState.TabType.CONFIGURACION) {
            int authW = 110; int btnY = y + h - 20;
            if (in(mx, my, x + 4, btnY, authW, 16)) { if (s.isConnected()) actions.logoutDiscord(p); else actions.loginDiscord(p); return true; }
            if (in(mx, my, x + 118, btnY, 90, 16)) { if (DiscordConnectModule.INSTANCE != null) s.poll(DiscordConnectModule.INSTANCE.backendUrl()); actions.rebuild(); return true; }
            if (in(mx, my, x, y, w, h)) return true;
        }
        return false;
    }

    public boolean isResizeHit(HudmcPanelState p, double mx, double my) {
        return !p.minimized && !p.maximized && mx >= p.x + p.width - 12 && mx <= p.x + p.width && my >= p.y + p.height - 12 && my <= p.y + p.height;
    }

    public boolean isHeaderHit(HudmcPanelState p, double mx, double my) {
        if (!in(mx, my, p.x, p.y, p.width, HEADER_H)) return false;
        int closeX = p.x + p.width - (BTN_W + 4);
        int maxX = closeX - (BTN_W + 2);
        int minX = maxX - (BTN_W + 2);
        int by = p.y + 4;
        return !(in(mx, my, minX, by, BTN_W, BTN_W) || in(mx, my, maxX, by, BTN_W, BTN_W) || in(mx, my, closeX, by, BTN_W, BTN_W));
    }

    public void toggleMinimize(HudmcPanelState p, int screenW, int screenH) {
        if (p.maximized) { restore(p, screenW, screenH); p.minimized = true; p.height = HEADER_H; return; }
        if (p.minimized) { p.minimized = false; p.height = Math.max(120, p.restoreHeight); }
        else { p.captureRestore(); p.minimized = true; }
        p.clampToScreen(screenW, screenH);
    }

    public void toggleMaximize(HudmcPanelState p, int screenW, int screenH) {
        if (p.maximized) { restore(p, screenW, screenH); return; }
        p.captureRestore();
        p.maximized = true;
        p.minimized = false;
        p.x = 8; p.y = 28; p.width = Math.max(220, screenW - 16); p.height = Math.max(120, screenH - 36);
        p.clampToScreen(screenW, screenH);
    }

    public void restore(HudmcPanelState p, int screenW, int screenH) {
        p.maximized = false;
        p.minimized = false;
        p.x = p.restoreX; p.y = p.restoreY; p.width = p.restoreWidth; p.height = p.restoreHeight;
        p.clampToScreen(screenW, screenH);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
}
