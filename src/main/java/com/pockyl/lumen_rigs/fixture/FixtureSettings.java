package com.pockyl.lumen_rigs.fixture;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * What the player sets on a fixture.
 *
 * @param pan        horizontal aim in degrees (Minecraft yaw: 0 = south, 90 = west)
 * @param tilt       vertical aim in degrees, positive is up
 * @param beam       full beam angle in degrees
 * @param brightness 0-15
 * @param color      RGB of the light
 * @param sweepWidth half the angle a sweeping fixture swings through, in degrees
 * @param sweepSpeed full swings per minute
 */
public record FixtureSettings(float pan, float tilt, float beam, int brightness, int color, AimMode mode, RedstoneMode redstone,
        float sweepWidth, float sweepSpeed) {
    public static final int WARM_WHITE = 0xFFF1DC;
    public static final float MIN_SWEEP_WIDTH = 5;
    public static final float MAX_SWEEP_WIDTH = 90;
    public static final float MIN_SWEEP_SPEED = 1;
    public static final float MAX_SWEEP_SPEED = 30;

    public static final StreamCodec<ByteBuf, FixtureSettings> STREAM_CODEC = StreamCodec.of(
            (buf, settings) -> {
                buf.writeFloat(settings.pan);
                buf.writeFloat(settings.tilt);
                buf.writeFloat(settings.beam);
                ByteBufCodecs.VAR_INT.encode(buf, settings.brightness);
                buf.writeInt(settings.color);
                ByteBufCodecs.VAR_INT.encode(buf, settings.mode.ordinal());
                ByteBufCodecs.VAR_INT.encode(buf, settings.redstone.ordinal());
                buf.writeFloat(settings.sweepWidth);
                buf.writeFloat(settings.sweepSpeed);
            },
            buf -> new FixtureSettings(buf.readFloat(), buf.readFloat(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readInt(), AimMode.byId(ByteBufCodecs.VAR_INT.decode(buf)), RedstoneMode.byId(ByteBufCodecs.VAR_INT.decode(buf)),
                    buf.readFloat(), buf.readFloat()));

    public static FixtureSettings defaults(FixtureType type) {
        float tilt = type == FixtureType.SEARCHLIGHT ? 45 : -30;
        return new FixtureSettings(0, tilt, type.defaultBeam(), 15, WARM_WHITE, AimMode.MANUAL, RedstoneMode.IGNORE, 40, 6);
    }

    /** Brings every value into the range the fixture supports; applied to everything a client sends. */
    public FixtureSettings clamp(FixtureType type) {
        float safePan = Float.isFinite(pan) ? Mth.wrapDegrees(pan) : 0;
        float safeTilt = Float.isFinite(tilt) ? Mth.clamp(tilt, -90, 90) : 0;
        float safeBeam = Float.isFinite(beam) ? Mth.clamp(beam, type.minBeam(), type.maxBeam()) : type.defaultBeam();
        float width = Float.isFinite(sweepWidth) ? Mth.clamp(sweepWidth, MIN_SWEEP_WIDTH, MAX_SWEEP_WIDTH) : 40;
        float speed = Float.isFinite(sweepSpeed) ? Mth.clamp(sweepSpeed, MIN_SWEEP_SPEED, MAX_SWEEP_SPEED) : 6;
        return new FixtureSettings(safePan, safeTilt, safeBeam, Mth.clamp(brightness, 0, 15), color & 0xFFFFFF, mode, redstone,
                width, speed);
    }

    public FixtureSettings withAim(float newPan, float newTilt, AimMode newMode) {
        return new FixtureSettings(newPan, newTilt, beam, brightness, color, newMode, redstone, sweepWidth, sweepSpeed);
    }

    public FixtureSettings withMode(AimMode newMode) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, newMode, redstone, sweepWidth, sweepSpeed);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Pan", pan);
        tag.putFloat("Tilt", tilt);
        tag.putFloat("Beam", beam);
        tag.putInt("Brightness", brightness);
        tag.putInt("Color", color);
        tag.putString("Mode", mode.name());
        tag.putString("Redstone", redstone.name());
        tag.putFloat("SweepWidth", sweepWidth);
        tag.putFloat("SweepSpeed", sweepSpeed);
        return tag;
    }

    public static FixtureSettings load(CompoundTag tag, FixtureType type) {
        FixtureSettings defaults = defaults(type);
        if (tag.isEmpty()) {
            return defaults;
        }
        return new FixtureSettings(tag.getFloat("Pan"), tag.getFloat("Tilt"), tag.getFloat("Beam"), tag.getInt("Brightness"),
                tag.getInt("Color"), parse(AimMode.class, tag.getString("Mode"), AimMode.MANUAL),
                parse(RedstoneMode.class, tag.getString("Redstone"), RedstoneMode.IGNORE),
                tag.contains("SweepWidth") ? tag.getFloat("SweepWidth") : defaults.sweepWidth,
                tag.contains("SweepSpeed") ? tag.getFloat("SweepSpeed") : defaults.sweepSpeed).clamp(type);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
