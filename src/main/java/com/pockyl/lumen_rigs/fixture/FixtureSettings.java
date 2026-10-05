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
 * @param brightness 0-15, the dimmer (redstone can scale it further)
 * @param color      RGB of the light
 * @param sweepWidth half the angle a sweeping fixture swings through, in degrees
 * @param sweepSpeed full swings per minute
 * @param power      strength of the lamp, 1 = normal
 * @param softness   edge of the light: 0 hard (profile spot), 1 fully diffuse
 * @param haze       how visible the beam is in the air, 1 = normal
 * @param range      how far the light reaches, in blocks
 */
public record FixtureSettings(float pan, float tilt, float beam, int brightness, int color, AimMode mode, RedstoneMode redstone,
        float sweepWidth, float sweepSpeed, float power, float softness, float haze, float range) {
    public static final int WARM_WHITE = 0xFFF1DC;
    public static final float MIN_SWEEP_WIDTH = 5;
    public static final float MAX_SWEEP_WIDTH = 90;
    public static final float MIN_SWEEP_SPEED = 1;
    public static final float MAX_SWEEP_SPEED = 30;
    public static final float MIN_POWER = 0.25F;
    public static final float MAX_POWER = 4.0F;
    public static final float MAX_HAZE = 3.0F;

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
                buf.writeFloat(settings.power);
                buf.writeFloat(settings.softness);
                buf.writeFloat(settings.haze);
                buf.writeFloat(settings.range);
            },
            buf -> new FixtureSettings(buf.readFloat(), buf.readFloat(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readInt(), AimMode.byId(ByteBufCodecs.VAR_INT.decode(buf)), RedstoneMode.byId(ByteBufCodecs.VAR_INT.decode(buf)),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    public static FixtureSettings defaults(FixtureType type) {
        float tilt = type == FixtureType.SEARCHLIGHT ? 45 : -30;
        return new FixtureSettings(0, tilt, type.defaultBeam(), 15, WARM_WHITE, AimMode.MANUAL, RedstoneMode.IGNORE, 40, 6,
                1.0F, type.defaultSoftness(), 1.0F, type.range());
    }

    /** Brings every value into the range the fixture supports; applied to everything a client sends. */
    public FixtureSettings clamp(FixtureType type) {
        FixtureSettings defaults = defaults(type);
        return new FixtureSettings(
                Float.isFinite(pan) ? Mth.wrapDegrees(pan) : 0,
                clamp(tilt, -90, 90, 0),
                clamp(beam, type.minBeam(), type.maxBeam(), type.defaultBeam()),
                Mth.clamp(brightness, 0, 15),
                color & 0xFFFFFF,
                mode,
                redstone,
                clamp(sweepWidth, MIN_SWEEP_WIDTH, MAX_SWEEP_WIDTH, defaults.sweepWidth),
                clamp(sweepSpeed, MIN_SWEEP_SPEED, MAX_SWEEP_SPEED, defaults.sweepSpeed),
                clamp(power, MIN_POWER, MAX_POWER, 1),
                clamp(softness, 0, 1, type.defaultSoftness()),
                clamp(haze, 0, MAX_HAZE, 1),
                clamp(range, type.minRange(), type.maxRange(), type.range()));
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Mth.clamp(value, min, max) : fallback;
    }

    public FixtureSettings withAim(float newPan, float newTilt, AimMode newMode) {
        return new FixtureSettings(newPan, newTilt, beam, brightness, color, newMode, redstone, sweepWidth, sweepSpeed, power, softness, haze,
                range);
    }

    public FixtureSettings withMode(AimMode newMode) {
        return withAim(pan, tilt, newMode);
    }

    public FixtureSettings withRedstone(RedstoneMode newRedstone) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, newRedstone, sweepWidth, sweepSpeed, power, softness, haze, range);
    }

    public FixtureSettings withBeam(float newBeam) {
        return new FixtureSettings(pan, tilt, newBeam, brightness, color, mode, redstone, sweepWidth, sweepSpeed, power, softness, haze, range);
    }

    public FixtureSettings withBrightness(int newBrightness) {
        return new FixtureSettings(pan, tilt, beam, newBrightness, color, mode, redstone, sweepWidth, sweepSpeed, power, softness, haze, range);
    }

    public FixtureSettings withColor(int newColor) {
        return new FixtureSettings(pan, tilt, beam, brightness, newColor, mode, redstone, sweepWidth, sweepSpeed, power, softness, haze, range);
    }

    public FixtureSettings withSweep(float newWidth, float newSpeed) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, redstone, newWidth, newSpeed, power, softness, haze, range);
    }

    public FixtureSettings withPower(float newPower) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, redstone, sweepWidth, sweepSpeed, newPower, softness, haze, range);
    }

    public FixtureSettings withSoftness(float newSoftness) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, redstone, sweepWidth, sweepSpeed, power, newSoftness, haze, range);
    }

    public FixtureSettings withHaze(float newHaze) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, redstone, sweepWidth, sweepSpeed, power, softness, newHaze, range);
    }

    public FixtureSettings withRange(float newRange) {
        return new FixtureSettings(pan, tilt, beam, brightness, color, mode, redstone, sweepWidth, sweepSpeed, power, softness, haze, newRange);
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
        tag.putFloat("Power", power);
        tag.putFloat("Softness", softness);
        tag.putFloat("Haze", haze);
        tag.putFloat("Range", range);
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
                orDefault(tag, "SweepWidth", defaults.sweepWidth), orDefault(tag, "SweepSpeed", defaults.sweepSpeed),
                orDefault(tag, "Power", defaults.power), orDefault(tag, "Softness", defaults.softness),
                orDefault(tag, "Haze", defaults.haze), orDefault(tag, "Range", defaults.range)).clamp(type);
    }

    private static float orDefault(CompoundTag tag, String key, float fallback) {
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
