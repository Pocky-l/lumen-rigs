package com.pockyl.lumen_rigs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.block.HazeMachineBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LumenRigs.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FixtureBlockEntity>> FIXTURE = BLOCK_ENTITIES.register(
            "fixture", () -> BlockEntityType.Builder.of(FixtureBlockEntity::new,
                    ModBlocks.FIXTURES.stream().map(holder -> (Block) holder.get()).toArray(Block[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HazeMachineBlockEntity>> HAZE_MACHINE = BLOCK_ENTITIES.register(
            "haze_machine", () -> BlockEntityType.Builder.of(HazeMachineBlockEntity::new, ModBlocks.HAZE_MACHINE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
