package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.gui.VendingGui;
import com.eisiadev.vendingmachine.network.SyncDataPacket;
import com.eisiadev.vendingmachine.network.TradeNotificationPacket;
import com.eisiadev.vendingmachine.network.TradeStatusPacket;
import com.eisiadev.vendingmachine.trade.ItemIngredient;
import com.eisiadev.vendingmachine.trade.Trade;
import com.eisiadev.vendingmachine.trade.TradeDatabase;
import com.eisiadev.vendingmachine.trade.TradeGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    private static final int MAX_NOTIFICATION_ITEMS = 4;

    private static ByteArrayOutputStream pending;
    private static int expectedNext = 0;

    private ClientPacketHandlers() {}

    public static void handleSyncData(SyncDataPacket p) {
        if (p.index == 0) {
            pending = new ByteArrayOutputStream();
            expectedNext = 0;
        }
        if (pending == null || p.index != expectedNext) {
            VendingMachine.LOG.error("Out of order trade data packet {}/{}", p.index, p.total);
            pending = null;
            return;
        }
        pending.writeBytes(p.data);
        expectedNext++;
        if (expectedNext < p.total) return;

        byte[] all = pending.toByteArray();
        pending = null;
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(all))) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            CurrencyRegistry.client().readJson(root.getAsJsonObject("currencies"));
            TradeDatabase.client().readJson(root.getAsJsonObject("trades"));
            ClientTradeState.clear();
        } catch (IOException | RuntimeException e) {
            VendingMachine.LOG.error("Failed to read trade data from server", e);
        }
        try {
            GeneratedLang.update(CurrencyRegistry.client().all());
        } catch (RuntimeException e) {
            VendingMachine.LOG.warn("Could not update generated currency translations", e);
        }
    }

    public static void handleTradeStatus(TradeStatusPacket p) {
        ClientTradeState.update(p);
    }

    public static void handleNotification(TradeNotificationPacket p) {
        if (!VMConfig.SHOW_RESTOCK_NOTIFICATIONS.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (VendingGui.isOpen()) return;

        List<String> names = new ArrayList<>();
        for (UUID id : p.tradeGroups) {
            TradeGroup tg = TradeDatabase.client().getTradeGroup(id);
            if (tg == null) continue;
            for (Trade t : tg.getTrades()) {
                for (ItemIngredient out : t.toItems) names.add(out.getDisplayStack().getHoverName().getString());
            }
        }
        if (names.isEmpty()) return;
        String text;
        if (names.size() > MAX_NOTIFICATION_ITEMS) {
            text = String.join(", ", names.subList(0, MAX_NOTIFICATION_ITEMS)) + ", ... [+" +
                (names.size() - MAX_NOTIFICATION_ITEMS) + "]";
        } else {
            text = String.join(", ", names);
        }
        mc.player.displayClientMessage(
            Component.translatable("vendingmachine.chat.trade_restock",
                Component.literal(text).withStyle(ChatFormatting.YELLOW)),
            false);
        mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.2F, 1.8F);
    }
}
