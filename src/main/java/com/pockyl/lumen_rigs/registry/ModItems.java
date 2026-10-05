package com.pockyl.lumen_rigs.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LumenRigs.MOD_ID);

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
