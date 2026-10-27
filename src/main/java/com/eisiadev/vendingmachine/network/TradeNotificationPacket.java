package com.eisiadev.vendingmachine.network;

import com.eisiadev.vendingmachine.client.ClientPacketHandlers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class TradeNotificationPacket {

    public final List<UUID> tradeGroups;

    public TradeNotificationPacket(List<UUID> tradeGroups) {
        this.tradeGroups = tradeGroups;
    }

    public static void encode(TradeNotificationPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.tradeGroups.size());
        p.tradeGroups.forEach(buf::writeUUID);
    }

    public static TradeNotificationPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<UUID> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUUID());
        return new TradeNotificationPacket(list);
    }

    public static void handle(TradeNotificationPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleNotification(p));
        ctx.get().setPacketHandled(true);
    }
}
