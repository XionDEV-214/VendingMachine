package com.eisiadev.vendingmachine.handlers;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.command.VendingCommand;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.data.DataFiles;
import com.eisiadev.vendingmachine.network.SyncDataPacket;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VendingMachine.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {

    private static int tickCounter = 0;

    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        DataFiles.loadAll();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CurrencyRegistry.server().clear();
        TradeDatabase.server().clear();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        VendingCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyncDataPacket.send(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !VMConfig.RESTOCK_NOTIFICATIONS.get()) return;
        if (++tickCounter < VMConfig.RESTOCK_CHECK_INTERVAL.get()) return;
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TradeManager.sendTradeNotifications(player);
        }
    }
}
