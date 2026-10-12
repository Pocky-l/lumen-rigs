package com.pockyl.lumen_rigs.fixture;

import net.minecraft.util.Mth;

/**
 * An effect played by a fixture's light. Effects run on game time, so every fixture with the same effect and speed
 * plays in step, for every player.
 */
public enum LightEffect {
    STEADY,
    /** Brightness breathes smoothly between dim and full. */
    PULSE,
    /** Short flashes. */
    STROBE,
    /** The color travels around the color wheel, starting from the set color. */
    RAINBOW;

    public static final float MIN_SPEED = 6;
    public static final float MAX_SPEED = 600;
    public static final float DEFAULT_SPEED = 30;
    private static final float PULSE_MIN = 0.1F;
    private static final double STROBE_FLASH = 0.2;
    /** A rainbow from a pale color (warm white) would hardly change; it gets at least this saturation. */
    private static final float RAINBOW_SATURATION = 0.85F;

    public static LightEffect byId(int id) {
        LightEffect[] values = values();
        return id >= 0 && id < values.length ? values[id] : STEADY;
    }

    public LightEffect next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Cycles played since the world started; {@code speed} is in cycles per minute. */
    public static double cycles(long gameTime, float partialTick, float speed) {
        return (gameTime + (double) partialTick) * speed / 1200.0;
    }

    /** Share of the light that is on at this point of the effect, 0-1. */
    public float intensity(double cycles) {
        double phase = cycles - Math.floor(cycles);
        return switch (this) {
            case STEADY, RAINBOW -> 1.0F;
            case PULSE -> PULSE_MIN + (1 - PULSE_MIN) * (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2));
            case STROBE -> phase < STROBE_FLASH ? 1.0F : 0.0F;
        };
    }

    /** The light's color at this point of the effect. */
    public int color(int base, double cycles) {
        if (this != RAINBOW) {
            return base;
        }
        float r = (base >> 16 & 0xFF) / 255.0F;
        float g = (base >> 8 & 0xFF) / 255.0F;
        float b = (base & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float hue = 0;
        if (max - min > 1.0E-3F) {
            if (max == r) {
                hue = (g - b) / (max - min) / 6;
            } else if (max == g) {
                hue = (2 + (b - r) / (max - min)) / 6;
            } else {
                hue = (4 + (r - g) / (max - min)) / 6;
            }
        }
        float saturation = max <= 0 ? 0 : Math.max(RAINBOW_SATURATION, (max - min) / max);
        double shifted = hue + cycles;
        return Mth.hsvToRgb((float) (shifted - Math.floor(shifted)), saturation, Math.max(max, 0.5F)) & 0xFFFFFF;
    }
}
