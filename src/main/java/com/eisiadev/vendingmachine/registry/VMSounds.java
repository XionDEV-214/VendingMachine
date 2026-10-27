package com.eisiadev.vendingmachine.registry;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VMSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
        VendingMachine.MODID);

    public static final RegistryObject<SoundEvent> COIN_DROP = register("coin_drop");
    public static final RegistryObject<SoundEvent> COIN_INSERT = register("coin_insert");
    public static final RegistryObject<SoundEvent> ITEM_DROP = register("item_drop");
    public static final RegistryObject<SoundEvent> LUNCH_BREAK = register("track.lunch_break");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(VendingMachine.id(name)));
    }

    private VMSounds() {}
}
