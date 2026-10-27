package com.eisiadev.vendingmachine.storage;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VMSavedData extends SavedData {

    private static final String NAME = VendingMachine.MODID;

    private final Map<UUID, PlayerVendingData> players = new HashMap<>();

    public static VMSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(VMSavedData::load, VMSavedData::new, NAME);
    }

    public PlayerVendingData getPlayer(UUID player) {
        return players.computeIfAbsent(player, k -> new PlayerVendingData());
    }

    public Map<UUID, PlayerVendingData> getAll() {
        return players;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag p = new CompoundTag();
        players.forEach((id, data) -> p.put(id.toString(), data.save()));
        tag.put("players", p);
        return tag;
    }

    public static VMSavedData load(CompoundTag tag) {
        VMSavedData data = new VMSavedData();
        CompoundTag p = tag.getCompound("players");
        for (String key : p.getAllKeys()) {
            try {
                data.players.put(UUID.fromString(key), PlayerVendingData.load(p.getCompound(key)));
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }
}
