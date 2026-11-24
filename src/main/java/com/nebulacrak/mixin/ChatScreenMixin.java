package com.nebulacrak.mixin;

import com.nebulacrak.modules.RangosChangeModule;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"))
    private void nebulacrak$observeChatInput(String msg, boolean addToRecent, CallbackInfo ci) {
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && msg != null && msg.startsWith("/")) {
                rc.onCommandWritten(msg);
            }
        } catch (Throwable ignored) {}
    }
}
