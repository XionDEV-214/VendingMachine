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

public class TradeStatusPacket {

    /**
     * @param maxQuantity how many units of this trade the player can buy in one purchase right now
     *                    (wallet, input items, trade limits and cooldown budget all considered; 0 = none)
     */
    public record Entry(UUID tradeGroup, int order, long cooldownRemaining, boolean enabled, boolean tradeableNow,
                        int cdTradeCount, int maxQuantity) {}

    public final List<Entry> entries;
    public final int tradesPerCooldown;

    public TradeStatusPacket(List<Entry> entries, int tradesPerCooldown) {
        this.entries = entries;
        this.tradesPerCooldown = tradesPerCooldown;
    }

    public static void encode(TradeStatusPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.tradesPerCooldown);
        buf.writeVarInt(p.entries.size());
        for (Entry e : p.entries) {
            buf.writeUUID(e.tradeGroup());
            buf.writeVarInt(e.order());
            buf.writeLong(e.cooldownRemaining());
            buf.writeBoolean(e.enabled());
            buf.writeBoolean(e.tradeableNow());
            buf.writeVarInt(e.cdTradeCount());
            buf.writeVarInt(e.maxQuantity());
        }
    }

    public static TradeStatusPacket decode(FriendlyByteBuf buf) {
        int perCooldown = buf.readVarInt();
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readUUID(), buf.readVarInt(), buf.readLong(), buf.readBoolean(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt()));
        }
        return new TradeStatusPacket(list, perCooldown);
    }

    public static void handle(TradeStatusPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleTradeStatus(p));
        ctx.get().setPacketHandled(true);
    }
}
