package com.nebulacrak.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.nebulacrak.config.NebulaConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class NebulaKeybinds {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("nebulacrakmod", "main"));

    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.nebulacrakmod.gui",
        InputConstants.Type.KEYSYM,
        NebulaConfig.openKeyCode,
        CATEGORY
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }

    public static void updateKeyBinding() {
        try {
            OPEN_GUI.setKey(InputConstants.Type.KEYSYM.getOrCreate(NebulaConfig.openKeyCode));
            KeyMapping.resetMapping();
        } catch (Exception ignored) {
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