package com.eisiadev.vendingmachine.network;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.client.ClientPacketHandlers;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.trade.TradeDatabase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

public class SyncDataPacket {

    private static final int CHUNK_SIZE = 512 * 1024;

    public final int index;
    public final int total;
    public final byte[] data;

    public SyncDataPacket(int index, int total, byte[] data) {
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(SyncDataPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.total);
        buf.writeByteArray(p.data);
    }

    public static SyncDataPacket decode(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        int total = buf.readVarInt();
        byte[] data = buf.readByteArray(CHUNK_SIZE + 16);
        return new SyncDataPacket(index, total, data);
    }

    public static void handle(SyncDataPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSyncData(p));
        ctx.get().setPacketHandled(true);
    }

    public static void send(ServerPlayer player) {
        JsonObject root = new JsonObject();
        root.add("currencies", CurrencyRegistry.server().toJson());
        root.add("trades", TradeDatabase.server().toJson());
        byte[] bytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
                gz.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
            bytes = bos.toByteArray();
        } catch (IOException e) {
            VendingMachine.LOG.error("Failed to encode trade data", e);
            return;
        }
        int total = Math.max(1, (bytes.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int i = 0; i < total; i++) {
            byte[] chunk = Arrays.copyOfRange(bytes, i * CHUNK_SIZE, Math.min(bytes.length, (i + 1) * CHUNK_SIZE));
            SyncDataPacket packet = new SyncDataPacket(i, total, chunk);
            if (player == null) VMNetwork.sendToAll(packet);
            else VMNetwork.sendTo(player, packet);
        }
    }
}
