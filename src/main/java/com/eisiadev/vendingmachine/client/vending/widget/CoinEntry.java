package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

public class CoinEntry extends VendingWidget<CoinEntry> implements Interactable {

    public static final int W = 40;
    public static final int H = 12;

    private final CurrencyDefinition def;

    public CoinEntry(VendingView view, CurrencyDefinition def) {
        super(view);
        this.def = def;
        size(W, H);
        tooltipDynamic(t -> t.clearText()
            .addLine(Component.literal(VendingView.full(view.balance(def)) + " ").append(def.getDisplayName()))
            .emptyLine()
            .addLine(Component.translatable("vendingmachine.gui.single_coin_type_eject_hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        tooltipAutoUpdate(true);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0 || !Interactable.hasShiftDown()) return Result.IGNORE;
        view.ejectCoins(def);
        VendingView.click(1.4F);
        return Result.SUCCESS;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        long amount = view.balance(def);
        if (isHovering()) Gfx.rect(g, 0, 0, w, h, 0x30FFFFFF);
        g.flush();
        GuiDraw.drawItem(g, def.getIcon(amount), 0, 0, 12, 12, context.getCurrentDrawingZ());
        Gfx.text(g, VendingView.compact(amount), 14, 3, 0.75f, amount > 0 ? Gfx.LED_ON : Gfx.DIM, false);
    }
}
