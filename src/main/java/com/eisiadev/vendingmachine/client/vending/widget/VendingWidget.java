package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.VendingView;

import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.Widget;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/** Base for the vending machine's custom-drawn widgets: draws in local coordinates with the current time. */
public abstract class VendingWidget<W extends VendingWidget<W>> extends Widget<W> {

    protected final VendingView view;

    protected VendingWidget(VendingView view) {
        this.view = view;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        render(context, context.getGraphics(), getArea().width, getArea().height, Util.getMillis());
    }

    protected abstract void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now);
}
