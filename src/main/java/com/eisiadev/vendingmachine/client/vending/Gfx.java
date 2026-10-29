package com.eisiadev.vendingmachine.client.vending;

import brachy.modularui.drawable.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class Gfx {

    public static final int FRAME = 0xFF07090C;
    public static final int PLATE_TOP = 0xFF3B424B;
    public static final int PLATE_BOTTOM = 0xFF282D34;
    public static final int BEVEL_HI = 0xFF5E6975;
    public static final int BEVEL_LO = 0xFF14181C;
    public static final int SEAM = 0xFF1C2026;

    public static final int WELL = 0xFF0E1115;
    public static final int WELL_SHADOW = 0xFF050608;
    public static final int WELL_HI = 0xFF48515B;

    public static final int TEXT = 0xFFD7DEE6;
    public static final int MUTED = 0xFF808B97;
    public static final int DIM = 0xFF4A525B;

    public static final int CYAN = 0xFF38E1FF;
    public static final int AMBER = 0xFFFFB21A;
    public static final int RED = 0xFFFF4A3D;
    public static final int HAZARD_DARK = 0xFF16181B;

    public static final int LED_BG = 0xFF040904;
    public static final int LED_ON = 0xFF52FF2E;
    public static final int LED_GHOST = 0xFF0C220A;
    public static final int LED_LABEL = 0xFF2F9C1F;

    private Gfx() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.drawRect(g, x, y, w, h, color);
    }

    public static void vgrad(GuiGraphics g, float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.drawVerticalGradientRect(g, x, y, w, h, top, bottom);
    }

    public static void hgrad(GuiGraphics g, float x, float y, float w, float h, int left, int right) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.drawHorizontalGradientRect(g, x, y, w, h, left, right);
    }

    public static void outline(GuiGraphics g, float x, float y, float w, float h, int color) {
        rect(g, x, y, w, 1, color);
        rect(g, x, y + h - 1, w, 1, color);
        rect(g, x, y + 1, 1, h - 2, color);
        rect(g, x + w - 1, y + 1, 1, h - 2, color);
    }

    public static void glow(GuiGraphics g, float x, float y, float w, float h, int rgb, int strength) {
        for (int i = 1; i <= 3; i++) {
            int alpha = Mth.clamp(strength / (i * i), 0, 255);
            outline(g, x - i, y - i, w + 2 * i, h + 2 * i, (alpha << 24) | (rgb & 0xFFFFFF));
        }
    }

    public static void plate(GuiGraphics g, float x, float y, float w, float h) {
        rect(g, x, y, w, h, FRAME);
        vgrad(g, x + 1, y + 1, w - 2, h - 2, PLATE_TOP, PLATE_BOTTOM);
        rect(g, x + 1, y + 1, w - 2, 1, BEVEL_HI);
        rect(g, x + 1, y + 2, 1, h - 3, BEVEL_HI);
        rect(g, x + 1, y + h - 2, w - 2, 1, BEVEL_LO);
        rect(g, x + w - 2, y + 2, 1, h - 4, BEVEL_LO);
    }

    public static void well(GuiGraphics g, float x, float y, float w, float h) {
        rect(g, x, y, w, h, WELL);
        rect(g, x, y, w, 1, WELL_SHADOW);
        rect(g, x, y + 1, 1, h - 1, WELL_SHADOW);
        rect(g, x + 1, y + 1, w - 1, 1, 0x40000000);
        rect(g, x, y + h - 1, w, 1, WELL_HI);
        rect(g, x + w - 1, y, 1, h, WELL_HI);
    }

    public static void rivet(GuiGraphics g, float x, float y) {
        rect(g, x, y, 2, 2, 0xFF1A1E23);
        rect(g, x, y, 1, 1, 0xFF7C8793);
        rect(g, x + 1, y + 1, 1, 1, 0xFF0B0D10);
    }

    public static void rivets(GuiGraphics g, float x, float y, float w, float h, int inset) {
        rivet(g, x + inset, y + inset);
        rivet(g, x + w - inset - 2, y + inset);
        rivet(g, x + inset, y + h - inset - 2);
        rivet(g, x + w - inset - 2, y + h - inset - 2);
    }

    public static void hazard(GuiGraphics g, float x, float y, int w, int h, int stripe, int dark, int phase) {
        rect(g, x, y, w, h, dark);
        int period = 6;
        for (int row = 0; row < h; row++) {
            int shift = Math.floorMod(row + phase, period);
            for (int start = -period + shift; start < w; start += period) {
                int x0 = Math.max(0, start);
                int x1 = Math.min(w, start + 3);
                if (x1 > x0) rect(g, x + x0, y + row, x1 - x0, 1, stripe);
            }
        }
    }

    public static void scanlines(GuiGraphics g, float x, float y, float w, float h, int color) {
        for (int row = 1; row < h; row += 2) rect(g, x, y + row, w, 1, color);
    }

    public static void arrow(GuiGraphics g, float cx, float cy, boolean left, int size, int color) {
        for (int i = 0; i < size; i++) {
            float len = 2 * i + 1;
            float col = left ? cx - size / 2f + i : cx + size / 2f - 1 - i;
            rect(g, col, cy - i, 1, len, color);
        }
    }

    public static void arrowVertical(GuiGraphics g, float cx, float cy, boolean up, int size, int color) {
        for (int i = 0; i < size; i++) {
            float len = 2 * i + 1;
            float row = up ? cy - size / 2f + i : cy + size / 2f - 1 - i;
            rect(g, cx - i, row, len, 1, color);
        }
    }

    public static void text(GuiGraphics g, String s, float x, float y, float scale, int color, boolean shadow) {
        GuiDraw.drawText(g, s, x, y, scale, color, shadow);
    }

    public static void textCentered(GuiGraphics g, String s, float cx, float y, float scale, int color, boolean shadow) {
        text(g, s, cx - width(s) * scale / 2f, y, scale, color, shadow);
    }

    public static void textRight(GuiGraphics g, String s, float right, float y, float scale, int color, boolean shadow) {
        text(g, s, right - width(s) * scale, y, scale, color, shadow);
    }

    public static int width(String s) {
        return font().width(s);
    }

    public static String fit(String s, float maxWidth, float scale) {
        int limit = (int) (maxWidth / scale);
        if (width(s) <= limit) return s;
        String cut = font().plainSubstrByWidth(s, Math.max(0, limit - width("…")));
        return cut + "…";
    }

    public static int withAlpha(int color, float alpha) {
        int a = Mth.clamp((int) (alpha * ((color >>> 24) & 0xFF)), 0, 255);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24);
        int r = (int) (ar + (br - ar) * t), gr = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (gr << 8) | bl;
    }

    public static int shade(int color, float factor) {
        int r = Mth.clamp((int) (((color >> 16) & 0xFF) * factor), 0, 255);
        int gr = Mth.clamp((int) (((color >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((color & 0xFF) * factor), 0, 255);
        return (color & 0xFF000000) | (r << 16) | (gr << 8) | b;
    }

    public static float pulse(long now, int periodMs) {
        float t = (now % periodMs) / (float) periodMs;
        return 0.5f - 0.5f * Mth.cos(t * Mth.TWO_PI);
    }

    public static float easeOutCubic(float t) {
        t = Mth.clamp(t, 0, 1);
        float u = 1 - t;
        return 1 - u * u * u;
    }

    public static float bounceOut(float t) {
        t = Mth.clamp(t, 0, 1);
        if (t < 1 / 2.75f) return 7.5625f * t * t;
        if (t < 2 / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        }
        if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        }
        t -= 2.625f / 2.75f;
        return 7.5625f * t * t + 0.984375f;
    }
}
