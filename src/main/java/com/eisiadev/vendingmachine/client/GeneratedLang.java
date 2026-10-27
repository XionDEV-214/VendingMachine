package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.currency.CurrencyDefinition;
import com.eisiadev.vendingmachine.data.DataFiles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = VendingMachine.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GeneratedLang {

    private static final String PACK_ID = "vendingmachine_generated";
    private static final int PACK_FORMAT = 15;

    private GeneratedLang() {}

    public static Path packDir() {
        return FMLPaths.CONFIGDIR.get().resolve("vendingmachine").resolve("generated_resources");
    }

    private static Path langFile(String lang) {
        return packDir().resolve("assets").resolve(VendingMachine.MODID).resolve("lang")
            .resolve(lang.toLowerCase(Locale.ROOT) + ".json");
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path dir = packDir();
        if (!ensurePackMeta(dir)) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Vending Machine (generated)"), true,
                id -> new PathPackResources(id, dir, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP,
                PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    private static boolean ensurePackMeta(Path dir) {
        try {
            Files.createDirectories(dir);
            Path meta = dir.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                JsonObject pack = new JsonObject();
                pack.addProperty("description", "Vending Machine generated translations (editable)");
                pack.addProperty("pack_format", PACK_FORMAT);
                JsonObject root = new JsonObject();
                root.add("pack", pack);
                Files.writeString(meta, DataFiles.GSON.toJson(root), StandardCharsets.UTF_8);
            }
            return true;
        } catch (IOException e) {
            VendingMachine.LOG.error("Could not create generated resource pack at {}", dir, e);
            return false;
        }
    }

    public static void update(Collection<CurrencyDefinition> currencies) {
        if (!ensurePackMeta(packDir())) return;

        Path enFile = langFile("en_us");
        JsonObject en = readOrEmpty(enFile);
        boolean enChanged = false;
        for (CurrencyDefinition def : currencies) {
            if (!def.isTranslationKey() || en.has(def.getName())) continue;
            if (I18n.exists(def.getName())) continue;
            en.addProperty(def.getName(), def.defaultEnglishName());
            enChanged = true;
        }
        if (enChanged) DataFiles.write(enFile, en);

        String current = Minecraft.getInstance().getLanguageManager().getSelected();
        if (current == null || current.equalsIgnoreCase("en_us")) return;
        Path localFile = langFile(current);
        JsonObject local = readOrEmpty(localFile);
        boolean localChanged = false;
        for (var entry : en.entrySet()) {
            if (local.has(entry.getKey())) continue;
            local.add(entry.getKey(), entry.getValue());
            localChanged = true;
        }
        if (localChanged) DataFiles.write(localFile, local);
    }

    private static JsonObject readOrEmpty(Path file) {
        JsonObject json = DataFiles.read(file);
        return json == null ? new JsonObject() : json;
    }
}
