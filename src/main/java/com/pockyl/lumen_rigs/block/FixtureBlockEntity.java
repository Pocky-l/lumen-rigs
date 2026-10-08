package com.pockyl.lumen_rigs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.fixture.Aim;
import com.pockyl.lumen_rigs.fixture.AimMode;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

/**
 * A light fixture: its settings, what it aims at and, on the client, where its motorized head currently points.
 * The server owns the settings and targets; the client turns the head smoothly towards the aim every tick and the
 * renderer and light engine read that.
 */
public final class FixtureBlockEntity extends BlockEntity {
    /** Client only: every fixture loaded on the client, for the renderer and the light engine. */
    public static final Set<FixtureBlockEntity> CLIENT_FIXTURES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final int FOLLOW_REFRESH_TICKS = 10;

    private FixtureSettings settings;
    @Nullable
    private Vec3 targetPoint;
    @Nullable
    private UUID followUuid;
    /** Entity id of the followed entity as the clients know it, -1 when none is in range. */
    private int followId = -1;
    private int signal;

    // Client-side head motion.
    @Nullable
    private Vec3 headDirection;
    @Nullable
    private Vec3 headDirectionO;

    public FixtureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIXTURE.get(), pos, state);
        this.settings = FixtureSettings.defaults(type());
    }

    public FixtureType type() {
        return getBlockState().getBlock() instanceof FixtureBlock block ? block.type() : FixtureType.SPOTLIGHT;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(FixtureBlock.FACING) ? state.getValue(FixtureBlock.FACING) : Direction.UP;
    }

    public FixtureSettings settings() {
        return settings;
    }

    @Nullable
    public Vec3 targetPoint() {
        return targetPoint;
    }

    public int followId() {
        return followId;
    }

    /** Brightness after redstone, 0-15. */
    public int effectiveBrightness() {
        return settings.redstone().apply(settings.brightness(), signal);
    }

    // ------------------------------------------------------------------------------------------------
    // Changing (server)
    // ------------------------------------------------------------------------------------------------

    public void applySettings(FixtureSettings newSettings) {
        settings = newSettings.clamp(type());
        if (settings.mode() != AimMode.FOLLOW) {
            followUuid = null;
            followId = -1;
        }
        changed();
    }

    /** Points the fixture at a spot in the world and keeps it there. */
    public void aimAt(Vec3 point) {
        targetPoint = point;
        Vec3 direction = point.subtract(pivot()).normalize();
        settings = settings.withAim(Aim.pan(direction), Aim.tilt(direction), AimMode.POINT).clamp(type());
        followUuid = null;
        followId = -1;
        changed();
    }

    /** Points the fixture in a direction, e.g. into the sky. */
    public void aimAlong(Vec3 direction) {
        settings = settings.withAim(Aim.pan(direction), Aim.tilt(direction), AimMode.MANUAL).clamp(type());
        followUuid = null;
        followId = -1;
        changed();
    }

    /** Makes the fixture track an entity. */
    public void follow(Entity entity) {
        followUuid = entity.getUUID();
        followId = entity.getId();
        settings = settings.withMode(AimMode.FOLLOW);
        changed();
    }

    void setSignal(int newSignal) {
        if (newSignal != signal) {
            signal = newSignal;
            changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Server: resolves the followed entity (or the nearest player) to an id the clients know. */
    static void serverTick(Level level, BlockPos pos, BlockState state, FixtureBlockEntity fixture) {
        if (fixture.settings.mode() != AimMode.FOLLOW || level.getGameTime() % FOLLOW_REFRESH_TICKS != 0
                || !(level instanceof ServerLevel server)) {
            return;
        }
        double range = Config.followRange();
        Vec3 pivot = fixture.pivot();
        Entity target = fixture.followUuid != null ? server.getEntity(fixture.followUuid) : null;
        if (target == null && fixture.followUuid == null) {
            Player nearest = level.getNearestPlayer(pivot.x, pivot.y, pivot.z, range, entity -> !entity.isSpectator());
            target = nearest;
        }
        int id = target != null && target.isAlive() && target.position().distanceTo(pivot) <= range ? target.getId() : -1;
        if (id != fixture.followId) {
            fixture.followId = id;
            fixture.changed();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Aim
    // ------------------------------------------------------------------------------------------------

    /** Where the head turns around, in world coordinates. */
    public Vec3 pivot() {
        return Aim.toWorld(facing(), 0.5, type().pivotY(), 0.5, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    /** Straight out of the mount face: where a soft panel shines and where an unaimed head rests. */
    public Vec3 mountNormal() {
        Direction facing = facing();
        return new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    /** The direction the fixture wants to point at {@code time} (game ticks plus partial tick). */
    public Vec3 desiredDirection(double time) {
        if (!type().aimable()) {
            return mountNormal();
        }
        Vec3 manual = Aim.direction(settings.pan(), settings.tilt());
        return switch (settings.mode()) {
            case MANUAL -> manual;
            case POINT -> targetPoint != null ? towards(targetPoint, manual) : manual;
            case FOLLOW -> {
                Entity entity = followId >= 0 && level != null ? level.getEntity(followId) : null;
                yield entity != null ? towards(entity.getBoundingBox().getCenter(), manual) : current(manual);
            }
            case SWEEP -> {
                double phase = time / 1200.0 * settings.sweepSpeed() * Mth.TWO_PI;
                yield Aim.direction(settings.pan() + (float) (Math.sin(phase) * settings.sweepWidth()), settings.tilt());
            }
        };
    }

    private Vec3 towards(Vec3 point, Vec3 fallback) {
        Vec3 offset = point.subtract(pivot());
        return offset.lengthSqr() < 1.0E-4 ? fallback : offset.normalize();
    }

    private Vec3 current(Vec3 fallback) {
        return headDirection != null ? headDirection : fallback;
    }

    /** Client: where the head points, interpolated between ticks. */
    public Vec3 headDirection(float partialTick) {
        if (headDirection == null) {
            return desiredDirection(level != null ? level.getGameTime() : 0);
        }
        if (headDirectionO == null || partialTick >= 1) {
            return headDirection;
        }
        Vec3 blended = headDirectionO.lerp(headDirection, partialTick);
        return blended.lengthSqr() < 1.0E-6 ? headDirection : blended.normalize();
    }

    /** Where the beam leaves the fixture, in world coordinates. */
    public Vec3 lens(Vec3 direction) {
        return pivot().add(direction.scale(type().lensOffset()));
    }

    /** Client: turns the motorized head towards the aim. */
    static void clientTick(Level level, BlockPos pos, BlockState state, FixtureBlockEntity fixture) {
        Vec3 target = fixture.desiredDirection(level.getGameTime());
        if (fixture.headDirection == null) {
            fixture.headDirection = target;
        }
        fixture.headDirectionO = fixture.headDirection;
        fixture.headDirection = Aim.turnTowards(fixture.headDirection, target, fixture.type().turnSpeed());
    }

    // ------------------------------------------------------------------------------------------------
    // Lifecycle, saving, syncing
    // ------------------------------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CLIENT_FIXTURES.add(this);
        } else if (level != null && getBlockState().getBlock() instanceof FixtureBlock) {
            signal = level.getBestNeighborSignal(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_FIXTURES.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_FIXTURES.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Settings", settings.save());
        if (targetPoint != null) {
            tag.putDouble("TargetX", targetPoint.x);
            tag.putDouble("TargetY", targetPoint.y);
            tag.putDouble("TargetZ", targetPoint.z);
        }
        if (followUuid != null) {
            tag.putUUID("Follow", followUuid);
        }
        tag.putInt("Signal", signal);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        settings = FixtureSettings.load(tag.getCompound("Settings"), type());
        targetPoint = tag.contains("TargetX") ? new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ")) : null;
        followUuid = tag.hasUUID("Follow") ? tag.getUUID("Follow") : null;
        followId = tag.contains("FollowId") ? tag.getInt("FollowId") : -1;
        signal = tag.getInt("Signal");
        if (level != null && level.isClientSide() && type() == FixtureType.SOFT_PANEL) {
            // The panel's color is baked into the chunk mesh.
            level.setBlocksDirty(worldPosition, getBlockState(), getBlockState());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putInt("FollowId", followId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The beam reaches far beyond the block. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(settings.range());
    }
}
