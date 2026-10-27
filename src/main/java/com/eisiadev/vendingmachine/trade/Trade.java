package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.currency.CurrencyAmount;

import net.minecraft.world.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Trade {

    public final List<CurrencyAmount> fromCurrency = new ArrayList<>();
    public final List<ItemIngredient> fromItems = new ArrayList<>();
    public final List<ItemIngredient> nonConsumedItems = new ArrayList<>();
    public final List<ItemIngredient> toItems = new ArrayList<>();
    @Nullable
    public ItemIngredient displayItem;

    public ItemStack getDisplayStack() {
        if (displayItem != null) return displayItem.getDisplayStackWithCount();
        if (!toItems.isEmpty()) return toItems.get(0).getDisplayStackWithCount();
        return ItemStack.EMPTY;
    }

    public static Trade fromJson(JsonObject json) {
        Trade t = new Trade();
        if (json.has("displayItem")) t.displayItem = ItemIngredient.fromJson(json.getAsJsonObject("displayItem"));
        if (json.has("fromCurrency")) {
            for (JsonElement e : json.getAsJsonArray("fromCurrency")) {
                t.fromCurrency.add(CurrencyAmount.fromJson(e.getAsJsonObject()));
            }
        }
        readList(json, "fromItems", t.fromItems);
        readList(json, "nonConsumedItems", t.nonConsumedItems);
        readList(json, "toItems", t.toItems);
        return t;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (displayItem != null) json.add("displayItem", displayItem.toJson());
        if (!fromCurrency.isEmpty()) {
            JsonArray arr = new JsonArray();
            fromCurrency.forEach(c -> arr.add(c.toJson()));
            json.add("fromCurrency", arr);
        }
        writeList(json, "fromItems", fromItems);
        writeList(json, "nonConsumedItems", nonConsumedItems);
        writeList(json, "toItems", toItems);
        return json;
    }

    private static void readList(JsonObject json, String key, List<ItemIngredient> out) {
        if (!json.has(key)) return;
        for (JsonElement e : json.getAsJsonArray(key)) out.add(ItemIngredient.fromJson(e.getAsJsonObject()));
    }

    private static void writeList(JsonObject json, String key, List<ItemIngredient> list) {
        if (list.isEmpty()) return;
        JsonArray arr = new JsonArray();
        list.forEach(i -> arr.add(i.toJson()));
        json.add(key, arr);
    }
}
