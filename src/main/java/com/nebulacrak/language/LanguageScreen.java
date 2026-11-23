package com.nebulacrak.language;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LanguageScreen extends Screen {
    private final Screen parent;

    public LanguageScreen(Screen parent) {
        super(Component.literal("NebulaCrakMod - Language"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 30;
        this.addRenderableWidget(Button.builder(Component.literal("English"), b -> {
            LanguageManager.set(LanguageManager.Language.EN_US);
            Minecraft.getInstance().setScreen(new LanguageScreen(parent));
        }).bounds(centerX - 100, y, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Español (México)"), b -> {
            LanguageManager.set(LanguageManager.Language.ES_MX);
            Minecraft.getInstance().setScreen(new LanguageScreen(parent));
        }).bounds(centerX - 100, y + 25, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> Minecraft.getInstance().setScreen(parent)).bounds(centerX - 100, y + 60, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(context, mouseX, mouseY, partialTick);
        context.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        String current = "Current: " + LanguageManager.code();
        context.drawCenteredString(this.font, Component.literal(current), this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(context, mouseX, mouseY, partialTick);
    }
}
