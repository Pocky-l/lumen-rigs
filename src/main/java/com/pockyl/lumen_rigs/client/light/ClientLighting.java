package com.pockyl.lumen_rigs.client.light;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Picks which fixtures light the world with this mod's own block-light engine. */
public final class ClientLighting {
    private ClientLighting() {
    }

    /** Fixtures that currently give light, nearest to the camera first, at most the configured number. */
    public static List<FixtureBlockEntity> activeFixtures(ClientLevel level, Vec3 camera) {
        List<FixtureBlockEntity> fixtures = new ArrayList<>();
        for (FixtureBlockEntity fixture : FixtureBlockEntity.CLIENT_FIXTURES) {
            if (fixture.getLevel() == level && !fixture.isRemoved() && fixture.effectiveBrightness() > 0) {
                fixtures.add(fixture);
            }
        }
        fixtures.sort(Comparator.comparingDouble(fixture -> fixture.getBlockPos().getCenter().distanceToSqr(camera)));
        int max = Config.maxLights();
        return fixtures.size() > max ? new ArrayList<>(fixtures.subList(0, max)) : fixtures;
    }
}
