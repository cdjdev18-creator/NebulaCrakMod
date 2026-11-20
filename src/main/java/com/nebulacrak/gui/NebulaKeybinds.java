package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class NebulaKeybinds {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.nebulacrakmod.gui",
        NebulaConfig.openKeyCode,
        KeyMapping.Category.MISC
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }

    public static void updateKeyBinding() {
        // Re-create the key mapping with the new key code
        // Note: This is a workaround since KeyMapping.setKey() requires a Key object
        // which is not available in official Mojang mappings
        try {
            java.lang.reflect.Field keyField = KeyMapping.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(OPEN_GUI, NebulaConfig.openKeyCode);
        } catch (Exception e) {
            // Fallback: ignore
        }
    }

    public static void handleKeyPress() {
        while (OPEN_GUI.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new NebulaGUIScreen(null));
            } else if (minecraft.screen instanceof NebulaGUIScreen) {
                if (NebulaConfig.closeWithOpenKey) {
                    minecraft.setScreen(null);
                }
            }
        }
    }
}