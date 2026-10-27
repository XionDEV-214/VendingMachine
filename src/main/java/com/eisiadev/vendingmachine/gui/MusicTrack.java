package com.eisiadev.vendingmachine.gui;

import com.eisiadev.vendingmachine.registry.VMSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

public enum MusicTrack {

    NONE("none", null, "audio_off"),
    LUNCH_BREAK("lunch_break", VMSounds.LUNCH_BREAK, "audio_on");

    public final String translationKey;
    @Nullable
    private final RegistryObject<SoundEvent> sound;
    public final String icon;

    MusicTrack(String name, @Nullable RegistryObject<SoundEvent> sound, String icon) {
        this.translationKey = "vendingmachine.gui.display_track_" + name;
        this.sound = sound;
        this.icon = icon;
    }

    @Nullable
    public SoundEvent getSound() {
        return sound == null ? null : sound.get();
    }
}
