package com.pockyl.lumen_rigs;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.pockyl.lumen_rigs.registry.ModBlocks;
import com.pockyl.lumen_rigs.registry.ModItems;
import com.pockyl.lumen_rigs.registry.PockyModsTab;

@Mod(LumenRigs.MOD_ID)
public final class LumenRigs {
    public static final String MOD_ID = "lumen_rigs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LumenRigs(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        // Add this mod's items to the shared "Pocky Mods" creative tab, e.g. output.accept(ModItems.FOO).
        PockyModsTab.register(modBus, () -> new ItemStack(Items.SLIME_BALL), output -> {
        });

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
