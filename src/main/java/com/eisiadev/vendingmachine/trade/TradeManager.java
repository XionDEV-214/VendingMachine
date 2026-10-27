package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.network.TradeNotificationPacket;
import com.eisiadev.vendingmachine.network.VMNetwork;
import com.eisiadev.vendingmachine.storage.PlayerVendingData;
import com.eisiadev.vendingmachine.storage.VMSavedData;
import com.eisiadev.vendingmachine.storage.Wallet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class TradeManager {

    private TradeManager() {}

    @Nullable
    private static VMSavedData data() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : VMSavedData.get(server);
    }

    @Nullable
    public static PlayerVendingData getPlayerData(UUID player) {
        VMSavedData d = data();
        return d == null ? null : d.getPlayer(player);
    }

    @Nullable
    public static Wallet getWallet(UUID player) {
        PlayerVendingData d = getPlayerData(player);
        return d == null ? null : d.wallet;
    }

    public static void markDirty() {
        VMSavedData d = data();
        if (d != null) d.setDirty();
    }

    public static TradeHistory getHistory(UUID player, TradeGroup tg) {
        PlayerVendingData d = getPlayerData(player);
        return d == null ? new TradeHistory() : d.getHistory(tg.getId());
    }

    public static int getTradesPerCooldown() {
        return VMConfig.TRADES_PER_COOLDOWN.get();
    }

    public static long getCooldownRemaining(UUID player, TradeGroup tg) {
        if (!tg.hasCooldown()) return -1;
        TradeHistory h = getHistory(player, tg);
        if (h.lastTrade == -1) return -1;
        long elapsed = (System.currentTimeMillis() - h.lastTrade) / 1000;
        return elapsed < tg.cooldown ? tg.cooldown - elapsed : -1;
    }

    public static boolean isEnabled(UUID player, TradeGroup tg) {
        return tg.maxTrades == -1 || getHistory(player, tg).tradeCount < tg.maxTrades;
    }

    public static boolean isBlockedByCooldown(UUID player, TradeGroup tg) {
        return getCooldownRemaining(player, tg) > 0 &&
            getHistory(player, tg).cooldownTradeCount >= getTradesPerCooldown();
    }

    public static boolean canExecuteTrade(UUID player, @Nullable TradeGroup tg) {
        return tg != null && isEnabled(player, tg) && !isBlockedByCooldown(player, tg);
    }

    public static void executeTrade(UUID player, TradeGroup tg) {
        PlayerVendingData d = getPlayerData(player);
        if (d == null) return;
        TradeHistory h = d.getHistory(tg.getId());
        h.executeTrade(tg.maxTrades, tg.cooldown);
        d.setHistory(tg.getId(), h);
        if (h.notificationQueued) d.notificationQueue.add(tg.getId());
        markDirty();
    }

    public static void sendTradeNotifications(ServerPlayer player) {
        PlayerVendingData d = getPlayerData(player.getUUID());
        if (d == null || d.notificationQueue.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<UUID> ready = new ArrayList<>();
        Iterator<UUID> it = d.notificationQueue.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            TradeGroup tg = TradeDatabase.server().getTradeGroup(id);
            if (tg == null) {
                it.remove();
                continue;
            }
            TradeHistory h = d.getHistory(id);
            if ((now - h.lastTrade) / 1000 > tg.cooldown) {
                ready.add(id);
                h.setNotified();
                it.remove();
            }
        }
        if (!ready.isEmpty()) {
            VMNetwork.sendTo(player, new TradeNotificationPacket(ready));
            markDirty();
        }
    }
}
