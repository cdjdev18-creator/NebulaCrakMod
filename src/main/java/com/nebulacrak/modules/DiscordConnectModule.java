package com.nebulacrak.modules;

import com.nebulacrak.discord.NebulaDiscordSession;
import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DiscordConnect: Nebula chat integration using official Discord OAuth2 through an independent backend.
 * No Discord bot, no self-bots, no Discord passwords/tokens stored in mod.
 */
public class DiscordConnectModule extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("NebulaCrakMod/DiscordConnect");
    public static DiscordConnectModule INSTANCE;

    private final SettingGroup sgConfig = settings.getDefaultGroup();
    private final SettingGroup sgAuth = settings.createGroup("OAuth2/Session");
    private final SettingGroup sgChat = settings.createGroup("Chat Nebula");
    private final SettingGroup sgStatus = settings.createGroup("Estado");

    private final Setting<String> backendApiUrl = sgConfig.add(new StringSetting.Builder()
        .name("backend-api-url")
        .description("URL base del backend Nebula (HTTPS en producción, localhost permitido en dev). Gestiona OAuth2, sesiones, canales y chat.")
        .defaultValue("")
        .build());
    private final Setting<String> frontendUrl = sgConfig.add(new StringSetting.Builder()
        .name("frontend-url")
        .description("Opcional: frontend estático (GitHub Pages) que inicia OAuth2. No debe contener secretos.")
        .defaultValue("")
        .build());

    private final Setting<String> sessionToken = sgAuth.add(new StringSetting.Builder()
        .name("session-token")
        .description("Token de sesión Nebula devuelto por backend tras OAuth2 Discord. No es token de Discord ni contraseña. Pégalo aquí tras autorizar en navegador.")
        .defaultValue("")
        .build());
    private final Setting<Boolean> loginDiscord = sgAuth.add(new BoolSetting.Builder()
        .name("iniciar-sesion-discord")
        .description("Abre flujo OAuth2 oficial en navegador vía backend. No solicita contraseña de Discord.")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> applySession = sgAuth.add(new BoolSetting.Builder()
        .name("aplicar-sesion")
        .description("Valida session-token contra backend y vincula perfil.")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> logout = sgAuth.add(new BoolSetting.Builder()
        .name("cerrar-sesion")
        .description("Cierra sesión Nebula local/remota.")
        .defaultValue(false)
        .build());

    private final Setting<String> channel = sgChat.add(new StringSetting.Builder()
        .name("canal")
        .description("Canal Nebula: #general, #minecraft, #anuncios u otros provistos por backend.")
        .defaultValue("#general")
        .build());
    private final Setting<String> message = sgChat.add(new StringSetting.Builder()
        .name("mensaje")
        .description("Mensaje para enviar al chat Nebula autenticado.")
        .defaultValue("")
        .build());
    private final Setting<Boolean> send = sgChat.add(new BoolSetting.Builder()
        .name("enviar-mensaje")
        .description("Envía mensaje al backend (requiere sesión autenticada).")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> refresh = sgChat.add(new BoolSetting.Builder()
        .name("actualizar")
        .description("Actualiza historial/canales/miembros desde backend.")
        .defaultValue(false)
        .build());

    private final Setting<String> status = sgStatus.add(new StringSetting.Builder()
        .name("estado")
        .description("Estado real de conexión/autenticación.")
        .defaultValue("Desconectado")
        .build());
    private final Setting<String> profile = sgStatus.add(new StringSetting.Builder()
        .name("perfil")
        .description("Perfil Discord vinculado por backend.")
        .defaultValue("No conectado")
        .build());
    private final Setting<String> lastError = sgStatus.add(new StringSetting.Builder()
        .name("ultimo-resultado")
        .description("Último resultado o error comprensible.")
        .defaultValue("Requiere backend Nebula configurado")
        .build());

    private boolean resetting;

    public DiscordConnectModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "DiscordConnect", "Chat Nebula con Discord OAuth2 vía backend independiente. Sin bot, sin self-bots, sin contraseña/token Discord en el mod.");
        INSTANCE = this;
    }

    public String backendUrl() { return NebulaDiscordSession.normalizeBase(backendApiUrl.get()); }
    public String frontendUrl() { return NebulaDiscordSession.normalizeBase(frontendUrl.get()); }
    public String sessionToken() { return sessionToken.get() == null ? "" : sessionToken.get().trim(); }
    public void setSessionToken(String token) { sessionToken.set(token == null ? "" : token.trim()); NebulaDiscordSession.get().applySessionToken(backendUrl(), sessionToken.get()); }
    public String channel() { return channel.get() == null ? "#general" : channel.get().trim(); }
    public String messageDraft() { return message.get() == null ? "" : message.get(); }
    public void setChannel(String ch) { if (ch != null && !ch.isBlank()) { channel.set(ch); NebulaDiscordSession.get().setCurrentChannel(ch); } }
    public void setMessageDraft(String m) { message.set(m == null ? "" : m); }
    public void triggerLogin() { NebulaDiscordSession.get().beginLogin(backendUrl(), ""); }
    public void triggerLogout() { NebulaDiscordSession.get().logout(backendUrl()); }
    public void triggerSend() { NebulaDiscordSession.get().sendMessage(backendUrl(), message.get()); message.set(""); }
    public void triggerRefresh() { NebulaDiscordSession.get().poll(backendUrl()); }

    @Override
    public void onActivate() {
        NebulaDiscordSession s = NebulaDiscordSession.get();
        if (!backendUrl().isBlank() && !sessionToken().isBlank() && !s.isConnected()) {
            s.applySessionToken(backendUrl(), sessionToken());
        }
        syncStatus();
    }

    @Override
    public void onDeactivate() {
        syncStatus();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;
        NebulaDiscordSession s = NebulaDiscordSession.get();
        if (loginDiscord.get()) { reset(loginDiscord); triggerLogin(); }
        if (applySession.get()) { reset(applySession); s.applySessionToken(backendUrl(), sessionToken()); }
        if (logout.get()) { reset(logout); sessionToken.set(""); triggerLogout(); }
        if (send.get()) { reset(send); triggerSend(); }
        if (refresh.get()) { reset(refresh); triggerRefresh(); }
        if (channel.get() != null && !channel.get().equals(s.currentChannel())) s.setCurrentChannel(channel.get());
        syncStatus();
    }

    private void syncStatus() {
        NebulaDiscordSession s = NebulaDiscordSession.get();
        status.set(s.status().name());
        profile.set(s.profile().displayName());
        lastError.set(s.statusDetail());
    }

    private void reset(Setting<Boolean> s) {
        if (resetting) return;
        resetting = true;
        try { s.set(false); } finally { resetting = false; }
    }
}
