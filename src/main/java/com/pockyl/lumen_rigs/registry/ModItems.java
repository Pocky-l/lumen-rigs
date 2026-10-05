package com.pockyl.lumen_rigs.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LumenRigs.MOD_ID);

    public static final DeferredItem<?> SPOTLIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.SPOTLIGHT);
    public static final DeferredItem<?> FLOODLIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.FLOODLIGHT);
    public static final DeferredItem<?> SEARCHLIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.SEARCHLIGHT);
    public static final DeferredItem<?> SOFT_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.SOFT_PANEL);
    public static final DeferredItem<?> HAZE_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.HAZE_MACHINE);
    public static final DeferredItem<LightingRemoteItem> LIGHTING_REMOTE = ITEMS.register("lighting_remote",
            () -> new LightingRemoteItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
