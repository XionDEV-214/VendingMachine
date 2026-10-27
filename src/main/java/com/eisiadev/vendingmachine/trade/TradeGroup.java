package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.VendingMachine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TradeGroup {

    private UUID id;
    private String categoryKey;
    private TradeCategory category;
    public int cooldown = -1;
    public int maxTrades = -1;
    private final List<Trade> trades = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public TradeCategory getCategory() {
        return category;
    }

    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public boolean hasCooldown() {
        return cooldown != -1;
    }

    public boolean readJson(JsonObject json) {
        boolean generated = false;
        if (json.has("id")) {
            id = UUID.fromString(json.get("id").getAsString());
        } else {
            id = UUID.randomUUID();
            generated = true;
        }
        if (json.has("category")) {
            categoryKey = json.get("category").getAsString();
            category = TradeCategory.ofString(categoryKey);
        } else {
            categoryKey = TradeCategory.UNKNOWN.getKey();
            category = TradeCategory.UNKNOWN;
            generated = true;
        }
        cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : -1;
        maxTrades = json.has("maxTrades") ? json.get("maxTrades").getAsInt() : -1;
        trades.clear();
        if (json.has("trades")) {
            for (JsonElement e : json.getAsJsonArray("trades")) trades.add(Trade.fromJson(e.getAsJsonObject()));
        }
        if (json.has("requirements") && !json.getAsJsonArray("requirements").isEmpty()) {
            VendingMachine.LOG.warn("Trade group {} has requirements; unlock conditions are not supported and are ignored", id);
        }
        return generated;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("category", categoryKey);
        json.addProperty("cooldown", cooldown);
        json.addProperty("maxTrades", maxTrades);
        JsonArray arr = new JsonArray();
        trades.forEach(t -> arr.add(t.toJson()));
        json.add("trades", arr);
        return json;
    }
}
