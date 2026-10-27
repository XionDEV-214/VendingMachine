package com.eisiadev.vendingmachine.gui;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.machine.VendingMachineController;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.AbstractUIFactory;
import brachy.modularui.factory.GuiManager;
import brachy.modularui.factory.PosGuiData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VendingUIFactory extends AbstractUIFactory<PosGuiData> {

    public static final VendingUIFactory INSTANCE = new VendingUIFactory();

    private VendingUIFactory() {
        super(VendingMachine.id("vending_machine"));
    }

    public static void register() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void open(ServerPlayer player, VendingMachineController machine) {
        GuiManager.open(this, new PosGuiData(player, machine.getPos()), player);
    }

    @Nullable
    private static VendingMachineController find(PosGuiData data) {
        Player player = data.getPlayer();
        if (player == null) return null;
        MetaMachine machine = MetaMachine.getMachine(player.level(), data.getBlockPos());
        return machine instanceof VendingMachineController vm ? vm : null;
    }

    @Override
    public @NotNull IUIHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        VendingMachineController vm = find(data);
        if (vm == null) throw new IllegalStateException("No vending machine at " + data.getBlockPos());
        return vm;
    }

    @Override
    public boolean canInteractWith(Player player, PosGuiData data) {
        VendingMachineController vm = find(data);
        return vm != null && player == data.getPlayer() && !vm.isInValid() &&
            data.getSquaredDistance(player) <= 64;
    }

    @Override
    public void writeGuiData(PosGuiData data, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(data.getBlockPos());
    }

    @Override
    public @NotNull PosGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        return new PosGuiData(player, buffer.readBlockPos());
    }
}
