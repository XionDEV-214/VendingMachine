package com.eisiadev.vendingmachine.currency;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public record CurrencyAmount(String type, long value) {

    public static CurrencyAmount fromJson(JsonObject json) {
        if (!json.has("type")) throw new JsonParseException("currency price is missing \"type\"");
        long value = json.has("value") ? json.get("value").getAsLong() : 0;
        if (value < 0) throw new JsonParseException("currency price must not be negative");
        return new CurrencyAmount(json.get("type").getAsString(), value);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("value", value);
        return json;
    }
}
