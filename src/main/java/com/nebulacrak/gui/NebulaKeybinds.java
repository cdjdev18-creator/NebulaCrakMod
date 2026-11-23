package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class NebulaKeybinds {
    public static final KeyBinding OPEN_GUI = new KeyBinding(
        "key.nebulacrakmod.gui",
        KeyBinding.Type.KEYSYM,
        NebulaConfig.openKeyCode,
        KeyBinding.Category.MISC
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }

    public static void updateKeyBinding() {
        try {
            // Update the key code using reflection on the internal field
            java.lang.reflect.Field keyField = KeyBinding.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(OPEN_GUI, NebulaConfig.openKeyCode);
        } catch (Exception e) {
            // Fallback: ignore
        }
    }

    public static void handleKeyPress() {
        while (OPEN_GUI.wasPressed()) {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            if (minecraft.currentScreen == null) {
                minecraft.setScreen(new NebulaGUIScreen(null));
            } else if (minecraft.currentScreen instanceof NebulaGUIScreen) {
                if (NebulaConfig.closeWithOpenKey) {
                    minecraft.setScreen(null);
                }
            }
        }
    }
}