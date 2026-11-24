package com.nebulacrak.mixin;

import com.nebulacrak.modules.RangosChangeModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void nebulacrak$observeCommand(String command, CallbackInfo ci) {
        try {
            RangosChangeModule rc = RangosChangeModule.INSTANCE;
            if (rc != null && rc.isActive() && command != null) {
                rc.onCommandSent("/" + command);
            }
        } catch (Throwable ignored) {}
    }
}
