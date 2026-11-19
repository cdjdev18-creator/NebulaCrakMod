package com.nebulacrak.gui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class NebulaKeybinds {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.nebulacrakmod.gui",
        GLFW.GLFW_KEY_GRAVE_ACCENT,
        KeyMapping.Category.MISC
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }

    public static void handleKeyPress() {
        while (OPEN_GUI.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new NebulaGUIScreen(null));
            } else if (minecraft.screen instanceof NebulaGUIScreen) {
                minecraft.setScreen(null);
            }
        }
    }
}