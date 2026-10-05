package com.pockyl.lumen_rigs.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlock;
import com.pockyl.lumen_rigs.block.HazeMachineBlock;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.List;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LumenRigs.MOD_ID);

    public static final DeferredBlock<FixtureBlock> SPOTLIGHT = fixture(FixtureType.SPOTLIGHT);
    public static final DeferredBlock<FixtureBlock> FLOODLIGHT = fixture(FixtureType.FLOODLIGHT);
    public static final DeferredBlock<FixtureBlock> SEARCHLIGHT = fixture(FixtureType.SEARCHLIGHT);
    public static final DeferredBlock<FixtureBlock> SOFT_PANEL = fixture(FixtureType.SOFT_PANEL);

    public static final DeferredBlock<HazeMachineBlock> HAZE_MACHINE = BLOCKS.register("haze_machine",
            () -> new HazeMachineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final List<DeferredBlock<FixtureBlock>> FIXTURES = List.of(SPOTLIGHT, FLOODLIGHT, SEARCHLIGHT, SOFT_PANEL);

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static DeferredBlock<FixtureBlock> fixture(FixtureType type) {
        return BLOCKS.register(type.getSerializedName(), () -> new FixtureBlock(type, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 6.0F)
                .sound(SoundType.LANTERN)
                .noOcclusion()));
    }
}
