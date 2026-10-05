package com.pockyl.lumen_rigs.fixture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Where haze machines have recently puffed haze, as seen by this game instance. The client uses it to make beams near
 * a running haze machine denser. Only touched from the thread that ticks the level.
 */
public final class HazeTracker {
    /** A machine counts as running for this long after its last puff. */
    private static final long MEMORY_TICKS = 60;
    /** Haze fills this many blocks around a machine. */
    public static final double REACH = 20;

    private static final Map<BlockPos, Long> PUFFS = new HashMap<>();
    private static Level trackedLevel;

    private HazeTracker() {
    }

    public static void puff(Level level, BlockPos machine) {
        if (level != trackedLevel) {
            PUFFS.clear();
            trackedLevel = level;
        }
        PUFFS.put(machine.immutable(), level.getGameTime());
    }

    /** 0 far from running haze machines, up to 1 right next to one (several add up to 2). */
    public static float density(Level level, Vec3 at) {
        if (level != trackedLevel || PUFFS.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        float density = 0;
        Iterator<Map.Entry<BlockPos, Long>> iterator = PUFFS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (now - entry.getValue() > MEMORY_TICKS) {
                iterator.remove();
                continue;
            }
            double distance = entry.getKey().getCenter().distanceTo(at);
            if (distance < REACH) {
                density += (float) (1 - distance / REACH);
            }
        }
        return Math.min(2, density);
    }
}
