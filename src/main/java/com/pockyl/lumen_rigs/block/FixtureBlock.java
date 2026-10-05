package com.pockyl.lumen_rigs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.network.OpenFixturePayload;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;

import java.util.EnumMap;
import java.util.Map;

/**
 * A light fixture block. It is attached to the face it was placed on ({@link #FACING} points away from that face) and
 * is drawn by the block entity renderer, except the static soft panel. Right-click with an empty hand opens its
 * settings.
 */
public final class FixtureBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final FixtureType type;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public FixtureBlock(FixtureType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        // Floor-standing outline, turned onto every mount face.
        AABB box = type == FixtureType.SOFT_PANEL ? new AABB(1, 0, 1, 15, 2, 15) : new AABB(2, 0, 2, 14, 14, 14);
        for (Direction facing : Direction.values()) {
            shapes.put(facing, rotated(box, facing));
        }
    }

    public FixtureType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /** The soft panel is a normal block model; the other fixtures move, so their renderer draws them. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return type == FixtureType.SOFT_PANEL ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FixtureBlockEntity) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenFixturePayload(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture) {
            fixture.setSignal(level.getBestNeighborSignal(pos));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FixtureBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != ModBlockEntities.FIXTURE.get()) {
            return null;
        }
        BlockEntityTicker<FixtureBlockEntity> ticker = level.isClientSide() ? FixtureBlockEntity::clientTick : FixtureBlockEntity::serverTick;
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> cast = (BlockEntityTicker<T>) ticker;
        return cast;
    }

    /** Turns a floor-standing box (in pixels) onto a mount face. */
    private static VoxelShape rotated(AABB box, Direction facing) {
        double x0 = box.minX / 16;
        double y0 = box.minY / 16;
        double z0 = box.minZ / 16;
        double x1 = box.maxX / 16;
        double y1 = box.maxY / 16;
        double z1 = box.maxZ / 16;
        return switch (facing) {
            case UP -> Shapes.box(x0, y0, z0, x1, y1, z1);
            case DOWN -> Shapes.box(x0, 1 - y1, z0, x1, 1 - y0, z1);
            case NORTH -> Shapes.box(x0, z0, 1 - y1, x1, z1, 1 - y0);
            case SOUTH -> Shapes.box(x0, z0, y0, x1, z1, y1);
            case EAST -> Shapes.box(y0, z0, x0, y1, z1, x1);
            case WEST -> Shapes.box(1 - y1, z0, x0, 1 - y0, z1, x1);
        };
    }
}
