package com.nebulacrak.mixin;

import com.nebulacrak.modules.LoginCensorModule;
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
                LoginCensorModule lc = LoginCensorModule.INSTANCE;
                if (lc == null || !lc.shouldCensorInput(msg)) {
                    rc.onCommandWritten(msg);
                }
            }
        } catch (Throwable ignored) {}
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "handleChatInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V"))
    private void nebulacrak$safeAddRecentChat(net.minecraft.client.gui.components.ChatComponent instance, String msg) {
        String safe = msg;
        try {
            LoginCensorModule lc = LoginCensorModule.INSTANCE;
            if (lc != null && lc.isActive()) safe = lc.sanitizeForHistory(msg);
        } catch (Throwable ignored) {}
        instance.addRecentChat(safe);
    }
}
