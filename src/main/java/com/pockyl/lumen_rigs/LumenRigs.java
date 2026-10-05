package com.pockyl.lumen_rigs;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import com.pockyl.lumen_rigs.network.ModNetwork;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;
import com.pockyl.lumen_rigs.registry.ModBlocks;
import com.pockyl.lumen_rigs.registry.ModDataComponents;
import com.pockyl.lumen_rigs.registry.ModItems;
import com.pockyl.lumen_rigs.registry.PockyModsTab;

@Mod(LumenRigs.MOD_ID)
public final class LumenRigs {
    public static final String MOD_ID = "lumen_rigs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LumenRigs(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModDataComponents.register(modBus);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.SPOTLIGHT.get()), output -> {
            output.accept(ModItems.SPOTLIGHT.get());
            output.accept(ModItems.FLOODLIGHT.get());
            output.accept(ModItems.SEARCHLIGHT.get());
            output.accept(ModItems.SOFT_PANEL.get());
            output.accept(ModItems.LIGHTING_REMOTE.get());
        });
        modBus.addListener(LumenRigs::addToVanillaTabs);
        modBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.SPOTLIGHT.get());
            event.accept(ModItems.FLOODLIGHT.get());
            event.accept(ModItems.SEARCHLIGHT.get());
            event.accept(ModItems.SOFT_PANEL.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.LIGHTING_REMOTE.get());
        }
    }
}
