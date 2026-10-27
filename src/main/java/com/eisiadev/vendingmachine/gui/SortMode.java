package com.eisiadev.vendingmachine.gui;

public enum SortMode {

    SMART("vendingmachine.gui.display_sort_smart", "sort_smart"),
    ALPHABET("vendingmachine.gui.display_sort_alphabet", "sort_alphabet");

    public final String translationKey;
    public final String icon;

    SortMode(String translationKey, String icon) {
        this.translationKey = translationKey;
        this.icon = icon;
    }
}
