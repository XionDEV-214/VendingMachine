package com.eisiadev.vendingmachine.storage;

import com.eisiadev.vendingmachine.trade.TradeHistory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerVendingData {

    public final Wallet wallet = new Wallet();
    private final Map<UUID, TradeHistory> tradeHistory = new HashMap<>();
    public final Set<UUID> notificationQueue = new LinkedHashSet<>();

    public TradeHistory getHistory(UUID tradeGroup) {
        return tradeHistory.computeIfAbsent(tradeGroup, k -> new TradeHistory());
    }

    public void setHistory(UUID tradeGroup, TradeHistory history) {
        if (history == null || history.isDefault()) tradeHistory.remove(tradeGroup);
        else tradeHistory.put(tradeGroup, history);
    }

    public void resetHistory() {
        tradeHistory.clear();
        notificationQueue.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("wallet", wallet.save());
        CompoundTag hist = new CompoundTag();
        tradeHistory.forEach((id, h) -> {
            if (!h.isDefault()) hist.put(id.toString(), h.save());
        });
        tag.put("tradeHistory", hist);
        ListTag notif = new ListTag();
        notificationQueue.forEach(id -> notif.add(StringTag.valueOf(id.toString())));
        tag.put("notificationQueue", notif);
        return tag;
    }

    public static PlayerVendingData load(CompoundTag tag) {
        PlayerVendingData d = new PlayerVendingData();
        d.wallet.load(tag.getCompound("wallet"));
        CompoundTag hist = tag.getCompound("tradeHistory");
        for (String key : hist.getAllKeys()) {
            try {
                d.tradeHistory.put(UUID.fromString(key), TradeHistory.load(hist.getCompound(key)));
            } catch (IllegalArgumentException ignored) {}
        }
        ListTag notif = tag.getList("notificationQueue", Tag.TAG_STRING);
        for (int i = 0; i < notif.size(); i++) {
            try {
                d.notificationQueue.add(UUID.fromString(notif.getString(i)));
            } catch (IllegalArgumentException ignored) {}
        }
        return d;
    }
}
