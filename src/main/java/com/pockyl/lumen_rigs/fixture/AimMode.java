package com.pockyl.lumen_rigs.fixture;

/** How a fixture decides where to point. */
public enum AimMode {
    /** A fixed direction (pan and tilt). */
    MANUAL,
    /** Always at one point in the world, set with the lighting remote. */
    POINT,
    /** Tracks an entity picked with the remote, or the nearest player. */
    FOLLOW,
    /** Swings left and right around its pan and tilt. */
    SWEEP;

    public static AimMode byId(int id) {
        AimMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : MANUAL;
    }

    public AimMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
