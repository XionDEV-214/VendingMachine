package com.eisiadev.vendingmachine.command;

import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.data.DataFiles;
import com.eisiadev.vendingmachine.network.SyncDataPacket;
import com.eisiadev.vendingmachine.storage.PlayerVendingData;
import com.eisiadev.vendingmachine.storage.VMSavedData;
import com.eisiadev.vendingmachine.storage.Wallet;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class VendingCommand {

    private static final DynamicCommandExceptionType UNKNOWN_CURRENCY = new DynamicCommandExceptionType(
        id -> Component.translatable("vendingmachine.command.unknown_currency", id));

    private VendingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vending")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("players", EntityArgument.players())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(currencySuggestions(), b))
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                            .executes(ctx -> modify(ctx, Op.SET, LongArgumentType.getLong(ctx, "amount")))))))
            .then(Commands.literal("add")
                .then(Commands.argument("players", EntityArgument.players())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(currencySuggestions(), b))
                        .then(Commands.argument("amount", LongArgumentType.longArg())
                            .executes(ctx -> modify(ctx, Op.ADD, LongArgumentType.getLong(ctx, "amount")))))))
            .then(Commands.literal("reset")
                .then(Commands.argument("players", EntityArgument.players())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(currencySuggestions(), b))
                        .executes(ctx -> modify(ctx, Op.SET, 0)))))
            .then(Commands.literal("resettrades")
                .then(Commands.argument("players", EntityArgument.players())
                    .executes(VendingCommand::resetTrades)))
            .then(Commands.literal("reload")
                .executes(VendingCommand::reload)));
    }

    private enum Op {
        SET,
        ADD
    }

    private static List<String> currencySuggestions() {
        List<String> list = new ArrayList<>(CurrencyRegistry.server().ids());
        list.add("all");
        return list;
    }

    private static int modify(CommandContext<CommandSourceStack> ctx, Op op, long amount) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String currency = StringArgumentType.getString(ctx, "currency");
        List<String> targets;
        if (currency.equalsIgnoreCase("all")) {
            targets = CurrencyRegistry.server().ids();
        } else {
            if (CurrencyRegistry.server().get(currency) == null) throw UNKNOWN_CURRENCY.create(currency);
            targets = List.of(currency);
        }
        VMSavedData data = VMSavedData.get(ctx.getSource().getServer());
        for (ServerPlayer player : players) {
            Wallet wallet = data.getPlayer(player.getUUID()).wallet;
            for (String c : targets) {
                if (op == Op.SET) wallet.set(c, amount);
                else wallet.set(c, Math.max(0, Wallet.saturatedAdd(wallet.get(c), amount)));
            }
        }
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("vendingmachine.command.wallet_updated",
            players.size(), currency), true);
        return players.size();
    }

    private static int resetTrades(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        VMSavedData data = VMSavedData.get(ctx.getSource().getServer());
        for (ServerPlayer player : players) {
            PlayerVendingData pd = data.getPlayer(player.getUUID());
            pd.resetHistory();
        }
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("vendingmachine.command.trades_reset",
            players.size()), true);
        return players.size();
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        DataFiles.loadAll();
        SyncDataPacket.send(null);
        ctx.getSource().sendSuccess(() -> Component.translatable("vendingmachine.command.reloaded",
            CurrencyRegistry.server().all().size(),
            com.eisiadev.vendingmachine.trade.TradeDatabase.server().getTradeGroups().size()), true);
        return 1;
    }
}
