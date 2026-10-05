package com.pockyl.lumen_rigs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.lumen_rigs.registry.ModBlockEntities;

/** Only there to tick the haze machine on the client, so it puffs steadily (random block ticks are too sparse). */
public final class HazeMachineBlockEntity extends BlockEntity {
    public HazeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAZE_MACHINE.get(), pos, state);
    }
}
