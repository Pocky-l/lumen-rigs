package com.pockyl.lumen_rigs.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LumenRigs.MOD_ID);

    public static final RegistryObject<BlockItem> SPOTLIGHT = blockItem(ModBlocks.SPOTLIGHT);
    public static final RegistryObject<BlockItem> FLOODLIGHT = blockItem(ModBlocks.FLOODLIGHT);
    public static final RegistryObject<BlockItem> SEARCHLIGHT = blockItem(ModBlocks.SEARCHLIGHT);
    public static final RegistryObject<BlockItem> SOFT_PANEL = blockItem(ModBlocks.SOFT_PANEL);
    public static final RegistryObject<LightingRemoteItem> LIGHTING_REMOTE = ITEMS.register("lighting_remote",
            () -> new LightingRemoteItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static RegistryObject<BlockItem> blockItem(RegistryObject<? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
