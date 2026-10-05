package com.pockyl.lumen_rigs.client.light;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * How much the air scatters light around a point: rain, thunderstorms and water make beams show up more, like in real
 * life.
 */
public final class Atmosphere {
    private Atmosphere() {
    }

    /** 1 in clear air, more in rain, storms and water. */
    public static float scattering(Level level, Vec3 at) {
        float factor = 1.0F;
        BlockPos pos = BlockPos.containing(at);
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            factor += 1.5F;
        } else if (level.canSeeSky(pos)) {
            factor += level.getRainLevel(1.0F) * 0.8F + level.getThunderLevel(1.0F) * 0.7F;
        }
        return factor;
    }
}
