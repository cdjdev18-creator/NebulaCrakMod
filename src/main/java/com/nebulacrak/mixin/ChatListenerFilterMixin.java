package com.nebulacrak.mixin;

import com.nebulacrak.modules.MuteChatPlusModule;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatListener.class)
public class ChatListenerFilterMixin {
    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void onPlayerChatMessage(PlayerChatMessage message, com.mojang.authlib.GameProfile sender, ChatType.Bound type, CallbackInfo ci) {
        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null || !module.isActive() || !module.muteChat.get()) {
            return; // Módulo desactivado → permitir
        }

        if (sender == null) {
            return; // Sin remitente verificable → permitir (fail-open)
        }

        String senderName = sender.name();
        UUID senderUuid = sender.id();
        var player = MinecraftClient.getInstance().player;

        // Jugador local → siempre permitir
        if (player != null && senderUuid != null && senderUuid.equals(player.getUuid())) {
            return;
        }

        // Consultar Meteor Friends
        boolean friend = false;
        try {
            friend = Friends.get().get(senderName) != null;
        } catch (Exception e) {
            // Fail-open: permitir si falla la consulta
        }

        if (friend) {
            return; // Amigo → permitir
        }

        // Desconocido → BLOQUEAR
        ci.cancel();
    }
}