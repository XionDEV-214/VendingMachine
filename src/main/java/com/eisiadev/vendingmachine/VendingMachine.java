package com.eisiadev.vendingmachine;

import com.eisiadev.vendingmachine.gui.VendingUIFactory;
import com.eisiadev.vendingmachine.network.VMNetwork;
import com.eisiadev.vendingmachine.registry.VMBlocks;
import com.eisiadev.vendingmachine.registry.VMCreativeTab;
import com.eisiadev.vendingmachine.registry.VMItems;
import com.eisiadev.vendingmachine.registry.VMMachines;
import com.eisiadev.vendingmachine.registry.VMSounds;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VendingMachine.MODID)
public class VendingMachine {

    public static final String MODID = "vendingmachine";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MODID);

    public VendingMachine() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VMConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VMConfig.CLIENT_SPEC);

        VMBlocks.BLOCKS.register(modBus);
        VMItems.ITEMS.register(modBus);
        VMSounds.SOUNDS.register(modBus);
        VMCreativeTab.TABS.register(modBus);

        REGISTRATE.registerRegistrate();
        modBus.addGenericListener(MachineDefinition.class, this::registerMachines);

        modBus.addListener(this::commonSetup);
    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        VMMachines.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        VMNetwork.register();
        event.enqueueWork(VendingUIFactory::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
