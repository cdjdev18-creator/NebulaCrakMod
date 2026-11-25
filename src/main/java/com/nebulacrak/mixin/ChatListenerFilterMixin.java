package com.nebulacrak.mixin;

import com.mojang.authlib.GameProfile;
import com.nebulacrak.modules.ChatSendViewModule;
import com.nebulacrak.modules.MuteChatPlusModule;
import com.nebulacrak.modules.RangosChangeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatListener.class)
public class ChatListenerFilterMixin {

    @Unique private static final Pattern JOIN_LEAVE_NAME = Pattern.compile("^\\s*([A-Za-z0-9_*.]{1,32})\\s+(joined|left)\\b", Pattern.CASE_INSENSITIVE);
    @Unique private static final Pattern PRIVATE_MSG_NAME = Pattern.compile("^\\s*(?:\\[.*?\\]\\s*)?(?:from|to|de|para|whisper(?:\\s+from)?|msg(?:\\s+from)?)\\s+([A-Za-z0-9_*.]{1,32})\\b", Pattern.CASE_INSENSITIVE);

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$handlePlayerChat(PlayerChatMessage message, GameProfile sender, ChatType.Bound type, CallbackInfo ci) {
        String text = null;
        try {
            if (message != null) {
                text = message.decoratedContent() != null ? message.decoratedContent().getString() : message.signedContent();
            }
        } catch (RuntimeException ignored) {}

        // RangosChange observation must be preserved
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && text != null) {
                String senderName = sender != null ? sender.name() : "";
                rc.handleServerMessage((senderName == null || senderName.isBlank() ? "" : senderName + ": ") + text);
            }
        } catch (RuntimeException ignored) {}

        if (sender == null) return; // fail-open: unverifiable sender

        try {
            String name = sender.name();
            UUID uuid = sender.id();
            MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
            boolean muted = module != null && module.isActive() && module.shouldMutePlayerChat(uuid, name);
            if (muted) {
                ci.cancel();
                module.recordHiddenChat();
            } else {
                ChatSendViewModule csv = ChatSendViewModule.INSTANCE;
                if (csv != null && csv.isActive() && text != null) {
                    csv.onPlayerChat(uuid, name, text);
                }
            }
        } catch (RuntimeException ignored) {
            // fail-open
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$handleDisguised(Component message, ChatType.Bound boundChatType, CallbackInfo ci) {
        String msgText = null;
        try { msgText = message != null ? message.getString() : null; } catch (RuntimeException ignored) {}

        // Preserve RangosChange observation
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && msgText != null) {
                rc.handleServerMessage(msgText);
            }
        } catch (RuntimeException ignored) {}

        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null || !module.isActive()) return;

        // Fail-open: disguised messages do not provide reliable sender identity in all cases.
        // Only attempt filtering when we can confidently extract a player name and the message is clearly private.
        try {
            if (msgText != null && module.mutePrivateMessages.get()) {
                String name = nebulacrak$extractPrivateSender(msgText);
                if (name != null && nebulacrak$isKnownPlayer(name)) {
                    if (module.shouldMutePrivateMessage(null, name)) {
                        ci.cancel();
                        module.recordHiddenPrivate();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // fail-open
        }
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$handleSystem(Component message, boolean overlay, CallbackInfo ci) {
        String msgText = null;
        try { msgText = message != null ? message.getString() : null; } catch (RuntimeException ignored) {}

        // Preserve RangosChange observation
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && msgText != null) {
                rc.handleServerMessage(msgText);
            }
        } catch (RuntimeException ignored) {}

        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null || !module.isActive() || msgText == null) return;

        try {
            // Join/leave: only when clearly identifiable from a strict pattern.
            String joinLeaveName = nebulacrak$extractJoinLeaveName(msgText);
            if (joinLeaveName != null) {
                boolean isJoin = nebulacrak$isJoinMessage(msgText);
                boolean isLeave = nebulacrak$isLeaveMessage(msgText);
                if ((isJoin && module.isJoinMuteEnabled()) || (isLeave && module.isLeaveMuteEnabled())) {
                    if (module.shouldMuteJoinLeave(joinLeaveName)) {
                        ci.cancel();
                        module.recordHiddenJoinLeave();
                        return;
                    }
                }
                return; // identifiable system join/leave but not muted/disabled: fail-open
            }

            // Private messages in system channel: only when confidently identifiable
            if (module.mutePrivateMessages.get()) {
                String pmName = nebulacrak$extractPrivateSender(msgText);
                if (pmName != null) {
                    if (module.shouldMutePrivateMessage(null, pmName)) {
                        ci.cancel();
                        module.recordHiddenPrivate();
                        return;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // fail-open
        }
        // All other system messages: fail-open
    }

    @Unique
    private static String nebulacrak$extractJoinLeaveName(String msg) {
        if (msg == null) return null;
        Matcher m = JOIN_LEAVE_NAME.matcher(msg);
        if (m.find()) return m.group(1);
        return null;
    }

    @Unique
    private static boolean nebulacrak$isJoinMessage(String msg) {
        if (msg == null) return false;
        String l = msg.toLowerCase(Locale.ROOT);
        return l.contains(" joined ") || l.endsWith(" joined") || l.contains("se ha unido") || l.contains("entró") || l.contains("entro");
    }

    @Unique
    private static boolean nebulacrak$isLeaveMessage(String msg) {
        if (msg == null) return false;
        String l = msg.toLowerCase(Locale.ROOT);
        return l.contains(" left ") || l.endsWith(" left") || l.contains("se ha ido") || l.contains("salió") || l.contains("salio");
    }

    @Unique
    private static String nebulacrak$extractPrivateSender(String msg) {
        if (msg == null) return null;
        Matcher m = PRIVATE_MSG_NAME.matcher(msg);
        if (m.find()) return m.group(1);
        return null;
    }

    @Unique
    private static boolean nebulacrak$isKnownPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) return false;
            for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
                if (info == null || info.getProfile() == null) continue;
                String n = info.getProfile().name();
                if (n != null && n.equalsIgnoreCase(name)) return true;
            }
        } catch (RuntimeException ignored) {}
        return false;
    }
}
