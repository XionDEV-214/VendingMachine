package com.eisiadev.vendingmachine.currency;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.world.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CurrencyRegistry {

    private static final CurrencyRegistry SERVER = new CurrencyRegistry();
    private static final CurrencyRegistry CLIENT = new CurrencyRegistry();

    private final Map<String, CurrencyDefinition> currencies = new LinkedHashMap<>();

    public static CurrencyRegistry get(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static CurrencyRegistry server() {
        return SERVER;
    }

    public static CurrencyRegistry client() {
        return CLIENT;
    }

    public Collection<CurrencyDefinition> all() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    @Nullable
    public CurrencyDefinition get(String id) {
        return currencies.get(id);
    }

    public boolean isEmpty() {
        return currencies.isEmpty();
    }

    @Nullable
    public Match match(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Match found = null;
        for (CurrencyDefinition def : currencies.values()) {
            CurrencyTier tier = def.findTier(stack);
            if (tier == null) continue;
            if (found != null) {
                VendingMachine.LOG.warn("{} matches both currency '{}' and '{}'; using '{}'", stack,
                    found.currency.getId(), def.getId(), found.currency.getId());
                break;
            }
            found = new Match(def, tier);
        }
        return found;
    }

    public long valueOf(ItemStack stack) {
        Match m = match(stack);
        if (m == null) return 0;
        long v = com.eisiadev.vendingmachine.storage.Wallet.checkedMultiply(m.tier.getValue(), stack.getCount());
        return v < 0 ? Long.MAX_VALUE : v;
    }

    public void clear() {
        currencies.clear();
    }

    public void readJson(JsonObject root) {
        currencies.clear();
        JsonArray arr = root.has("currencies") ? root.getAsJsonArray("currencies") : new JsonArray();
        for (JsonElement e : arr) {
            try {
                CurrencyDefinition def = CurrencyDefinition.fromJson(e.getAsJsonObject());
                if (currencies.containsKey(def.getId())) {
                    VendingMachine.LOG.error("Duplicate currency id '{}', ignoring the second one", def.getId());
                    continue;
                }
                currencies.put(def.getId(), def);
            } catch (RuntimeException ex) {
                VendingMachine.LOG.error("Skipping invalid currency entry: {}", ex.getMessage());
            }
        }
        VendingMachine.LOG.info("Loaded {} currencies", currencies.size());
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (CurrencyDefinition def : currencies.values()) arr.add(def.toJson());
        root.add("currencies", arr);
        return root;
    }

    public java.util.List<String> ids() {
        return new ArrayList<>(currencies.keySet());
    }

    public record Match(CurrencyDefinition currency, CurrencyTier tier) {}
}
