package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.VendingMachine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeDatabase {

    private static final TradeDatabase SERVER = new TradeDatabase();
    private static final TradeDatabase CLIENT = new TradeDatabase();

    public int version = 1;
    private final Map<UUID, TradeGroup> tradeGroups = new LinkedHashMap<>();
    private final Map<TradeCategory, List<UUID>> byCategory = new EnumMap<>(TradeCategory.class);

    public static TradeDatabase get(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static TradeDatabase server() {
        return SERVER;
    }

    public static TradeDatabase client() {
        return CLIENT;
    }

    public void clear() {
        tradeGroups.clear();
        byCategory.clear();
    }

    @Nullable
    public TradeGroup getTradeGroup(UUID id) {
        return tradeGroups.get(id);
    }

    public Collection<TradeGroup> getTradeGroups() {
        return Collections.unmodifiableCollection(tradeGroups.values());
    }

    public List<TradeCategory> getCategories() {
        List<TradeCategory> list = new ArrayList<>(byCategory.keySet());
        list.sort(Comparator.comparingInt(Enum::ordinal));
        return list;
    }

    public int getTradeCount() {
        return tradeGroups.values().stream().mapToInt(tg -> tg.getTrades().size()).sum();
    }

    public boolean readJson(JsonObject root) {
        clear();
        version = root.has("version") ? root.get("version").getAsInt() : 1;
        int generated = 0;
        JsonArray arr = root.has("tradeGroups") ? root.getAsJsonArray("tradeGroups") : new JsonArray();
        for (JsonElement e : arr) {
            TradeGroup tg = new TradeGroup();
            try {
                if (tg.readJson(e.getAsJsonObject())) generated++;
            } catch (RuntimeException ex) {
                VendingMachine.LOG.error("Skipping invalid trade group: {}", ex.getMessage());
                continue;
            }
            if (tradeGroups.containsKey(tg.getId())) {
                VendingMachine.LOG.error("Multiple trade groups with id {} exist in the file!", tg.getId());
                continue;
            }
            tradeGroups.put(tg.getId(), tg);
            byCategory.computeIfAbsent(tg.getCategory(), k -> new ArrayList<>()).add(tg.getId());
        }
        if (generated > 0) VendingMachine.LOG.info("Generated metadata for {} new trade groups", generated);
        VendingMachine.LOG.info("Loaded {} trade groups containing {} trades.", tradeGroups.size(), getTradeCount());
        return generated > 0;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", version);
        JsonArray arr = new JsonArray();
        tradeGroups.values().forEach(tg -> arr.add(tg.toJson()));
        root.add("tradeGroups", arr);
        return root;
    }
}
