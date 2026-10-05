package com.pockyl.lumen_rigs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import com.pockyl.lumen_rigs.fixture.HazeTracker;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;
import com.pockyl.lumen_rigs.registry.ModParticles;

/**
 * Fills the air around it with a light haze, so light beams show up in it, like a stage haze machine. Right-click
 * switches it on and off; a redstone signal also runs it.
 */
public final class HazeMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HazeMachineBlock> CODEC = simpleCodec(HazeMachineBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 9, 14);

    public HazeMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, true).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, POWERED);
    }

    public static boolean running(BlockState state) {
        return state.getValue(ENABLED) || state.getValue(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockState toggled = state.cycle(ENABLED);
            level.setBlock(pos, toggled, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, toggled.getValue(ENABLED) ? 0.9F : 0.7F);
            player.displayClientMessage(Component.translatable("block.lumen_rigs.haze_machine." + (toggled.getValue(ENABLED) ? "on" : "off")),
                    true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (!level.isClientSide() && powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HazeMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || type != ModBlockEntities.HAZE_MACHINE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (running(tickState) && tickLevel.getGameTime() % 3 == 0) {
                puff(tickState, tickLevel, pos, tickLevel.getRandom());
            }
        };
    }

    /** Client: puffs haze out of the nozzle. */
    private static void puff(BlockState state, Level level, BlockPos pos, RandomSource random) {
        HazeTracker.puff(level, pos);
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.55;
        double y = pos.getY() + 0.45;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.55;
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            double speed = 0.06 + random.nextDouble() * 0.06;
            level.addParticle(ModParticles.HAZE.get(), x, y, z, facing.getStepX() * speed + (random.nextDouble() - 0.5) * 0.03,
                    0.004 + random.nextDouble() * 0.01, facing.getStepZ() * speed + (random.nextDouble() - 0.5) * 0.03);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.05F, 1.6F + random.nextFloat() * 0.3F, false);
        }
    }
}
