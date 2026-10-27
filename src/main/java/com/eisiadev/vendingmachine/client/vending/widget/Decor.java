package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.VendingView;

import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.client.gui.GuiGraphics;

/** A non-interactive widget that just runs a drawing function: labels, banners, seams. */
public class Decor extends VendingWidget<Decor> {

    @FunctionalInterface
    public interface Painter {

        void paint(GuiGraphics g, int w, int h, long now);
    }

    private final Painter painter;

    public Decor(VendingView view, Painter painter) {
        super(view);
        this.painter = painter;
    }

    @Override
    public boolean canHover() {
        return false;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        painter.paint(g, w, h, now);
    }
}
