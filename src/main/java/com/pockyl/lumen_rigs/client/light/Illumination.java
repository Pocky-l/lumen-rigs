package com.pockyl.lumen_rigs.client.light;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.List;

/** How much light the fixtures throw onto a point in the air: used to make haze glow inside beams. */
public final class Illumination {
    private static List<FixtureBlockEntity> cached = List.of();
    private static long cachedAt = Long.MIN_VALUE;
    private static ClientLevel cachedLevel;

    private Illumination() {
    }

    /** The fixtures giving light this tick (computed at most once per tick). */
    public static List<FixtureBlockEntity> fixtures() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return List.of();
        }
        if (level != cachedLevel || level.getGameTime() != cachedAt) {
            cached = ClientLighting.activeFixtures(level, minecraft.gameRenderer.getMainCamera().getPosition());
            cachedAt = level.getGameTime();
            cachedLevel = level;
        }
        return cached;
    }

    /**
     * Adds the light (RGB, about 0-1 per fixture) reaching {@code point} to {@code out}.
     *
     * @param occlusion whether to check that nothing blocks the way to the lens (one ray per lit fixture)
     */
    public static void at(ClientLevel level, Vec3 point, boolean occlusion, float[] out) {
        for (FixtureBlockEntity fixture : fixtures()) {
            FixtureSettings settings = fixture.settings();
            Vec3 direction = fixture.headDirection(1.0F);
            Vec3 lens = fixture.lens(direction);
            Vec3 offset = point.subtract(lens);
            double distance = offset.length();
            double range = settings.range();
            if (distance > range || distance < 1.0E-3) {
                continue;
            }
            double cos = offset.dot(direction) / distance;
            double strength;
            if (fixture.type() == FixtureType.SOFT_PANEL) {
                strength = Math.max(0, cos);
            } else {
                double half = Math.toRadians(settings.beam() / 2);
                double angle = Math.acos(Mth.clamp(cos, -1, 1));
                strength = 1 - smoothstep(half * (1 - 0.9 * settings.softness()), half + 1.0E-4, angle);
            }
            if (strength <= 0.001) {
                continue;
            }
            double falloff = 1 - distance / range;
            strength *= falloff * falloff * settings.power() * fixture.effectiveBrightness() / 15.0;
            if (strength <= 0.01) {
                continue;
            }
            if (occlusion && level.clip(new ClipContext(lens, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty())).getType() != HitResult.Type.MISS) {
                continue;
            }
            int color = settings.color();
            out[0] += (float) (strength * (color >> 16 & 0xFF) / 255.0);
            out[1] += (float) (strength * (color >> 8 & 0xFF) / 255.0);
            out[2] += (float) (strength * (color & 0xFF) / 255.0);
        }
    }

    public static double smoothstep(double from, double to, double value) {
        double t = Mth.clamp((value - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
