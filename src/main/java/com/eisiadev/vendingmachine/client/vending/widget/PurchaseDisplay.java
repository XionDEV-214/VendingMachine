package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.TradeDisplay;
import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;

import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Green LED panel under the shelf: status lamp on the left, the hovered trade's name, the quantity input
 * ({@link QuantityField}, a separate widget placed at {@link #FIELD_X}) and how many of it can be bought now.
 */
public class PurchaseDisplay extends VendingWidget<PurchaseDisplay> {

    public static final int FIELD_X = 74;
    public static final int FIELD_Y = 11;
    public static final int FIELD_W = 40;
    public static final int FIELD_H = 10;
    private static final int TEXT_LEFT = 48;

    public PurchaseDisplay(VendingView view) {
        super(view);
    }

    @Override
    public boolean canHover() {
        return false;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        Gfx.well(g, 0, 0, w, h);
        Gfx.rect(g, 2, 2, w - 4, h - 4, Gfx.LED_BG);

        drawStatus(g, h, now);
        Gfx.rect(g, TEXT_LEFT - 4, 4, 1, h - 8, 0xFF143014);

        TradeDisplay d = view.hovered();
        String name = d == null ? Component.translatable("vendingmachine.gui.led.none").getString()
            : d.display.getHoverName().getString();
        Gfx.text(g, Gfx.fit(name, FIELD_X + FIELD_W - TEXT_LEFT - 4, 0.5f), TEXT_LEFT, 4, 0.5f, Gfx.LED_LABEL, false);

        String quantity = Component.translatable("vendingmachine.gui.led.quantity").getString();
        Gfx.text(g, Gfx.fit(quantity, FIELD_X - TEXT_LEFT - 2, 0.5f), TEXT_LEFT, FIELD_Y + 3, 0.5f, Gfx.LED_LABEL, false);
        Gfx.text(g, Component.translatable("vendingmachine.gui.led.max").getString(), FIELD_X + FIELD_W + 4, 4,
            0.5f, Gfx.LED_LABEL, false);

        // Max purchasable for the hovered trade, with unlit "8"s behind it like a real 7-segment display
        float right = w - 6;
        float y = FIELD_Y + 1;
        String ghost = "888,888";
        Gfx.textRight(g, ghost, right, y, 1f, Gfx.LED_GHOST, false);
        if (d != null) {
            String max = String.format(Locale.ROOT, "%,d", d.maxQuantity);
            int color = view.canBuy(d) ? Gfx.LED_ON : Gfx.RED;
            Gfx.textRight(g, max, right + 0.5f, y + 0.5f, 1f, 0x40205A10, false);
            Gfx.textRight(g, max, right, y, 1f, color, false);
        }

        Gfx.scanlines(g, 2, 2, w - 4, h - 4, 0x22000000);
        Gfx.rect(g, 2, 2, w - 4, 1, 0x18FFFFFF);
    }

    private void drawStatus(GuiGraphics g, int h, long now) {
        VendingView.Status status = view.status();
        int color = switch (status) {
            case READY -> Gfx.LED_ON;
            case SHORT -> Gfx.AMBER;
            case BLOCKED, OFFLINE -> Gfx.RED;
        };
        boolean blink = status == VendingView.Status.OFFLINE && (now / 300) % 2 == 0;
        int lamp = blink ? Gfx.shade(color, 0.35f) : color;
        Gfx.rect(g, 5, 5, 5, 5, Gfx.FRAME);
        Gfx.rect(g, 6, 6, 3, 3, lamp);
        if (!blink) Gfx.glow(g, 6, 6, 3, 3, color, 90);

        String key = "vendingmachine.gui.status." + status.name().toLowerCase(Locale.ROOT);
        Gfx.text(g, Gfx.fit(Component.translatable(key).getString(), 28, 0.5f), 13, 6, 0.5f, color, false);

        for (int i = 0; i < 3; i++) {
            boolean on = status == VendingView.Status.READY || (now / 150 + i) % 3 != 0;
            Gfx.rect(g, 6 + i * 5, h - 8, 3, 3, on ? Gfx.shade(color, 0.8f) : Gfx.LED_GHOST);
        }
    }
}
