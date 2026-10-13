package com.pockyl.lumen_rigs.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlock;
import com.pockyl.lumen_rigs.item.FixtureItem;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LumenRigs.MOD_ID);

    public static final DeferredItem<?> SPOTLIGHT = fixture(ModBlocks.SPOTLIGHT);
    public static final DeferredItem<?> FLOODLIGHT = fixture(ModBlocks.FLOODLIGHT);
    public static final DeferredItem<?> SEARCHLIGHT = fixture(ModBlocks.SEARCHLIGHT);
    public static final DeferredItem<?> SOFT_PANEL = fixture(ModBlocks.SOFT_PANEL);
    public static final DeferredItem<LightingRemoteItem> LIGHTING_REMOTE = ITEMS.register("lighting_remote",
            () -> new LightingRemoteItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static DeferredItem<FixtureItem> fixture(DeferredBlock<FixtureBlock> block) {
        return ITEMS.register(block.getId().getPath(), () -> new FixtureItem(block.get(), new Item.Properties()));
    }
}
