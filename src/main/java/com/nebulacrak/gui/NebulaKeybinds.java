package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class NebulaKeybinds {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.nebulacrakmod.gui",
        InputConstants.Type.KEYSYM,
        NebulaConfig.openKeyCode,
        KeyMapping.Category.MISC
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }

    public static void updateKeyBinding() {
        try {
            // Update the key code using reflection on the internal field
            java.lang.reflect.Field keyField = KeyMapping.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(OPEN_GUI, InputConstants.Type.KEYSYM.getOrCreate(NebulaConfig.openKeyCode));
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