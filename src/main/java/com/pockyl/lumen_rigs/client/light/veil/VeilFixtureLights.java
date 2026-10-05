package com.pockyl.lumen_rigs.client.light.veil;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.AreaLightData;
import foundry.veil.api.client.render.light.data.LightData;
import foundry.veil.api.client.render.light.data.SpotLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.client.light.Atmosphere;
import com.pockyl.lumen_rigs.client.light.ClientLighting;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fixtures as <a href="https://www.curseforge.com/minecraft/mc-mods/veil-lib">Veil</a> deferred lights: per-pixel
 * spot and area lights with block shadows and light scattering in the air (volumetric beams). Only loaded when Veil is
 * installed; every frame the lights follow the fixtures' heads.
 */
public final class VeilFixtureLights {
    private static final Map<FixtureBlockEntity, LightRenderHandle<? extends LightData>> HANDLES = new IdentityHashMap<>();
    private static ClientLevel lastLevel;
    private static boolean failed;

    private VeilFixtureLights() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VeilFixtureLights::onRenderLevel);
        LumenRigs.LOGGER.info("Veil found: fixtures use its deferred lighting");
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        try {
            update(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        } catch (RuntimeException | LinkageError e) {
            if (!failed) {
                failed = true;
                LumenRigs.LOGGER.error("Veil lights failed; falling back to block light", e);
                ClientLighting.disableVeil();
            }
            freeAll();
        }
    }

    private static void update(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            freeAll();
            lastLevel = level;
        }
        if (level == null || !ClientLighting.useVeil()) {
            freeAll();
            return;
        }
        LightRenderer renderer = VeilRenderSystem.renderer().getLightRenderer();
        List<FixtureBlockEntity> active = ClientLighting.activeFixtures(level, minecraft.gameRenderer.getMainCamera().getPosition());
        Set<FixtureBlockEntity> keep = new HashSet<>(active);
        HANDLES.entrySet().removeIf(entry -> {
            if (!keep.contains(entry.getKey()) || !entry.getValue().isValid()) {
                entry.getValue().free();
                return true;
            }
            return false;
        });
        for (FixtureBlockEntity fixture : active) {
            LightRenderHandle<? extends LightData> handle = HANDLES.get(fixture);
            if (handle == null) {
                handle = renderer.addLight(fixture.type() == FixtureType.SOFT_PANEL ? new AreaLightData() : new SpotLightData());
                HANDLES.put(fixture, handle);
            }
            if (handle.getLightData() instanceof SpotLightData spot) {
                updateSpot(spot, fixture, partialTick);
            } else if (handle.getLightData() instanceof AreaLightData area) {
                updatePanel(area, fixture);
            }
        }
    }

    private static void updateSpot(SpotLightData spot, FixtureBlockEntity fixture, float partialTick) {
        FixtureType type = fixture.type();
        FixtureSettings settings = fixture.settings();
        Vec3 direction = fixture.headDirection(partialTick);
        Vec3 lens = fixture.lens(direction);
        float half = (float) Math.toRadians(settings.beam() / 2);
        spot.getPositionMutable().set(lens.x, lens.y, lens.z);
        orient(spot.getOrientationMutable(), direction);
        spot.setSize(half);
        // Softness 0 is a hard-edged profile spot, 1 fades over almost the whole cone.
        spot.setAngle(half * (0.05F + 0.9F * settings.softness()));
        spot.setDistance(settings.range());
        spot.setOcclusionEnabled(true);
        float air = Atmosphere.scattering(fixture.getLevel(), lens);
        spot.setInscatteringStrength(Config.beams() ? scattering(type) * settings.haze() * air * Config.beamStrength() : 0);
        spot.setColor(settings.color());
        spot.setBrightness(fixture.effectiveBrightness() / 15.0F * settings.power() * intensity(type));
        spot.markDirty();
    }

    private static void updatePanel(AreaLightData area, FixtureBlockEntity fixture) {
        Vec3 normal = fixture.mountNormal();
        Vec3 face = fixture.pivot().add(normal.scale(0.12));
        area.getPositionMutable().set(face.x, face.y, face.z);
        orient(area.getOrientationMutable(), normal);
        area.setSize(0.42, 0.42);
        area.setAngle((float) Math.toRadians(80));
        area.setDistance(fixture.settings().range());
        area.setOcclusionEnabled(true);
        area.setInscatteringStrength(0);
        area.setColor(fixture.settings().color());
        area.setBrightness(fixture.effectiveBrightness() / 15.0F * fixture.settings().power() * intensity(FixtureType.SOFT_PANEL));
        area.markDirty();
    }

    /** Veil lights shine along their local +Z; this is the rotation that takes {@code direction} there. */
    private static void orient(Quaternionf orientation, Vec3 direction) {
        Vector3f forward = new Vector3f((float) -direction.x, (float) -direction.y, (float) -direction.z);
        Vector3f up = Math.abs(direction.y) > 0.99 ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);
        orientation.identity().lookAlong(forward, up);
    }

    private static float intensity(FixtureType type) {
        return switch (type) {
            case SPOTLIGHT -> 1.8F;
            case FLOODLIGHT -> 1.3F;
            case SEARCHLIGHT -> 2.6F;
            case SOFT_PANEL -> 1.1F;
        };
    }

    private static float scattering(FixtureType type) {
        return switch (type) {
            case SPOTLIGHT -> 0.6F;
            case FLOODLIGHT -> 0.25F;
            case SEARCHLIGHT -> 1.0F;
            case SOFT_PANEL -> 0;
        };
    }

    private static void freeAll() {
        for (LightRenderHandle<? extends LightData> handle : HANDLES.values()) {
            handle.free();
        }
        HANDLES.clear();
    }
}
