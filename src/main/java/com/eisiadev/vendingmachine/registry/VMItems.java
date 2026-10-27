package com.eisiadev.vendingmachine.registry;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.items.PlaceholderItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

public final class VMItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
        VendingMachine.MODID);

    public static final RegistryObject<Item> CASING = ITEMS.register("vending_machine_casing",
        () -> new BlockItem(VMBlocks.CASING.get(), new Item.Properties()));

    public static final RegistryObject<Item> PLACEHOLDER = ITEMS.register("placeholder",
        () -> new PlaceholderItem(new Item.Properties()));

    private VMItems() {}

    public static ItemStack placeholderFor(@Nullable String originalId) {
        if (!PLACEHOLDER.isPresent()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(PLACEHOLDER.get());
        stack.getOrCreateTag().putString(PlaceholderItem.ORIGINAL_ID, originalId == null ? "?" : originalId);
        return stack;
    }
}
