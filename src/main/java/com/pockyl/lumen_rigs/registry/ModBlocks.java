package com.pockyl.lumen_rigs.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlock;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.List;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LumenRigs.MOD_ID);

    public static final RegistryObject<FixtureBlock> SPOTLIGHT = fixture(FixtureType.SPOTLIGHT);
    public static final RegistryObject<FixtureBlock> FLOODLIGHT = fixture(FixtureType.FLOODLIGHT);
    public static final RegistryObject<FixtureBlock> SEARCHLIGHT = fixture(FixtureType.SEARCHLIGHT);
    public static final RegistryObject<FixtureBlock> SOFT_PANEL = fixture(FixtureType.SOFT_PANEL);

    public static final List<RegistryObject<FixtureBlock>> FIXTURES = List.of(SPOTLIGHT, FLOODLIGHT, SEARCHLIGHT, SOFT_PANEL);

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static RegistryObject<FixtureBlock> fixture(FixtureType type) {
        return BLOCKS.register(type.getSerializedName(), () -> new FixtureBlock(type, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 6.0F)
                .sound(SoundType.LANTERN)
                .noOcclusion()));
    }
}
