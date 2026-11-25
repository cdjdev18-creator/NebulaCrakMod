package com.nebulacrak.modules;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.Minecraft;

public class StreamPrivacyModule extends Module {
    public static StreamPrivacyModule INSTANCE;

    private static RenderTarget privacyA;
    private static int lastW;
    private static int lastH;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPixelation = settings.createGroup("Pixelation");
    private final SettingGroup sgBlur = settings.createGroup("Blur");
    private final SettingGroup sgZoom = settings.createGroup("Zoom");
    private final SettingGroup sgPosition = settings.createGroup("Position");

    // General
    public final Setting<Boolean> pixelationEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("pixelation")
        .description("Enable pixelation in the privacy region.")
        .defaultValue(true)
        .build());

    public final Setting<Boolean> blurEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("blur")
        .description("Enable blur-like smoothing in the privacy region.")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> zoomEnabledSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("zoom")
        .description("Enable zoom for the protected region.")
        .defaultValue(false)
        .build());

    // Pixelation
    public final Setting<Integer> pixelSize = sgPixelation.add(new IntSetting.Builder()
        .name("pixel-size")
        .description("Pixel block size. Lower = smaller, sharper pixels.")
        .defaultValue(6)
        .min(1)
        .max(64)
        .sliderRange(1, 32)
        .build());

    public final Setting<Integer> pixelationIntensity = sgPixelation.add(new IntSetting.Builder()
        .name("pixelation-intensity")
        .description("Pixelation strength. Higher = coarser masking.")
        .defaultValue(100)
        .min(0)
        .max(100)
        .sliderRange(0, 100)
        .build());

    // Blur
    public final Setting<Integer> blurIntensity = sgBlur.add(new IntSetting.Builder()
        .name("blur-intensity")
        .description("Blur strength. Higher = stronger blur.")
        .defaultValue(35)
        .min(0)
        .max(100)
        .sliderRange(0, 100)
        .build());

    // Zoom
    public final Setting<Boolean> enableZoom = sgZoom.add(new BoolSetting.Builder()
        .name("enable-zoom")
        .description("Allow zooming the protected region.")
        .defaultValue(false)
        .build());

    public final Setting<Double> zoomLevel = sgZoom.add(new DoubleSetting.Builder()
        .name("zoom-level")
        .description("Zoom level for the protected region.")
        .defaultValue(1.5)
        .min(1.0)
        .max(6.0)
        .sliderRange(1.0, 4.0)
        .visible(enableZoom::get)
        .build());

    // Position (screen-relative percentages)
    public final Setting<Integer> positionX = sgPosition.add(new IntSetting.Builder()
        .name("position-x")
        .description("Horizontal position of the protected region (% of screen width).")
        .defaultValue(2)
        .min(0)
        .max(100)
        .sliderRange(0, 100)
        .build());

    public final Setting<Integer> positionY = sgPosition.add(new IntSetting.Builder()
        .name("position-y")
        .description("Vertical position of the protected region (% of screen height).")
        .defaultValue(74)
        .min(0)
        .max(100)
        .sliderRange(0, 100)
        .build());

    public final Setting<Integer> width = sgPosition.add(new IntSetting.Builder()
        .name("width")
        .description("Width of the protected region (% of screen width).")
        .defaultValue(28)
        .min(5)
        .max(100)
        .sliderRange(5, 100)
        .build());

    public final Setting<Integer> height = sgPosition.add(new IntSetting.Builder()
        .name("height")
        .description("Height of the protected region (% of screen height).")
        .defaultValue(24)
        .min(5)
        .max(100)
        .sliderRange(5, 100)
        .build());

    public final Setting<Boolean> resetPosition = sgPosition.add(new BoolSetting.Builder()
        .name("reset-position")
        .description("Reset protected region position and size to defaults.")
        .defaultValue(false)
        .build());

    public StreamPrivacyModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "StreamPrivacy", "Visual privacy layer with configurable pixelation, blur, zoom, and position.");
        INSTANCE = this;
    }

    @Override
    public void onDeactivate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.execute(StreamPrivacyModule::releasePrivacyTarget);
        else releasePrivacyTarget();
    }

    @Override
    public void onActivate() {
        resetPosition.set(false);
    }

    public static RenderTarget ensurePrivacyTarget(int w, int h) {
        if (privacyA != null && lastW == w && lastH == h) return privacyA;
        releasePrivacyTarget();
        privacyA = new TextureTarget("NebulaStreamPrivacyA", w, h, false);
        lastW = w;
        lastH = h;
        return privacyA;
    }

    public static void releasePrivacyTarget() {
        if (privacyA != null) {
            privacyA.destroyBuffers();
            privacyA = null;
        }
        lastW = 0;
        lastH = 0;
    }

    public boolean isPixelationEnabled() { return pixelationEnabled.get(); }
    public boolean isBlurEnabled() { return blurEnabled.get(); }
    public boolean isZoomActive() { return zoomEnabledSetting.get() && enableZoom.get(); }
    public int pixelSize() { return pixelSize.get(); }
    public int pixelationIntensity() { return pixelationIntensity.get(); }
    public int blurIntensity() { return blurIntensity.get(); }
    public double zoomLevel() { return zoomLevel.get(); }
    public int positionX() { return positionX.get(); }
    public int positionY() { return positionY.get(); }
    public int widthPercent() { return width.get(); }
    public int heightPercent() { return height.get(); }
}
