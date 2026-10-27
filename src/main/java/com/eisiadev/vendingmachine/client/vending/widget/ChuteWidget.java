package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

/**
 * Output chute. The output slots (real slots, drawn on top of this widget) land on its floor, spread sideways by
 * the golden ratio; new items fall in from under the lip with a bounce. Shift-click the chute to collect all.
 */
public class ChuteWidget extends VendingWidget<ChuteWidget> implements Interactable {

    public static final int W = 42;
    public static final int H = 64;
    public static final int LIP_H = 12;
    private static final int FALL_MS = 900;

    public ChuteWidget(VendingView view) {
        super(view);
        size(W, H);
        tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.collect_outputs").withStyle(ChatFormatting.WHITE))
            .addLine(Component.translatable("vendingmachine.gui.collect_outputs_hint").withStyle(ChatFormatting.GRAY)));
    }

    public static int slotX(int i) {
        final double phi = (Math.sqrt(5) - 1) / 2.0;
        return 1 + (int) (((i * phi) % 1.0) * (W - 2 - 18));
    }

    public static int slotY(int i) {
        return H - 2 - 18;
    }

    /** Vertical offset of an item that has just landed in slot {@code i} (negative while falling). */
    public float dropOffset(int i, long now) {
        long t0 = view.outputDropTime(i);
        if (t0 == 0) return 0;
        float t = (now - t0) / (float) FALL_MS;
        if (t >= 1) return 0;
        float distance = slotY(i) + 18 - LIP_H + 4;
        return -distance * (1 - Gfx.bounceOut(t));
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || !Interactable.hasShiftDown()) return Result.IGNORE;
        view.collectOutputs();
        VendingView.click(0.9F);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        // Recess and back wall
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        Gfx.vgrad(g, 1, 1, w - 2, h - 2, 0xFF07090B, 0xFF1B2026);
        for (int x = 5; x < w - 2; x += 6) Gfx.rect(g, x, LIP_H, 1, h - LIP_H - 4, 0x14FFFFFF);
        Gfx.rect(g, 1, h - 3, w - 2, 2, 0xFF2C333B);
        if (isHovering()) Gfx.outline(g, 0, 0, w, h, Gfx.AMBER);
    }

    /** The chute's front lip, drawn after the output slots so falling items pass behind it. */
    public static void drawLip(GuiGraphics g, int w, long now) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        Gfx.rect(g, 0, 0, w, LIP_H, Gfx.FRAME);
        Gfx.vgrad(g, 1, 1, w - 2, LIP_H - 3, 0xFF8C96A2, 0xFF4B545E);
        Gfx.rect(g, 1, 1, w - 2, 1, 0xFFB7C0CA);
        Gfx.hazard(g, 4, LIP_H - 6, w - 8, 3, Gfx.AMBER, Gfx.HAZARD_DARK, 0);
        Gfx.rect(g, 1, LIP_H - 2, w - 2, 1, 0xFF2A3036);
        Gfx.rect(g, 1, LIP_H - 1, w - 2, 1, 0x66000000);
        g.pose().popPose();
    }
}
