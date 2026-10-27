package com.eisiadev.vendingmachine.trade;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum TradeCategory {

    FAVOURITES("favourites"),
    UNKNOWN("unknown"),
    ALL("all"),
    COMPONENTS("components"),
    RAW("raw"),
    FARMING("farming"),
    CHEMISTRY("chemistry"),
    MAGIC("magic"),
    BEES("bees"),
    MISC("misc");

    private static final Map<String, TradeCategory> BY_KEY = new HashMap<>();

    static {
        for (TradeCategory c : values()) BY_KEY.put(c.key, c);
    }

    private final String key;

    TradeCategory(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getTranslationKey() {
        return "vendingmachine.category." + key;
    }

    public ResourceLocation getIconTexture() {
        return VendingMachine.id("textures/gui/icons/" + key + ".png");
    }

    public static TradeCategory ofString(String key) {
        TradeCategory c = BY_KEY.get(key.toLowerCase(Locale.ROOT));
        if (c == null) {
            VendingMachine.LOG.warn("Unknown trade category {}, defaulting to UNKNOWN", key);
            return UNKNOWN;
        }
        return c;
    }
}
