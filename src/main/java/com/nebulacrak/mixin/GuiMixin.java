package com.nebulacrak.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.nebulacrak.modules.StreamPrivacyModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("TAIL"))
    private void nebulacrak$streamPrivacy(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        try {
            StreamPrivacyModule mod = StreamPrivacyModule.INSTANCE;
            if (mod == null || !mod.isActive()) return;
            if (mod.resetPosition.get()) {
                mod.positionX.set(2);
                mod.positionY.set(74);
                mod.width.set(28);
                mod.height.set(24);
                mod.resetPosition.set(false);
            }
            if (minecraft == null || minecraft.getMainRenderTarget() == null) return;
            RenderTarget main = minecraft.getMainRenderTarget();
            if (main.getColorTexture() == null || main.getColorTextureView() == null) return;

            int sw = graphics.guiWidth();
            int sh = graphics.guiHeight();
            if (sw <= 0 || sh <= 0) return;

            int w = Mth.clamp(sw * Mth.clamp(mod.widthPercent(), 5, 100) / 100, 16, sw);
            int h = Mth.clamp(sh * Mth.clamp(mod.heightPercent(), 5, 100) / 100, 16, sh);
            int x = Mth.clamp(sw * Mth.clamp(mod.positionX(), 0, 100) / 100, 0, Math.max(0, sw - w));
            int y = Mth.clamp(sh * Mth.clamp(mod.positionY(), 0, 100) / 100, 0, Math.max(0, sh - h));
            if (w <= 0 || h <= 0) return;

            RenderTarget privacyA = StreamPrivacyModule.ensurePrivacyTarget(sw, sh);
            if (privacyA == null || privacyA.getColorTexture() == null) return;

            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorTexture(), privacyA.getColorTexture(), 0, 0, 0, 0, 0, sw, sh
            );

            boolean didEffect = false;
            if (mod.isPixelationEnabled() && mod.pixelationIntensity() > 0) {
                nebulacrak$drawPixelated(graphics, privacyA, x, y, w, h, sw, sh, mod);
                didEffect = true;
            }
            if (mod.isBlurEnabled() && mod.blurIntensity() > 0) {
                nebulacrak$drawBlur(graphics, privacyA, x, y, w, h, sw, sh, mod.blurIntensity());
                didEffect = true;
            }
            if (!didEffect) {
                nebulacrak$drawPixelatedFallback(graphics, privacyA, x, y, w, h, sw, sh, 8);
            }
        } catch (Throwable ignored) {
            // fail-open: never crash render loop
        }
    }

    @Unique
    private static void nebulacrak$drawPixelated(GuiGraphics graphics, RenderTarget src, int x, int y, int w, int h, int sw, int sh, StreamPrivacyModule mod) {
        int base = Mth.clamp(mod.pixelSize(), 1, 64);
        int intensity = Mth.clamp(mod.pixelationIntensity(), 0, 100);
        int block = Mth.clamp(Math.round(base * (0.35f + intensity / 100f * 1.65f)), 2, 96);

        int srcW = w;
        int srcH = h;
        int srcX = x;
        int srcY = y;
        if (mod.isZoomActive()) {
            double z = Mth.clamp(mod.zoomLevel(), 1.0, 6.0);
            srcW = Math.max(1, (int) Math.round(w / z));
            srcH = Math.max(1, (int) Math.round(h / z));
            srcX = x + (w - srcW) / 2;
            srcY = y + (h - srcH) / 2;
            srcX = Mth.clamp(srcX, 0, Math.max(0, sw - srcW));
            srcY = Mth.clamp(srcY, 0, Math.max(0, sh - srcH));
        }

        int maxBlocks = 12000; // guard against huge loops when block=1 on large region
        int estBlocks = ((w + block - 1) / block) * ((h + block - 1) / block);
        if (estBlocks > maxBlocks) {
            int scale = (int) Math.ceil(Math.sqrt(estBlocks / (double) maxBlocks));
            block = Mth.clamp(block * Math.max(1, scale), 2, 128);
        }

        for (int dy = 0; dy < h; dy += block) {
            int ch = Math.min(block, h - dy);
            int sampleY = srcY + Math.min(srcH - 1, (int) ((dy + ch * 0.5f) * srcH / (float) h));
            for (int dx = 0; dx < w; dx += block) {
                int cw = Math.min(block, w - dx);
                int sampleX = srcX + Math.min(srcW - 1, (int) ((dx + cw * 0.5f) * srcW / (float) w));
                float u0 = Mth.clamp(sampleX / (float) sw, 0f, 1f);
                float u1 = Mth.clamp((sampleX + 1) / (float) sw, 0f, 1f);
                float v0 = Mth.clamp(sampleY / (float) sh, 0f, 1f);
                float v1 = Mth.clamp((sampleY + 1) / (float) sh, 0f, 1f);
                if (u1 <= u0) u1 = Math.min(1f, u0 + 1f / sw);
                if (v1 <= v0) v1 = Math.min(1f, v0 + 1f / sh);
                int dx0 = x + dx;
                int dy0 = y + dy;
                ((GuiGraphicsAccessor) graphics).nebulacrak$submitBlit(
                    RenderPipelines.GUI_TEXTURED,
                    src.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                    dx0, dy0, dx0 + cw, dy0 + ch,
                    u0, u1, v0, v1,
                    0xFFFFFFFF
                );
            }
        }
    }

    @Unique
    private static void nebulacrak$drawPixelatedFallback(GuiGraphics graphics, RenderTarget src, int x, int y, int w, int h, int sw, int sh, int block) {
        for (int yy = y; yy < y + h; yy += block) {
            int ch = Math.min(block, y + h - yy);
            for (int xx = x; xx < x + w; xx += block) {
                int cw = Math.min(block, x + w - xx);
                int sx = xx + cw / 2;
                int sy = yy + ch / 2;
                ((GuiGraphicsAccessor) graphics).nebulacrak$submitBlit(
                    RenderPipelines.GUI_TEXTURED,
                    src.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                    xx, yy, xx + cw, yy + ch,
                    sx / (float) sw, (sx + 1) / (float) sw, sy / (float) sh, (sy + 1) / (float) sh,
                    0xFFFFFFFF
                );
            }
        }
    }

    @Unique
    private static void nebulacrak$drawBlur(GuiGraphics graphics, RenderTarget src, int x, int y, int w, int h, int sw, int sh, int intensity) {
        int iVal = Mth.clamp(intensity, 0, 100);
        if (iVal <= 0) return;
        int passes = Mth.clamp(1 + iVal / 20, 1, 6);
        int alpha = Mth.clamp(20 + iVal, 20, 110);
        int color = (alpha << 24) | 0xFFFFFF;

        int srcW = w;
        int srcH = h;
        int srcX = x;
        int srcY = y;
        StreamPrivacyModule mod = StreamPrivacyModule.INSTANCE;
        if (mod != null && mod.isZoomActive()) {
            double z = Mth.clamp(mod.zoomLevel(), 1.0, 6.0);
            srcW = Math.max(1, (int) Math.round(w / z));
            srcH = Math.max(1, (int) Math.round(h / z));
            srcX = x + (w - srcW) / 2;
            srcY = y + (h - srcH) / 2;
            srcX = Mth.clamp(srcX, 0, Math.max(0, sw - srcW));
            srcY = Mth.clamp(srcY, 0, Math.max(0, sh - srcH));
        }

        for (int i = 0; i < passes; i++) {
            int off = 1 + i;
            nebulacrak$blitRegionColor(graphics, src, srcX, srcY, srcW, srcH, x, y, w, h, sw, sh, -off, 0, color);
            nebulacrak$blitRegionColor(graphics, src, srcX, srcY, srcW, srcH, x, y, w, h, sw, sh, off, 0, color);
            nebulacrak$blitRegionColor(graphics, src, srcX, srcY, srcW, srcH, x, y, w, h, sw, sh, 0, -off, color);
            nebulacrak$blitRegionColor(graphics, src, srcX, srcY, srcW, srcH, x, y, w, h, sw, sh, 0, off, color);
        }
    }

    @Unique
    private static void nebulacrak$blitRegionColor(GuiGraphics graphics, RenderTarget src,
                                                   int srcX, int srcY, int srcW, int srcH,
                                                   int dstX, int dstY, int dstW, int dstH,
                                                   int sw, int sh, int ox, int oy, int argb) {
        int x0 = Math.max(dstX, dstX + ox);
        int y0 = Math.max(dstY, dstY + oy);
        int x1 = Math.min(dstX + dstW, dstX + dstW + ox);
        int y1 = Math.min(dstY + dstH, dstY + dstH + oy);
        if (x1 <= x0 || y1 <= y0) return;

        float u0 = (srcX + (x0 - (dstX + ox)) * srcW / (float) dstW) / sw;
        float u1 = (srcX + (x1 - (dstX + ox)) * srcW / (float) dstW) / sw;
        float v0 = (srcY + (y0 - (dstY + oy)) * srcH / (float) dstH) / sh;
        float v1 = (srcY + (y1 - (dstY + oy)) * srcH / (float) dstH) / sh;

        ((GuiGraphicsAccessor) graphics).nebulacrak$submitBlit(
            RenderPipelines.GUI_TEXTURED,
            src.getColorTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
            x0, y0, x1, y1,
            u0, u1, v0, v1,
            argb
        );
    }
}
