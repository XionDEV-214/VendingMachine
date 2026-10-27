package com.eisiadev.vendingmachine.registry;

import com.eisiadev.vendingmachine.machine.VendingMachineController;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.network.chat.Component;

import static com.eisiadev.vendingmachine.VendingMachine.REGISTRATE;

public final class VMMachines {

    public static MultiblockMachineDefinition VENDING_MACHINE;

    private VMMachines() {}

    public static void init() {
        VENDING_MACHINE = REGISTRATE.multiblock("vending_machine", VendingMachineController::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .allowExtendedFacing(false)
            .recipeType(GTRecipeTypes.DUMMY_RECIPES)
            .appearanceBlock(VMBlocks.CASING)
            .tooltips(
                Component.translatable("vendingmachine.machine.vending_machine.tooltip.0"),
                Component.translatable("vendingmachine.machine.vending_machine.tooltip.1"))
            .pattern(definition -> FactoryBlockPattern.start()
                .aisle("CC", "CS", "CC")
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('C', Predicates.blocks(VMBlocks.CASING.get()))
                .build())
            .register();
    }
}
