package com.nebulacrak.gui;

import com.nebulacrak.config.NebulaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.File;

public final class NebulaScreenBackgrounds {
    private NebulaScreenBackgrounds() {}

    public static boolean render(GuiGraphics g, int width, int height, String screenId) {
        NebulaConfig.ScreenBackground bg = NebulaConfig.getScreenBackground(screenId);
        NebulaConfig.BackgroundMode mode = bg.mode();
        if (mode == null || mode == NebulaConfig.BackgroundMode.ORIGINAL) return false;

        if (mode == NebulaConfig.BackgroundMode.TRANSPARENT) {
            return true; // skip original background drawing
        }

        if (mode == NebulaConfig.BackgroundMode.COLOR) {
            g.fill(0, 0, width, height, bg.color());
            return true;
        }

        if (mode == NebulaConfig.BackgroundMode.IMAGE) {
            String path = bg.imagePath();
            if (path != null && !path.isBlank() && new File(path).exists()) {
                Identifier tex = NebulaTextureManager.loadTextureFromFile("screen_" + screenId, path);
                if (tex != null) {
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0, 0, width, height, width, height);
                    return true;
                }
            }
            g.fill(0, 0, width, height, bg.color());
            return true;
        }
        return false;
    }

    public static boolean render(GuiGraphics g, String screenId) {
        Minecraft mc = Minecraft.getInstance();
        return render(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), screenId);
    }

    public static String screenIdFor(Screen screen) {
        if (screen == null) return null;
        if (screen instanceof TitleScreen) return "mainMenu";
        if (screen instanceof SelectWorldScreen) return "worldSelect";
        if (screen instanceof CreateWorldScreen) return "worldCreate";
        if (screen instanceof JoinMultiplayerScreen) return "multiplayer";
        if (screen instanceof OptionsScreen || screen instanceof OptionsSubScreen) return "options";
        String name = screen.getClass().getName();
        if (name.contains("Singleplayer") || name.contains("SelectWorld")) return "worldSelect";
        if (name.contains("Multiplayer") || name.contains("JoinMultiplayer")) return "multiplayer";
        if (name.contains("CreateWorld") || name.contains("WorldCreation")) return "worldCreate";
        if (name.contains("Options") || name.contains("Controls") || name.contains("Video") || name.contains("Sound") || name.contains("Language")) return "options";
        if (name.contains("DisconnectedScreen")) return "disconnected";
        if (name.contains("ConnectScreen") || name.contains("ServerReconfigScreen")) return "connecting";
        if (name.contains("LevelLoadingScreen") || name.contains("ProgressScreen") || name.contains("GenericMessageScreen") || name.contains("ReceivingLevelScreen") || name.contains("GenericWaitingScreen")) return "worldLoading";
        if (name.contains("PauseScreen")) return "options";
        return null;
    }
}
