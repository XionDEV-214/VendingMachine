package com.eisiadev.vendingmachine.currency;

import com.eisiadev.vendingmachine.trade.ItemIngredient;

import net.minecraft.world.item.ItemStack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

public final class CurrencyTier {

    private final long value;
    private final ItemIngredient match;
    private final ItemIngredient output;

    public CurrencyTier(long value, ItemIngredient match, ItemIngredient output) {
        this.value = value;
        this.match = match;
        this.output = output;
    }

    public long getValue() {
        return value;
    }

    public ItemIngredient getMatch() {
        return match;
    }

    public ItemIngredient getOutput() {
        return output;
    }

    public boolean matches(ItemStack stack) {
        return match.matches(stack);
    }

    public ItemStack createCoin() {
        return output.getDisplayStack().copy();
    }

    static CurrencyTier fromJson(JsonObject json, String currencyId) {
        if (!json.has("value")) throw new JsonParseException("tier of '" + currencyId + "' is missing \"value\"");
        long value = json.get("value").getAsLong();
        if (value <= 0) throw new JsonParseException("tier value of '" + currencyId + "' must be positive");
        if (!json.has("match")) throw new JsonParseException("tier of '" + currencyId + "' is missing \"match\"");
        ItemIngredient match = parseRef(json.get("match").getAsString());
        ItemIngredient output;
        if (json.has("output")) {
            output = parseRef(json.get("output").getAsString());
            if (output.isTag()) {
                throw new JsonParseException("tier output of '" + currencyId + "' must be an item id, not a tag");
            }
        } else if (match.isTag()) {
            throw new JsonParseException(
                "tier '" + json.get("match").getAsString() + "' of '" + currencyId +
                    "' matches a tag, so it needs an \"output\" item");
        } else {
            output = match;
        }
        return new CurrencyTier(value, match, output);
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("value", value);
        json.addProperty("match", refString(match));
        if (output != match) json.addProperty("output", refString(output));
        return json;
    }

    private static ItemIngredient parseRef(String ref) {
        return ref.startsWith("#") ? ItemIngredient.ofTag(ref.substring(1), 1) : ItemIngredient.ofItem(ref, 1, null);
    }

    private static String refString(@Nullable ItemIngredient ing) {
        if (ing == null) return "";
        return ing.isTag() ? "#" + ing.getTagId() : ing.getItemId();
    }
}
