package com.eisiadev.vendingmachine.registry;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.blocks.VendingMachineCasingBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VMBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
        VendingMachine.MODID);

    public static final RegistryObject<VendingMachineCasingBlock> CASING = BLOCKS.register("vending_machine_casing",
        () -> new VendingMachineCasingBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()));

    private VMBlocks() {}
}
