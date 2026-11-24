package com.nebulacrak.mixin;

import com.mojang.authlib.GameProfile;
import com.nebulacrak.modules.MuteChatPlusModule;
import com.nebulacrak.modules.RangosChangeModule;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerFilterMixin {

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$mutePlayerChat(PlayerChatMessage message, GameProfile sender, ChatType.Bound type, CallbackInfo ci) {
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && message != null) {
                String text = message.decoratedContent() != null ? message.decoratedContent().getString() : message.signedContent();
                rc.handleServerMessage((sender != null ? sender.name() + ": " : "") + text);
            }
        } catch (Throwable ignored) {}

        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null) return;
        if (sender == null) return; // fail-open: unverifiable sender
        try {
            if (module.shouldMutePlayer(sender.id(), sender.name())) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // fail-open
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$muteDisguisedPlayerChat(Component message, ChatType.Bound boundChatType, CallbackInfo ci) {
        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null || !module.isActive() || !module.muteChat.get()) return;
        try {
            // Disguised player chat can carry sender name in bound chat type; fail-open if not identifiable.
            GameProfile sender = boundChatType.name().getString() != null ? null : null;
        } catch (Throwable ignored) {
        }
        // Do not block disguised/system-styled messages without reliable sender identity.
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"))
    private void nebulacrak$observeSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && message != null) {
                rc.handleServerMessage(message.getString());
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"))
    private void nebulacrak$observeDisguised(Component message, ChatType.Bound boundChatType, CallbackInfo ci) {
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && message != null) {
                rc.handleServerMessage(message.getString());
            }
        } catch (Throwable ignored) {}
    }
}
