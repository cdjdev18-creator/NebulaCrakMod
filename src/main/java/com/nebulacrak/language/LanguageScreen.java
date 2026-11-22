package com.nebulacrak.language;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Button;
import net.minecraft.text.Text;

public class LanguageScreen extends Screen {
    private final Screen parent;

    public LanguageScreen(Screen parent) {
        super(Text.literal("NebulaCrakMod - Language"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 30;
        this.addRenderableWidget(Button.builder(Text.literal("English"), b -> {
            LanguageManager.set(LanguageManager.Language.EN_US);
            this.client.setScreen(this);
        }).bounds(centerX - 100, y, 200, 20).build());
        this.addRenderableWidget(Button.builder(Text.literal("Español (México)"), b -> {
            LanguageManager.set(LanguageManager.Language.ES_MX);
            this.client.setScreen(this);
        }).bounds(centerX - 100, y + 25, 200, 20).build());
        this.addRenderableWidget(Button.builder(Text.literal("Back"), b -> this.client.setScreen(parent)).bounds(centerX - 100, y + 60, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(context, mouseX, mouseY, partialTick);
        context.drawCenteredText(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        String current = "Current: " + LanguageManager.code();
        context.drawCenteredText(this.textRenderer, Text.literal(current), this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(context, mouseX, mouseY, partialTick);
    }
}