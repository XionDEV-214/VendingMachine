package com.eisiadev.vendingmachine.client.vending.widget;

import com.eisiadev.vendingmachine.client.vending.Gfx;
import com.eisiadev.vendingmachine.client.vending.VendingView;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.viewport.ModularGuiContext;

import net.minecraft.client.gui.GuiGraphics;

import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * A small raised metal key showing an icon, with a coloured accent line along the bottom:
 * eject keys, music / display / sort switches. Optionally reacts to the mouse wheel.
 */
public class IconKey extends VendingWidget<IconKey> implements Interactable {

    private final Supplier<UITexture> icon;
    private final Runnable action;
    private int accent = Gfx.AMBER;
    private int iconSize = 0;
    private DoubleConsumer onScroll;
    private boolean pressed;

    public IconKey(VendingView view, Supplier<UITexture> icon, Runnable action) {
        super(view);
        this.icon = icon;
        this.action = action;
    }

    public IconKey accent(int color) {
        this.accent = color;
        return this;
    }

    /** Icon edge length; 0 = fill the key minus its frame. */
    public IconKey iconSize(int size) {
        this.iconSize = size;
        return this;
    }

    public IconKey onScroll(DoubleConsumer onScroll) {
        this.onScroll = onScroll;
        return this;
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        if (button != 0) return Result.IGNORE;
        pressed = true;
        VendingView.click(1.0F);
        action.run();
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseReleased(int button) {
        pressed = false;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        if (onScroll == null) return false;
        onScroll.accept(delta);
        return true;
    }

    @Override
    protected void render(ModularGuiContext context, GuiGraphics g, int w, int h, long now) {
        boolean hovered = isHovering();
        if (!hovered) pressed = false;
        Gfx.rect(g, 0, 0, w, h, Gfx.FRAME);
        if (pressed) {
            Gfx.rect(g, 1, 1, w - 2, h - 2, 0xFF1D2227);
        } else {
            Gfx.vgrad(g, 1, 1, w - 2, h - 2, hovered ? 0xFF56616D : 0xFF444C56, hovered ? 0xFF3A424B : 0xFF2C3239);
            Gfx.rect(g, 1, 1, w - 2, 1, 0xFF7A8591);
        }
        Gfx.rect(g, 2, h - 2, w - 4, 1, accent);
        if (hovered) Gfx.glow(g, 0, 0, w, h, Gfx.CYAN, 90);

        int size = iconSize > 0 ? iconSize : Math.min(w, h) - 4;
        int dy = pressed ? 1 : 0;
        icon.get().draw(context, (w - size) / 2, (h - 1 - size) / 2 + dy, size, size);
    }
}
