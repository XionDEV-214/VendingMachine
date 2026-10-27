package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.network.TradeStatusPacket;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientTradeState {

    private static final List<TradeDisplay> trades = new ArrayList<>();
    private static int tradesPerCooldown = 1;
    private static boolean dirty = true;

    private ClientTradeState() {}

    public static List<TradeDisplay> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public static int getTradesPerCooldown() {
        return tradesPerCooldown;
    }

    public static boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public static void markDirty() {
        dirty = true;
    }

    static void update(TradeStatusPacket packet) {
        trades.clear();
        tradesPerCooldown = packet.tradesPerCooldown;
        TradeDatabase db = TradeDatabase.client();
        for (TradeStatusPacket.Entry e : packet.entries) {
            TradeGroup tg = db.getTradeGroup(e.tradeGroup());
            if (tg == null || e.order() < 0 || e.order() >= tg.getTrades().size()) continue;
            TradeDisplay d = new TradeDisplay(tg, e.order());
            d.cooldown = e.cooldownRemaining();
            d.cooldownText = TradeDisplay.formatCooldown(d.cooldown <= 0 ? Math.max(0, tg.cooldown) : d.cooldown);
            d.cdTradeCount = d.cooldown > 0 ? e.cdTradeCount() : 0;
            d.hasCooldown = d.cooldown > 0 && d.cdTradeCount >= tradesPerCooldown;
            d.enabled = e.enabled();
            d.tradeableNow = e.tradeableNow();
            d.isFavourite = FavouritesTracker.isFavourite(d.tgID, d.tradeGroupOrder);
            trades.add(d);
        }
        dirty = true;
    }

    public static void clear() {
        trades.clear();
        dirty = true;
    }
}
