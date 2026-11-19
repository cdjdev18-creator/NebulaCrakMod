package com.nebulacrak;

import com.nebulacrak.gui.NebulaKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class NebulaCrakModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NebulaKeybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NebulaKeybinds.handleKeyPress();
        });
    }
}