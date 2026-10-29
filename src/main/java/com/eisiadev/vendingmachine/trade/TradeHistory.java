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
        executeTrade(maxTrades, cooldown, 1);
    }

    public void executeTrade(int maxTrades, int cooldown, int times) {
        if (times <= 0) return;
        long now = System.currentTimeMillis();
        boolean hasCooldown = cooldown != -1;
        boolean cooldownOver = (now - lastTrade) / 1000 >= cooldown;
        lastTrade = now;
        tradeCount = saturatedIntAdd(tradeCount, times);
        cooldownTradeCount = cooldownOver ? times : saturatedIntAdd(cooldownTradeCount, times);
        notificationQueued = hasCooldown && (maxTrades == -1 || tradeCount < maxTrades);
    }

    private static int saturatedIntAdd(int a, int b) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) a + b));
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
