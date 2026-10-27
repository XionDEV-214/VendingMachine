package com.eisiadev.vendingmachine.gui.widget;

import brachy.modularui.api.MCHelper;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.widgets.slot.ItemSlot;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemTooltips {

    private ItemTooltips() {}

    public static ItemSlot fix(ItemSlot slot) {
        RichTooltip tooltip = slot.itemTooltip();
        tooltip.tooltipBuilder(null);
        tooltip.tooltipBuilder(t -> {
            if (!slot.isSynced()) return;
            ItemStack stack = slot.getSlot().getItem();
            if (stack.isEmpty()) return;
            List<Component> lines = MCHelper.getItemToolTip(stack);
            for (int i = 0; i < lines.size(); i++) {
                t.addLine(lines.get(i));
                if (i == 0 && lines.size() > 1) t.spaceLine();
            }
        });
        return slot;
    }
}
