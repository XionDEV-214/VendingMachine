package com.eisiadev.vendingmachine.data;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.currency.CurrencyRegistry;
import com.eisiadev.vendingmachine.trade.TradeDatabase;

import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DataFiles {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String CURRENCIES_FILE = "currencies.json";

    public static final String TRADE_DB_FILE = "tradeDatabase.json";

    public static final String TRADE_DB_DIR = "tradeDatabase";

    private DataFiles() {}

    public static Path dataDir() {
        return FMLPaths.GAMEDIR.get().resolve(VMConfig.DATA_DIR.get());
    }

    public static void loadAll() {
        Path dir = dataDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            VendingMachine.LOG.error("Could not create {}", dir, e);
        }

        Path currencies = dir.resolve(CURRENCIES_FILE);
        if (!Files.exists(currencies)) writeResource("/defaults/currencies.json", currencies);
        JsonObject currencyJson = read(currencies);
        CurrencyRegistry.server().readJson(currencyJson == null ? new JsonObject() : currencyJson);

        loadTradeDatabase(dir);
    }

    private static void loadTradeDatabase(Path dir) {
        Path tradeDir = dir.resolve(TRADE_DB_DIR);
        prepareTradeDir(dir, tradeDir);

        TradeDatabase db = TradeDatabase.server();
        db.clear();
        boolean force = VMConfig.FORCE_REWRITE_DATABASE.get();
        List<Path> files = findTradeFiles(tradeDir);
        for (Path file : files) {
            JsonObject json = read(file);
            if (json == null) continue;
            String source = tradeDir.relativize(file).toString().replace('\\', '/');
            boolean generated = db.addJson(json, source);
            if (generated || force) {
                backup(file, dir.resolve("backup").resolve(TRADE_DB_DIR).resolve(tradeDir.relativize(file)));
                write(file, json);
            }
        }
        VendingMachine.LOG.info("Scanned {} trade database file(s) in {}", files.size(), tradeDir);
        db.logSummary();
    }

    private static void prepareTradeDir(Path dir, Path tradeDir) {
        if (Files.isDirectory(tradeDir)) return;
        try {
            Files.createDirectories(tradeDir);
        } catch (IOException e) {
            VendingMachine.LOG.error("Could not create {}", tradeDir, e);
            return;
        }
        Path legacy = dir.resolve(TRADE_DB_FILE);
        if (Files.exists(legacy)) {
            try {
                Files.move(legacy, tradeDir.resolve(TRADE_DB_FILE));
                VendingMachine.LOG.info("Moved {} into {}", legacy, tradeDir);
            } catch (IOException e) {
                VendingMachine.LOG.error("Could not move {} into {}", legacy, tradeDir, e);
            }
        } else {
            writeResource("/defaults/tradeDatabase.json", tradeDir.resolve("default.json"));
        }
    }

    private static List<Path> findTradeFiles(Path tradeDir) {
        if (!Files.isDirectory(tradeDir)) return List.of();
        try (Stream<Path> walk = Files.walk(tradeDir)) {
            return walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);

                    return name.endsWith(".json") && !name.startsWith("malformed_");
                })
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            VendingMachine.LOG.error("Could not scan {}", tradeDir, e);
            return List.of();
        }
    }

    public static JsonObject read(Path file) {
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            VendingMachine.LOG.error("Failed to read {}: {}", file, e.getMessage());
            Path broken = file.resolveSibling("malformed_" + file.getFileName());
            try {
                Files.copy(file, broken, StandardCopyOption.REPLACE_EXISTING);
                VendingMachine.LOG.error("A copy was saved as {}", broken);
            } catch (IOException ignored) {}
            return null;
        }
    }

    public static void write(Path file, JsonObject json) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(json, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            VendingMachine.LOG.error("Failed to write {}", file, e);
        }
    }

    private static void backup(Path file, Path target) {
        if (!Files.exists(file)) return;
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            VendingMachine.LOG.warn("Could not back up {}", file, e);
        }
    }

    private static void writeResource(String resource, Path target) {
        try (var in = DataFiles.class.getResourceAsStream(resource)) {
            if (in == null) return;
            Files.copy(in, target);
            VendingMachine.LOG.info("Created example {}", target);
        } catch (IOException e) {
            VendingMachine.LOG.error("Could not create {}", target, e);
        }
    }
}
