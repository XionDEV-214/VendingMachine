package com.eisiadev.vendingmachine.gui.widget;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.client.TradeDisplay;
import com.eisiadev.vendingmachine.gui.DisplayType;
import com.eisiadev.vendingmachine.gui.VMTextures;
import com.eisiadev.vendingmachine.gui.VendingGui;
import com.eisiadev.vendingmachine.trade.TradeCategory;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.Widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TradeButton extends Widget<TradeButton> implements Interactable {

    public static final int TILE_WIDTH = 47;
    public static final int TILE_HEIGHT = 25;
    public static final int LIST_WIDTH = 153;
    public static final int LIST_HEIGHT = 14;

    private static final int TEXT_COLOR = 0xFF404040;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFFFFFFF;
    private static final int LIST_TRADEABLE = 0xFF55FF55;
    private static final int LIST_UNTRADEABLE = 0xFFFF5555;
    private static final int LIST_DISABLED = 0x80000000;
    private static final int LIST_SELECTED = 0xFFFFFF55;

    private final VendingGui gui;
    private final TradeCategory category;
    private final int index;
    private final DisplayType displayType;
    private boolean pressed = false;

    public TradeButton(VendingGui gui, TradeCategory category, int index, DisplayType displayType) {
        this.gui = gui;
        this.category = category;
        this.index = index;
        this.displayType = displayType;
        if (displayType == DisplayType.TILE) size(TILE_WIDTH, TILE_HEIGHT);
        else size(LIST_WIDTH, LIST_HEIGHT);
        setEnabledIf(w -> VMConfig.DISPLAY_TYPE.get() == displayType && getDisplay() != null);
        tooltipDynamic(tooltip -> {
            tooltip.clearText();
            TradeDisplay d = getDisplay();
            if (d != null) gui.buildTradeTooltip(tooltip, d);
        });
        tooltipAutoUpdate(true);
    }

    @Nullable
    public TradeDisplay getDisplay() {
        return gui.getDisplayed(category, index);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        TradeDisplay d = getDisplay();
        if (d == null || button != 0) return Result.IGNORE;
        boolean shift = Interactable.hasShiftDown();
        boolean ctrl = Interactable.hasControlDown();
        if (shift == ctrl) return Result.IGNORE;
        if (shift) {
            gui.attemptPurchase(d);
            pressed = true;
        } else {
            gui.toggleFavourite(d);
        }
        Interactable.playButtonClickSound();
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseReleased(int button) {
        pressed = false;
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        TradeDisplay d = getDisplay();
        if (d == null) return;
        if (!isHovering()) pressed = false;
        GuiGraphics g = context.getGraphics();
        ItemStack item = d.display;
        boolean blocked = !gui.isMachineFormed() || d.hasCooldown || !d.enabled;
        String count = String.valueOf(item.getCount());

        if (displayType == DisplayType.TILE) {
            (pressed ? VMTextures.TILE_BUTTON_PRESSED : VMTextures.TILE_BUTTON)
                .draw(context, 0, 0, TILE_WIDTH, TILE_HEIGHT);
            if (d.tradeableNow) VMTextures.OVERLAY_TRADEABLE.draw(context, 0, 0, TILE_WIDTH, TILE_HEIGHT);
            if (d.tgID.equals(gui.getHoveredGroup())) {
                VMTextures.OVERLAY_SELECTED.draw(context, 0, 0, TILE_WIDTH, TILE_HEIGHT);
            }
            GuiDraw.drawText(g, count, 4, 9, 1.0f, TEXT_COLOR, false);
            GuiDraw.drawItem(g, single(item), 26, 4, 16, 16, context.getCurrentDrawingZ());
            if (blocked) VMTextures.OVERLAY_COOLDOWN.draw(context, 0, 0, TILE_WIDTH, TILE_HEIGHT);
            if (d.hasCooldown) {
                String text = d.cooldownText;
                int w = context.getFont().width(text);
                GuiDraw.drawText(g, text, (TILE_WIDTH - w) / 2f, 9, 1.0f, COOLDOWN_TEXT_COLOR, true);
            }
            if (d.isFavourite) VMTextures.FAVOURITE.draw(context, 3, 3, 6, 6);
        } else {
            (pressed ? VMTextures.LIST_BUTTON_PRESSED : VMTextures.LIST_BUTTON)
                .draw(context, 0, 0, LIST_WIDTH, LIST_HEIGHT);
            GuiDraw.drawText(g, count, 6, 4, 0.9f, TEXT_COLOR, false);
            GuiDraw.drawItem(g, single(item), 24, 2, 9, 9, context.getCurrentDrawingZ());
            String name = item.getHoverName().getString();
            if (name.length() > 21) name = name.substring(0, 21) + "...";
            GuiDraw.drawText(g, name, 36, 4, 0.9f, TEXT_COLOR, false);
            GuiDraw.drawRect(g, 1, 1, 2, LIST_HEIGHT - 2, d.tradeableNow ? LIST_TRADEABLE : LIST_UNTRADEABLE);
            if (blocked) GuiDraw.drawRect(g, 1, 1, LIST_WIDTH - 2, LIST_HEIGHT - 2, LIST_DISABLED);
            if (d.tgID.equals(gui.getHoveredGroup())) GuiDraw.drawRect(g, 3, 1, 1, LIST_HEIGHT - 2, LIST_SELECTED);
            if (d.hasCooldown && d.enabled) {
                String text = d.cooldownText;
                int w = (int) (context.getFont().width(text) * 0.9f);
                GuiDraw.drawText(g, text, LIST_WIDTH - 16 - w, 4, 0.9f, COOLDOWN_TEXT_COLOR, true);
            }
            if (d.isFavourite) VMTextures.FAVOURITE.draw(context, 139, 2, 10, 10);
        }
    }

    private static ItemStack single(ItemStack stack) {
        if (stack.getCount() == 1) return stack;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
