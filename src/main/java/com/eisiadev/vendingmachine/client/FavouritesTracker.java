package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.data.DataFiles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class FavouritesTracker {

    private record Key(UUID group, int order) {}

    private static final Set<Key> favourites = new HashSet<>();
    private static String loadedFor = null;
    private static boolean dirty = false;

    private FavouritesTracker() {}

    public static boolean isFavourite(UUID group, int order) {
        ensureLoaded();
        return favourites.contains(new Key(group, order));
    }

    public static void toggle(UUID group, int order) {
        ensureLoaded();
        Key k = new Key(group, order);
        if (!favourites.remove(k)) favourites.add(k);
        dirty = true;
    }

    public static void save() {
        if (!dirty || loadedFor == null) return;
        JsonArray arr = new JsonArray();
        for (Key k : favourites) {
            JsonObject o = new JsonObject();
            o.addProperty("tradeGroup", k.group().toString());
            o.addProperty("order", k.order());
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("favourites", arr);
        DataFiles.write(file(loadedFor), root);
        dirty = false;
    }

    private static void ensureLoaded() {
        String key = computeWorldKey();
        if (key == null || key.equals(loadedFor)) return;
        save();
        favourites.clear();
        loadedFor = key;
        JsonObject root = DataFiles.read(file(key));
        if (root == null || !root.has("favourites")) return;
        for (JsonElement e : root.getAsJsonArray("favourites")) {
            try {
                JsonObject o = e.getAsJsonObject();
                favourites.add(new Key(UUID.fromString(o.get("tradeGroup").getAsString()), o.get("order").getAsInt()));
            } catch (RuntimeException ex) {
                VendingMachine.LOG.warn("Ignoring invalid favourite entry {}", e);
            }
        }
    }

    private static Path file(String key) {
        return DataFiles.dataDir().resolve("favourites").resolve(key + ".json");
    }

    @Nullable
    private static String computeWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer sp = mc.getSingleplayerServer();
        if (sp != null) {
            return sanitize("SP_" + sp.getWorldData().getLevelName());
        }
        ServerData data = mc.getCurrentServer();
        if (data == null) return null;
        String ip = data.ip;
        String host = ip;
        String port = "";
        int colon = ip.lastIndexOf(':');
        if (colon >= 0) {
            host = ip.substring(0, colon);
            port = ip.substring(colon);
        }
        if (data.isLan() || isPrivateAddress(host)) {
            return sanitize("LAN_" + host);
        }
        return sanitize("MP_" + host + port);
    }

    private static boolean isPrivateAddress(String host) {
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168") ||
            host.startsWith("10.") || host.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
    }

    private static String sanitize(String s) {
        return s.replaceAll("[<>:\"/\\\\|?*]", "_");
    }
}
