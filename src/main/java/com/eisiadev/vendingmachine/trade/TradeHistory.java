package com.eisiadev.vendingmachine.trade;

import net.minecraft.nbt.CompoundTag;

public class TradeHistory {

    public long lastTrade = -1;
    public int tradeCount = 0;
    public int cooldownTradeCount = 0;
    public boolean notificationQueued = false;

    public TradeHistory() {}

    public TradeHistory(long lastTrade, int tradeCount, int cooldownTradeCount, boolean notificationQueued) {
        this.lastTrade = lastTrade;
        this.tradeCount = tradeCount;
        this.cooldownTradeCount = cooldownTradeCount;
        this.notificationQueued = notificationQueued;
    }

    public void executeTrade(int maxTrades, int cooldown) {
        long now = System.currentTimeMillis();
        boolean hasCooldown = cooldown != -1;
        boolean cooldownOver = (now - lastTrade) / 1000 >= cooldown;
        lastTrade = now;
        tradeCount += 1;
        cooldownTradeCount = cooldownOver ? 1 : cooldownTradeCount + 1;
        notificationQueued = hasCooldown && (maxTrades == -1 || tradeCount < maxTrades);
    }

    public void setNotified() {
        notificationQueued = false;
    }

    public boolean isDefault() {
        return lastTrade == -1 && tradeCount == 0 && cooldownTradeCount == 0 && !notificationQueued;
    }

    public TradeHistory copy() {
        return new TradeHistory(lastTrade, tradeCount, cooldownTradeCount, notificationQueued);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastTrade", lastTrade);
        tag.putInt("tradeCount", tradeCount);
        tag.putInt("cooldownTradeCount", cooldownTradeCount);
        tag.putBoolean("notificationQueued", notificationQueued);
        return tag;
    }

    public static TradeHistory load(CompoundTag tag) {
        return new TradeHistory(tag.getLong("lastTrade"), tag.getInt("tradeCount"), tag.getInt("cooldownTradeCount"),
            tag.getBoolean("notificationQueued"));
    }
}
