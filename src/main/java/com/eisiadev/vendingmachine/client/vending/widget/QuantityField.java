package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;
import com.eisiadev.vendingmachine.machine.VendingMachineController;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.value.StringValue;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/**
 * Purchase quantity input on the LED display. Digits only, at most 6 (1..999,999); empty means 1.
 * The field itself enforces the pattern and length, so typed text is never rewritten under the cursor.
 */
public class QuantityField extends TextFieldWidget {

    private static final Pattern DIGITS = Pattern.compile("[0-9]*");
    private static final int MAX_DIGITS = String.valueOf(VendingMachineController.MAX_PURCHASE_QUANTITY).length();

    private static final IDrawable BACKGROUND = (context, x, y, w, h, theme) -> {
        GuiGraphics g = context.getGraphics();
        Gfx.rect(g, x, y, w, h, Gfx.FRAME);
        Gfx.rect(g, x + 1, y + 1, w - 2, h - 2, Gfx.LED_BG);
        Gfx.rect(g, x + 1, y + h - 2, w - 2, 1, 0xFF143014);
    };

    public QuantityField(VendingView view) {
        setPattern(DIGITS);
        setMaxLength(MAX_DIGITS);
        autoUpdateOnChange(true);
        setTextColor(Gfx.LED_ON);
        hintText(Component.literal("1"));
        hintColor(Gfx.LED_GHOST);
        disableThemeBackground(true);
        background(BACKGROUND);
        value(new StringValue.Dynamic(view::quantityText, view::setQuantityText));
        tooltipDynamic(t -> t.clearText()
            .addLine(Component.translatable("vendingmachine.gui.quantity.field",
                VendingView.full(VendingMachineController.MAX_PURCHASE_QUANTITY)))
            .addLine(Component.translatable("vendingmachine.gui.trade_hint").withStyle(ChatFormatting.GRAY)));
    }
}
