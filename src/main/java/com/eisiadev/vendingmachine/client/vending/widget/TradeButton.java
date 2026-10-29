package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.client.TradeDisplay;
import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;
import com.eisiadev.vendingmachine.gui.DisplayType;
import com.eisiadev.vendingmachine.trade.TradeCategory;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TradeButton extends VendingWidget<TradeButton> implements Interactable {

    public static final int TILE_WIDTH = 47;
    public static final int TILE_HEIGHT = 25;
    public static final int LIST_WIDTH = 153;
    public static final int LIST_HEIGHT = 14;

    private final TradeCategory category;
    private final int index;
    private final DisplayType displayType;
    private boolean pressed = false;

    public TradeButton(VendingView view, TradeCategory category, int index, DisplayType displayType) {
        super(view);
        this.category = category;
        this.index = index;
        this.displayType = displayType;
        if (displayType == DisplayType.TILE) size(TILE_WIDTH, TILE_HEIGHT);
        else size(LIST_WIDTH, LIST_HEIGHT);
        setEnabledIf(w -> VMConfig.DISPLAY_TYPE.get() == displayType && display() != null);
        tooltipDynamic(tooltip -> {
            tooltip.clearText();
            TradeDisplay d = display();
            if (d != null) view.buildTradeTooltip(tooltip, d);
        });
        tooltipAutoUpdate(true);
    }

    @Nullable
    public TradeDisplay display() {
        return view.displayed(category, index);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        TradeDisplay d = display();
        if (d == null || button != 0) return Result.IGNORE;
        boolean shift = Interactable.hasShiftDown();
        boolean ctrl = Interactable.hasControlDown();
        if (shift == ctrl) return Result.IGNORE;
        if (shift) {
            pressed = true;
            view.attemptPurchase(d);
        } else {
            view.toggleFavourite(d);
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseReleased(int button) {
        pressed = false;
        return true;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        TradeDisplay d = display();
        if (d == null) return;
        if (!isHovering()) pressed = false;
        if (displayType == DisplayType.TILE) renderTile(context, g, w, h, d);
        else renderRow(context, g, w, h, d);
    }

    private int lamp(TradeDisplay d) {
        if (view.canBuy(d)) return Gfx.LED_ON;
        if (d.tradeableNow && d.maxQuantity > 0) return Gfx.AMBER;
        return Gfx.shade(Gfx.RED, 0.7f);
    }

    private void renderTile(ModularGuiContext context, GuiGraphics g, int w, int h, TradeDisplay d) {
        boolean hovered = isHovering();
        boolean blocked = view.blocked(d);

        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        if (pressed) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF1D2227);
        } else {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF4E5964 : 0xFF3A424B, hovered ? 0xFF353D46 : 0xFF262C33);
            Gfx.rect(g, 1, 1, w - 2, 1, 0xFF66717D);
        }
        int dy = pressed ? 1 : 0;

        String count = String.valueOf(d.display.getCount());
        float scale = Math.min(1f, 19f / Math.max(1, Gfx.width(count)));
        Gfx.text(g, count, 4, (h - 8 * scale) / 2f - 1 + dy, scale, Gfx.TEXT, true);

        Gfx.well(g, 26, 4 + dy, 18, 18);
        g.flush();
        GuiDraw.drawItem(g, single(d.display), 26, 4 + dy, 16, 16, context.getCurrentDrawingZ());

        int lamp = lamp(d);
        Gfx.rect(g, 3, h - 3, 19, 1, lamp);
        if (!blocked && view.canBuy(d)) Gfx.glow(g, 3, h - 3, 19, 1, lamp, 40);

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (d.tgID.equals(view.hoveredGroup())) Gfx.outline(g, 0, 0, w, h, Gfx.CYAN);
        if (blocked) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xB0101418);
            Gfx.hazard(g, 1, h - 4, w - 2, 3, 0xFF3A2A00, 0xFF15130C, 0);
        }
        if (d.hasCooldown) Gfx.textCentered(g, d.cooldownText, w / 2f, (h - 8) / 2f, 1f, Gfx.AMBER, true);
        if (d.isFavourite) favourite(g, 3, 3);
        g.pose().popPose();
    }

    private void renderRow(ModularGuiContext context, GuiGraphics g, int w, int h, TradeDisplay d) {
        boolean hovered = isHovering();
        boolean blocked = view.blocked(d);

        int bg = pressed ? 0xFF11151A : 0xFF1A1F25;
        Gfx.rect(g, 0, 0, w, h - 1, hovered ? Gfx.shade(bg, 1.45f) : bg);
        Gfx.rect(g, 0, h - 1, w, 1, 0xFF0A0C0F);
        Gfx.rect(g, 0, 0, 2, h - 1, lamp(d));
        if (d.tgID.equals(view.hoveredGroup())) Gfx.rect(g, 3, 1, 1, h - 3, Gfx.CYAN);

        String count = String.valueOf(d.display.getCount());
        float scale = Math.min(0.9f, 15f / Math.max(1, Gfx.width(count)));
        Gfx.text(g, count, 6, (h - 8 * scale) / 2f, scale, Gfx.MUTED, false);

        g.flush();
        GuiDraw.drawItem(g, single(d.display), 22, 2, 10, 10, context.getCurrentDrawingZ());

        int right = w - 4;
        int favX = -1;
        if (d.isFavourite) {
            favX = right - 3;
            right -= 7;
        }
        int cooldownRight = right;
        boolean showCooldown = d.hasCooldown && d.enabled;
        if (showCooldown) right -= (int) (Gfx.width(d.cooldownText) * 0.9f) + 4;
        String name = Gfx.fit(d.display.getHoverName().getString(), right - 36, 0.9f);
        Gfx.text(g, name, 36, (h - 7) / 2f, 0.9f, blocked ? Gfx.DIM : Gfx.TEXT, false);

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (blocked) Gfx.rect(g, 2, 0, w - 2, h - 1, 0x90000000);
        if (showCooldown) Gfx.textRight(g, d.cooldownText, cooldownRight, (h - 7) / 2f, 0.9f, Gfx.AMBER, true);
        if (favX >= 0) favourite(g, favX, (h - 3) / 2f);
        g.pose().popPose();
    }

    private static void favourite(GuiGraphics g, float x, float y) {
        Gfx.rect(g, x, y, 3, 3, Gfx.AMBER);
        Gfx.glow(g, x, y, 3, 3, Gfx.AMBER, 50);
    }

    private static ItemStack single(ItemStack stack) {
        if (stack.getCount() == 1) return stack;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
