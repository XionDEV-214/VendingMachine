package com.eisiadev.vendingmachine.gui;

public enum DisplayType {

    TILE("vendingmachine.gui.display_mode_tile", "mode_tile"),
    LIST("vendingmachine.gui.display_mode_list", "mode_list");

    public final String translationKey;
    public final String icon;

    DisplayType(String translationKey, String icon) {
        this.translationKey = translationKey;
        this.icon = icon;
    }
}
