package com.eisiadev.vendingmachine.currency;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CurrencyDefinition {

    private final String id;
    private final String name;
    private final List<CurrencyTier> tiers;

    public CurrencyDefinition(String id, String name, List<CurrencyTier> tiers) {
        this.id = id;
        this.name = name;
        List<CurrencyTier> sorted = new ArrayList<>(tiers);
        sorted.sort(Comparator.comparingLong(CurrencyTier::getValue));
        this.tiers = Collections.unmodifiableList(sorted);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return Component.translatableWithFallback(name, isTranslationKey() ? defaultEnglishName() : name);
    }

    public boolean isTranslationKey() {
        return name.indexOf('.') > 0 && name.chars().noneMatch(Character::isWhitespace);
    }

    public String defaultEnglishName() {
        StringBuilder sb = new StringBuilder();
        for (String word : id.split("[_\\-\\s]+")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    public List<CurrencyTier> getTiers() {
        return tiers;
    }

    @Nullable
    public CurrencyTier findTier(ItemStack stack) {
        for (int i = tiers.size() - 1; i >= 0; i--) {
            if (tiers.get(i).matches(stack)) return tiers.get(i);
        }
        return null;
    }

    public ItemStack getIcon(long amount) {
        CurrencyTier best = tiers.get(0);
        for (CurrencyTier tier : tiers) {
            if (amount >= tier.getValue()) best = tier;
        }
        return best.createCoin();
    }

    public ItemStack getBaseIcon() {
        return tiers.get(0).createCoin();
    }

    public record Itemized(List<ItemStack> stacks, long value) {}

    public Itemized itemize(long amount, int maxStacks) {
        List<ItemStack> out = new ArrayList<>();
        long remaining = amount;
        long paid = 0;
        for (int i = tiers.size() - 1; i >= 0 && remaining > 0 && out.size() < maxStacks; i--) {
            CurrencyTier tier = tiers.get(i);
            long coins = remaining / tier.getValue();
            if (coins <= 0) continue;
            ItemStack coin = tier.createCoin();
            if (coin.isEmpty()) continue;
            int max = Math.max(1, coin.getMaxStackSize());
            while (coins > 0 && out.size() < maxStacks) {
                int size = (int) Math.min(coins, max);
                ItemStack s = coin.copy();
                s.setCount(size);
                out.add(s);
                coins -= size;
                long value = size * tier.getValue();
                remaining -= value;
                paid += value;
            }
        }
        return new Itemized(out, paid);
    }

    public static CurrencyDefinition fromJson(JsonObject json) {
        if (!json.has("id")) throw new JsonParseException("currency is missing \"id\"");
        String id = json.get("id").getAsString();
        if (id.isEmpty() || id.equalsIgnoreCase("all")) {
            throw new JsonParseException("currency id '" + id + "' is not allowed");
        }
        String name = json.has("name") ? json.get("name").getAsString() : "vendingmachine.currency." + id;
        JsonArray tierArray = json.has("tiers") ? json.getAsJsonArray("tiers") : new JsonArray();
        if (tierArray.isEmpty()) throw new JsonParseException("currency '" + id + "' has no tiers");
        List<CurrencyTier> tiers = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonElement e : tierArray) {
            CurrencyTier tier = CurrencyTier.fromJson(e.getAsJsonObject(), id);
            if (!seen.add(tier.getValue())) {
                throw new JsonParseException("currency '" + id + "' has two tiers with value " + tier.getValue());
            }
            tiers.add(tier);
        }
        CurrencyDefinition def = new CurrencyDefinition(id, name, tiers);
        if (def.tiers.get(0).getValue() != 1) {
            throw new JsonParseException("the lowest tier of currency '" + id + "' must have value 1");
        }
        return def;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        JsonArray arr = new JsonArray();
        for (CurrencyTier tier : tiers) arr.add(tier.toJson());
        json.add("tiers", arr);
        return json;
    }
}
