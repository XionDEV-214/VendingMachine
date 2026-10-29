package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.trade.Trade;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

public class TradeDisplay {

    public final TradeGroup group;
    public final Trade trade;
    public final UUID tgID;
    public final int tradeGroupOrder;
    public final ItemStack display;

    public long cooldown;
    public String cooldownText;
    public boolean hasCooldown;
    public boolean enabled;
    public boolean tradeableNow;
    public int cdTradeCount;

    public int maxQuantity;
    public boolean isFavourite;

    public TradeDisplay(TradeGroup group, int order) {
        this.group = group;
        this.trade = group.getTrades().get(order);
        this.tgID = group.getId();
        this.tradeGroupOrder = order;
        this.display = trade.getDisplayStack();
    }

    public boolean matchesSearch(String lowerSearch) {
        if (lowerSearch.isEmpty()) return true;
        if (matches(display, lowerSearch)) return true;
        for (var ing : trade.toItems) if (matches(ing.getDisplayStack(), lowerSearch)) return true;
        for (var ing : trade.fromItems) if (matches(ing.getDisplayStack(), lowerSearch)) return true;
        return false;
    }

    private static boolean matches(ItemStack stack, String lowerSearch) {
        if (stack.isEmpty()) return false;
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(lowerSearch)) return true;
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (lowerSearch.startsWith("@") && key != null) {
            return key.getNamespace().contains(lowerSearch.substring(1));
        }
        return false;
    }

    public static String formatCooldown(long seconds) {
        if (seconds < 60) return Component.translatable("vendingmachine.gui.cooldown_display.second", seconds).getString();
        if (seconds < 3600) return Component.translatable("vendingmachine.gui.cooldown_display.minute", seconds / 60).getString();
        if (seconds < 86400) return Component.translatable("vendingmachine.gui.cooldown_display.hour", seconds / 3600).getString();
        return Component.translatable("vendingmachine.gui.cooldown_display.day", seconds / 86400).getString();
    }
}
