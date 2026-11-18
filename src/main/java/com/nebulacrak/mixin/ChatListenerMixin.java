package com.nebulacrak.mixin;

import com.mojang.authlib.GameProfile;
import com.nebulacrak.modules.MuteChatPlusModule;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void nebulacrak$muteChat(PlayerChatMessage message, GameProfile sender, ChatType.Bound type, CallbackInfo ci) {
        MuteChatPlusModule module = MuteChatPlusModule.INSTANCE;
        if (module == null || !module.isActive() || !module.muteChat.get()) return;
        if (Minecraft.getInstance().player != null && sender.name().equals(Minecraft.getInstance().player.getName().getString())) return;
        if (Friends.get().get(sender.name()) != null) return;
        ci.cancel();
    }
}
