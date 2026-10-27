package com.eisiadev.vendingmachine.network;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VMNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        VendingMachine.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered = false;

    private VMNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(SyncDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncDataPacket::encode)
            .decoder(SyncDataPacket::decode)
            .consumerMainThread(SyncDataPacket::handle)
            .add();
        CHANNEL.messageBuilder(TradeStatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(TradeStatusPacket::encode)
            .decoder(TradeStatusPacket::decode)
            .consumerMainThread(TradeStatusPacket::handle)
            .add();
        CHANNEL.messageBuilder(TradeNotificationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(TradeNotificationPacket::encode)
            .decoder(TradeNotificationPacket::decode)
            .consumerMainThread(TradeNotificationPacket::handle)
            .add();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
