package com.pockyl.lumen_rigs.registry;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;

import java.util.List;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LumenRigs.MOD_ID);

    /** The fixtures a lighting remote controls. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GlobalPos>>> LINKS = COMPONENTS.registerComponentType(
            "links", builder -> builder.persistent(GlobalPos.CODEC.listOf())
                    .networkSynchronized(GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
