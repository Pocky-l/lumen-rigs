package com.pockyl.lumen_rigs;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue REMOTE_RANGE = BUILDER
            .comment("How far the lighting remote reaches: linked fixtures and aimed blocks, in blocks.")
            .translation("lumen_rigs.configuration.remoteRange")
            .defineInRange("remoteRange", 128, 8, 512);
    private static final ModConfigSpec.IntValue FOLLOW_RANGE = BUILDER
            .comment("Fixtures stop following an entity that is further away than this, in blocks.")
            .translation("lumen_rigs.configuration.followRange")
            .defineInRange("followRange", 96, 8, 256);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue BEAMS = CLIENT
            .comment("Draw visible light beams.")
            .translation("lumen_rigs.configuration.beams")
            .define("beams", true);
    private static final ModConfigSpec.DoubleValue BEAM_STRENGTH = CLIENT
            .comment("How visible the beams are.")
            .translation("lumen_rigs.configuration.beamStrength")
            .defineInRange("beamStrength", 1.0, 0.1, 2.0);
    private static final ModConfigSpec.BooleanValue LIGHTING = CLIENT
            .comment("Let fixtures light up blocks and mobs. The light is client-side: it does not stop mobs from spawning.")
            .translation("lumen_rigs.configuration.lighting")
            .define("lighting", true);
    private static final ModConfigSpec.EnumValue<LightingEngine> LIGHTING_ENGINE = CLIENT
            .comment("AUTO uses Veil's per-pixel lights (with shadows and volumetric beams) when Veil is installed, otherwise",
                    "the built-in block light. BLOCK_LIGHT always uses the built-in one.")
            .translation("lumen_rigs.configuration.lightingEngine")
            .defineEnum("lightingEngine", LightingEngine.AUTO);
    private static final ModConfigSpec.DoubleValue COLOR_STRENGTH = CLIENT
            .comment("How strongly colored light tints what it hits.")
            .translation("lumen_rigs.configuration.colorStrength")
            .defineInRange("colorStrength", 1.0, 0.0, 1.5);
    private static final ModConfigSpec.IntValue MAX_LIGHTS = CLIENT
            .comment("At most this many fixtures (the nearest ones) light up the world at once. Moving lights re-render the",
                    "chunks they reach, so lower this on weak computers.")
            .translation("lumen_rigs.configuration.maxLights")
            .defineInRange("maxLights", 48, 1, 512);

    public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

    private Config() {
    }

    public enum LightingEngine {
        AUTO,
        BLOCK_LIGHT
    }

    public static LightingEngine lightingEngine() {
        return LIGHTING_ENGINE.get();
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
