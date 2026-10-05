package com.pockyl.lumen_rigs.client.light;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks how fixtures light the world: with <a href="https://www.curseforge.com/minecraft/mc-mods/veil-lib">Veil</a>
 * installed, its deferred lights (per pixel, shadows, volumetric beams); otherwise this mod's own block-light engine.
 */
public final class ClientLighting {
    public static final String VEIL = "veil";
    private static final boolean VEIL_LOADED = ModList.get().isLoaded(VEIL);
    private static boolean veilFailed;

    private ClientLighting() {
    }

    public static boolean veilLoaded() {
        return VEIL_LOADED;
    }

    /** Whether Veil draws the light (and the volumetric beams) instead of the block-light engine. */
    public static boolean useVeil() {
        return VEIL_LOADED && !veilFailed && Config.lighting() && Config.lightingEngine() == Config.LightingEngine.AUTO;
    }

    /** Called when Veil's lights fail at runtime: the block-light engine takes over for the rest of the session. */
    public static void disableVeil() {
        veilFailed = true;
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
