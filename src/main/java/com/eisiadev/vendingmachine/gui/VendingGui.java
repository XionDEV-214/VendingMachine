package com.eisiadev.vendingmachine.gui;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.client.vending.VendingClientUI;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.machine.VendingMachineController;
import com.eisiadev.vendingmachine.storage.Wallet;
import com.eisiadev.vendingmachine.trade.TradeManager;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.LongSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Builds the vending machine panel on both sides. The server side only needs the sync handlers, slots, actions
 * and the player inventory; everything that is drawn is built by {@link VendingClientUI} on the client.
 */
public class VendingGui {

    public static final String PANEL_NAME = "vending_machine";
    public static final String SLOT_INPUT = "vm_input";
    public static final String SLOT_OUTPUT = "vm_output";
    public static final String SYNC_WALLET = "wallet_";
    public static final String ACTION_TRADE = "trade";
    public static final String ACTION_EJECT_ITEMS = "eject_items";
    public static final String ACTION_EJECT_COINS = "eject_coins";
    public static final String ACTION_COLLECT = "collect_outputs";

    public static final int WIDTH = 178;
    public static final int HEIGHT = 326;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 245;

    private static boolean openOnClient = false;

    private final VendingMachineController machine;
    private final PosGuiData data;
    private final PanelSyncManager syncManager;
    private final boolean client;

    /** When each output slot last received a new item (client only), for the chute's drop animation. */
    private final long[] outputDropTime = new long[VendingMachineController.OUTPUT_SLOTS];
    private Runnable onClientClose = () -> {};

    public VendingGui(VendingMachineController machine, PosGuiData data, PanelSyncManager syncManager) {
        this.machine = machine;
        this.data = data;
        this.syncManager = syncManager;
        this.client = syncManager.isClient();
    }

    @OnlyIn(Dist.CLIENT)
    public static ModularScreen createScreen(ModularPanel<?> panel) {
        return new ModularScreen(VendingMachine.MODID, panel);
    }

    public static boolean isOpen() {
        return openOnClient;
    }

    public ModularPanel<?> build(UISettings settings) {
        registerSync();

        ModularPanel<?> panel = new ModularPanel<>(PANEL_NAME);
        panel.size(WIDTH, HEIGHT);

        if (client) {
            openOnClient = true;
            onClientClose = VendingClientUI.build(panel, this);
        } else {
            panel.child(SlotGroupWidget.playerInventory(false).pos(INVENTORY_X, INVENTORY_Y));
        }
        return panel;
    }

    private void registerSync() {
        Player player = data.getPlayer();
        syncManager.bindPlayerInventory(player);

        // Input slots (coins and trade ingredients) and the output chute, registered by key on both sides so the
        // client can draw them however it likes.
        syncManager.registerSlotGroup(SLOT_INPUT, 2, true);
        syncManager.registerSlotGroup(SLOT_OUTPUT, 2, false);
        for (int i = 0; i < VendingMachineController.INPUT_SLOTS; i++) {
            syncManager.itemSlot(SLOT_INPUT, i, new ModularSlot(machine.inputItems, i).slotGroup(SLOT_INPUT));
        }
        for (int i = 0; i < VendingMachineController.OUTPUT_SLOTS; i++) {
            int slot = i;
            syncManager.itemSlot(SLOT_OUTPUT, i, new ModularSlot(machine.outputItems, i)
                .accessibility(false, true)
                .slotGroup(SLOT_OUTPUT)
                .changeListener((stack, onlyAmount, isClient, init) -> {
                    if (isClient && !init && !stack.isEmpty() && !onlyAmount) outputDropTime[slot] = Util.getMillis();
                }));
        }

        for (CurrencyDefinition def : CurrencyRegistry.get(client).all()) {
            String id = def.getId();
            syncManager.syncValue(SYNC_WALLET + id, new LongSyncValue(() -> {
                if (client) return 0L;
                Wallet wallet = TradeManager.getWallet(player.getUUID());
                return wallet == null ? 0L : wallet.get(id);
            }));
        }

        syncManager.registerServerSyncedAction(ACTION_TRADE, buf -> {
            UUID group = buf.readUUID();
            int order = buf.readVarInt();
            int quantity = buf.readVarInt();
            if (player instanceof ServerPlayer sp) machine.queueTrade(sp, group, order, quantity);
        });
        syncManager.registerServerSyncedAction(ACTION_EJECT_ITEMS, buf -> machine.ejectInputItems());
        syncManager.registerServerSyncedAction(ACTION_EJECT_COINS, buf -> {
            String currency = buf.readUtf();
            if (player instanceof ServerPlayer sp) machine.ejectCoins(sp, currency.isEmpty() ? null : currency);
        });
        syncManager.registerServerSyncedAction(ACTION_COLLECT, buf -> machine.collectOutputs(player));

        syncManager.addCloseListener(p -> {
            if (client) {
                openOnClient = false;
                onClientClose.run();
            } else {
                machine.resetCurrentUser(p);
            }
        });
    }

    // ---------- Accessors for the client UI ----------

    public VendingMachineController machine() {
        return machine;
    }

    public Player player() {
        return data.getPlayer();
    }

    public PanelSyncManager sync() {
        return syncManager;
    }

    public long outputDropTime(int slot) {
        return outputDropTime[slot];
    }
}
