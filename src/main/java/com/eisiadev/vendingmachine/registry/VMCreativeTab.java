package com.eisiadev.vendingmachine.registry;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class VMCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
        VendingMachine.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.vendingmachine"))
        .icon(() -> VMMachines.VENDING_MACHINE != null ? VMMachines.VENDING_MACHINE.asStack() :
            new ItemStack(VMItems.CASING.get()))
        .displayItems((params, output) -> {
            if (VMMachines.VENDING_MACHINE != null) output.accept(VMMachines.VENDING_MACHINE.asStack());
            output.accept(VMItems.CASING.get());
        })
        .build());

    private VMCreativeTab() {}
}
