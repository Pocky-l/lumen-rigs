package com.pockyl.lumen_rigs;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue REMOTE_RANGE = BUILDER
            .comment("How far the lighting remote reaches: linked fixtures and aimed blocks, in blocks.")
            .translation("lumen_rigs.configuration.remoteRange")
            .defineInRange("remoteRange", 128, 8, 512);
    private static final ForgeConfigSpec.IntValue FOLLOW_RANGE = BUILDER
            .comment("Fixtures stop following an entity that is further away than this, in blocks.")
            .translation("lumen_rigs.configuration.followRange")
            .defineInRange("followRange", 96, 8, 256);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final ForgeConfigSpec.Builder CLIENT = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue BEAMS = CLIENT
            .comment("Draw visible light beams.")
            .translation("lumen_rigs.configuration.beams")
            .define("beams", true);
    private static final ForgeConfigSpec.DoubleValue BEAM_STRENGTH = CLIENT
            .comment("How visible the beams are.")
            .translation("lumen_rigs.configuration.beamStrength")
            .defineInRange("beamStrength", 1.0, 0.1, 2.0);
    private static final ForgeConfigSpec.BooleanValue LIGHTING = CLIENT
            .comment("Let fixtures light up blocks and mobs. The light is client-side: it does not stop mobs from spawning.")
            .translation("lumen_rigs.configuration.lighting")
            .define("lighting", true);
    private static final ForgeConfigSpec.EnumValue<LightingEngine> LIGHTING_ENGINE = CLIENT
            .comment("Which light engine fixtures use. On Minecraft 1.20.1 both values use the built-in block light: the Veil",
                    "integration (AUTO) is only available for Minecraft 1.21.1. Kept so settings carry over between versions.")
            .translation("lumen_rigs.configuration.lightingEngine")
            .defineEnum("lightingEngine", LightingEngine.AUTO);
    private static final ForgeConfigSpec.DoubleValue COLOR_STRENGTH = CLIENT
            .comment("How strongly colored light tints what it hits.")
            .translation("lumen_rigs.configuration.colorStrength")
            .defineInRange("colorStrength", 1.0, 0.0, 1.5);
    private static final ForgeConfigSpec.IntValue MAX_LIGHTS = CLIENT
            .comment("At most this many fixtures (the nearest ones) light up the world at once. Moving lights re-render the",
                    "chunks they reach, so lower this on weak computers.")
            .translation("lumen_rigs.configuration.maxLights")
            .defineInRange("maxLights", 48, 1, 512);

    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT.build();

    private Config() {
    }

    public enum LightingEngine {
        AUTO,
        BLOCK_LIGHT
    }

    public static int remoteRange() {
        return REMOTE_RANGE.get();
    }

    public static int followRange() {
        return FOLLOW_RANGE.get();
    }

    public static boolean beams() {
        return BEAMS.get();
    }

    public static float beamStrength() {
        return BEAM_STRENGTH.get().floatValue();
    }

    public static boolean lighting() {
        return LIGHTING.get();
    }

    public static float colorStrength() {
        return COLOR_STRENGTH.get().floatValue();
    }

    public static int maxLights() {
        return MAX_LIGHTS.get();
    }
}
