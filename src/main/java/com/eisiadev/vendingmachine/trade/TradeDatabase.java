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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeDatabase {

    private static final TradeDatabase SERVER = new TradeDatabase();
    private static final TradeDatabase CLIENT = new TradeDatabase();

    public int version = 1;
    private final Map<UUID, TradeGroup> tradeGroups = new LinkedHashMap<>();
    private final Map<UUID, String> origins = new HashMap<>();
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
        origins.clear();
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
        boolean generated = addJson(root, "sync");
        logSummary();
        return generated;
    }

    public boolean addJson(JsonObject root, String source) {
        if (root.has("version")) version = Math.max(version, root.get("version").getAsInt());
        int generated = 0;
        JsonArray arr = root.has("tradeGroups") ? root.getAsJsonArray("tradeGroups") : new JsonArray();
        for (JsonElement e : arr) {
            TradeGroup tg = new TradeGroup();
            try {
                JsonObject obj = e.getAsJsonObject();
                if (tg.readJson(obj)) {
                    generated++;
                    if (!obj.has("id")) obj.addProperty("id", tg.getId().toString());
                    if (!obj.has("category")) obj.addProperty("category", tg.getCategory().getKey());
                }
            } catch (RuntimeException ex) {
                VendingMachine.LOG.error("Skipping invalid trade group in {}: {}", source, ex.getMessage());
                continue;
            }
            if (tradeGroups.containsKey(tg.getId())) {
                VendingMachine.LOG.error("Trade group id {} in {} is already defined in {}; skipping it.",
                    tg.getId(), source, origins.get(tg.getId()));
                continue;
            }
            tradeGroups.put(tg.getId(), tg);
            origins.put(tg.getId(), source);
            byCategory.computeIfAbsent(tg.getCategory(), k -> new ArrayList<>()).add(tg.getId());
        }
        if (generated > 0) VendingMachine.LOG.info("Generated metadata for {} new trade groups in {}", generated, source);
        return generated > 0;
    }

    public void logSummary() {
        VendingMachine.LOG.info("Loaded {} trade groups containing {} trades.", tradeGroups.size(), getTradeCount());
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
