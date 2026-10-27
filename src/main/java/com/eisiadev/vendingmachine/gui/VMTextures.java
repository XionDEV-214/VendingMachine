package com.eisiadev.vendingmachine.gui;

import com.eisiadev.vendingmachine.VendingMachine;

import brachy.modularui.drawable.UITexture;

public final class VMTextures {

    public static final UITexture TRADE_BACKGROUND = adaptable("gui/background/trade_background", 61, 12, 4);
    public static final UITexture SIDE_PANEL_BACKGROUND = adaptable("gui/background/panel_side", 50, 214, 4);
    public static final UITexture TEXT_FIELD_BACKGROUND = adaptable("gui/background/text_field_light_gray", 61, 12, 1);

    public static final UITexture TILE_BUTTON = adaptable("gui/background/trade_button_unpressed", 47, 25, 4);
    public static final UITexture TILE_BUTTON_PRESSED = adaptable("gui/background/trade_button_pressed", 47, 25, 4);
    public static final UITexture LIST_BUTTON = adaptable("gui/background/list_trade_button_unpressed", 153, 15, 4);
    public static final UITexture LIST_BUTTON_PRESSED = adaptable("gui/background/list_trade_button_pressed", 153, 15, 4);

    public static final UITexture OVERLAY_TRADEABLE = overlay("gui/overlay/tile_tradeable", 47, 25);
    public static final UITexture OVERLAY_SELECTED = overlay("gui/overlay/tile_selected", 47, 25);
    public static final UITexture OVERLAY_COOLDOWN = overlay("gui/overlay/tile_cooldown", 47, 25);

    public static final UITexture IO_BANNER = full("gui/background/input");
    public static final UITexture DISPENSER_BACKGROUND = full("gui/background/dispenser_background");
    public static final UITexture DISPENSER_OVERHANG = full("gui/background/dispenser_overhang");
    public static final UITexture FAVOURITE = full("gui/icons/favourite_indicator");
    public static final UITexture COIN_RETURN = full("gui/overlay/coin_return");
    public static final UITexture EJECT_ITEMS = full("gui/overlay/slot_empty");
    public static final UITexture SHOW_COINS = full("gui/overlay/show_coins");
    public static final UITexture HIDE_COINS = full("gui/overlay/hide_coins");
    public static final UITexture TAB_HIGHLIGHT = full("gui/overlay/filtered_tab");

    private VMTextures() {}

    public static UITexture full(String path) {
        return UITexture.fullImage(VendingMachine.MODID, path);
    }

    public static UITexture categoryIcon(String key) {
        return full("gui/icons/" + key);
    }

    public static UITexture displayType(DisplayType type) {
        return full("gui/overlay/" + type.icon);
    }

    public static UITexture sortMode(SortMode mode) {
        return full("gui/overlay/" + mode.icon);
    }

    public static UITexture musicTrack(MusicTrack track) {
        return full("gui/overlay/" + track.icon);
    }

    private static UITexture adaptable(String path, int w, int h, int border) {
        return UITexture.builder()
            .location(VendingMachine.MODID, path)
            .imageSize(w, h)
            .adaptable(border)
            .build();
    }

    private static UITexture overlay(String path, int w, int h) {
        return UITexture.builder()
            .location(VendingMachine.MODID, path)
            .imageSize(w, h)
            .adaptable(4)
            .nonOpaque()
            .build();
    }
}
