package com.eisiadev.vendingmachine;

import com.eisiadev.vendingmachine.gui.DisplayType;
import com.eisiadev.vendingmachine.gui.MusicTrack;
import com.eisiadev.vendingmachine.gui.SortMode;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VMConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.IntValue GUI_REFRESH_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue RESTOCK_NOTIFICATIONS;
    public static final ForgeConfigSpec.IntValue RESTOCK_CHECK_INTERVAL;
    public static final ForgeConfigSpec.IntValue TRADES_PER_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<String> DATA_DIR;
    public static final ForgeConfigSpec.BooleanValue FORCE_REWRITE_DATABASE;

    public static final ForgeConfigSpec.EnumValue<DisplayType> DISPLAY_TYPE;
    public static final ForgeConfigSpec.EnumValue<SortMode> SORT_MODE;
    public static final ForgeConfigSpec.BooleanValue SHOW_COINS;
    public static final ForgeConfigSpec.BooleanValue SHOW_RESTOCK_NOTIFICATIONS;
    public static final ForgeConfigSpec.EnumValue<MusicTrack> MUSIC_TRACK;
    public static final ForgeConfigSpec.DoubleValue MUSIC_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("vendingMachine");
        GUI_REFRESH_INTERVAL = b.comment("Ticks between trade availability updates sent to a player using a vending machine.")
            .defineInRange("guiRefreshInterval", 20, 1, 1200);
        RESTOCK_NOTIFICATIONS = b.comment("Notify players in chat when a trade comes off cooldown.")
            .define("restockNotifications", true);
        RESTOCK_CHECK_INTERVAL = b.comment("Ticks between restock notification checks.")
            .defineInRange("restockCheckInterval", 200, 20, 72000);
        TRADES_PER_COOLDOWN = b.comment(
            "How many times a trade group with a cooldown can be used before its cooldown blocks it.",
            "The original mod used the number of online team members; this port is solo-only.")
            .defineInRange("tradesPerCooldown", 1, 1, 1000);
        b.pop();
        b.push("developer");
        DATA_DIR = b.comment("Directory (relative to the game directory) holding currencies.json and the tradeDatabase folder.")
            .define("dataDir", "config/vendingmachine");
        FORCE_REWRITE_DATABASE = b.comment("Rewrite every file in the tradeDatabase folder on load even if nothing was generated (normalises formatting).")
            .define("forceRewriteDatabase", false);
        b.pop();
        COMMON_SPEC = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.push("gui");
        DISPLAY_TYPE = c.defineEnum("displayType", DisplayType.TILE);
        SORT_MODE = c.defineEnum("sortMode", SortMode.SMART);
        SHOW_COINS = c.define("showCoins", true);
        SHOW_RESTOCK_NOTIFICATIONS = c.comment("Show restock notifications sent by the server.")
            .define("showRestockNotifications", true);
        c.pop();
        c.push("music");
        MUSIC_TRACK = c.comment("Music played while the vending machine is open (NONE to turn it off).")
            .defineEnum("track", MusicTrack.LUNCH_BREAK);
        MUSIC_VOLUME = c.comment("Volume of the vending machine music, on top of the Jukebox/Note Blocks slider.")
            .defineInRange("volume", 0.75, 0.0, 1.0);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    private VMConfig() {}
}
