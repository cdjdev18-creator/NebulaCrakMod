package com.nebulacrak.gui;

import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModuleBrowserScreen extends Screen {
    private final Screen parent;
    private Category selectedCategory;
    private Module selectedModule;
    private String search = "";
    private EditBox searchBox;

    public ModuleBrowserScreen(Screen parent) {
        super(Component.literal("NebulaCrakMod - Module Browser"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        this.children().clear();
        this.clearWidgets();

        searchBox = new EditBox(this.font, 110, 10, 180, 20, Component.literal("Search"));
        searchBox.setValue(search);
        searchBox.setResponder(v -> { search = v.toLowerCase(); rebuild(); });
        this.addRenderableWidget(searchBox);

        int y = 35;
        for (Category category : Modules.loopCategories()) {
            final Category c = category;
            this.addRenderableWidget(Button.builder(Component.literal(category.name), b -> {
                selectedCategory = c;
                selectedModule = null;
                rebuild();
            }).bounds(10, y, 90, 20).build());
            y += 24;
        }
        if (selectedCategory == null && Modules.loopCategories().iterator().hasNext()) {
            selectedCategory = Modules.loopCategories().iterator().next();
        }

        int x = 110;
        int my = 40;
        for (Module module : filteredModules()) {
            final Module m = module;
            String label = module.name + (module.isActive() ? " [ON]" : " [OFF]");
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selectedModule = m;
                m.toggle();
                rebuild();
            }).bounds(x, my, 180, 20).build());
            my += 24;
        }

        if (selectedModule != null) {
            int sx = 310;
            int sy = 40;
            for (Setting<?> setting : selectedModule.settings.getDefaultGroup()) {
                if (setting instanceof BoolSetting) {
                    BoolSetting bool = (BoolSetting) setting;
                    String label = setting.name + ": " + bool.get();
                    this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                        bool.set(!bool.get());
                        rebuild();
                    }).bounds(sx, sy, 220, 20).build());
                    sy += 24;
                }
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent)).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private List<Module> filteredModules() {
        List<Module> result = new ArrayList<>();
        if (selectedCategory == null) return result;
        for (Module module : Modules.get().getGroup(selectedCategory)) {
            if (search.isEmpty() || module.name.toLowerCase().contains(search)) result.add(module);
        }
        return result;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
