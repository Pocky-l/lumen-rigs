package com.pockyl.lumen_rigs.fixture;

import net.minecraft.util.StringRepresentable;

/**
 * The kinds of light fixtures. Angles are the full beam angle in degrees; positions are in blocks, measured in the
 * fixture's own frame (standing on the floor). Pivot and lens numbers match the models made by the asset generator.
 */
public enum FixtureType implements StringRepresentable {
    /** Narrow, zoomable stage spot. */
    SPOTLIGHT("spotlight", true, 5, 45, 20, 32, 9.0 / 16, 5.25 / 16, 2.7 / 16, 8.0F),
    /** Wide wash of light for areas and facades. */
    FLOODLIGHT("floodlight", true, 40, 120, 80, 24, 7.0 / 16, 2.85 / 16, 4.0 / 16, 8.0F),
    /** Long, tight beam that can sweep the sky. */
    SEARCHLIGHT("searchlight", true, 2, 15, 6, 64, 11.0 / 16, 7.1 / 16, 5.2 / 16, 5.0F),
    /** Flat panel that lights everything in front of it softly. */
    SOFT_PANEL("soft_panel", false, 180, 180, 180, 12, 0.1 / 16, 0, 6.0 / 16, 0);

    private final String name;
    private final boolean aimable;
    private final float minBeam;
    private final float maxBeam;
    private final float defaultBeam;
    private final int range;
    private final double pivotY;
    private final double lensOffset;
    private final double lensRadius;
    private final float turnSpeed;

    FixtureType(String name, boolean aimable, float minBeam, float maxBeam, float defaultBeam, int range, double pivotY,
            double lensOffset, double lensRadius, float turnSpeed) {
        this.name = name;
        this.aimable = aimable;
        this.minBeam = minBeam;
        this.maxBeam = maxBeam;
        this.defaultBeam = defaultBeam;
        this.range = range;
        this.pivotY = pivotY;
        this.lensOffset = lensOffset;
        this.lensRadius = lensRadius;
        this.turnSpeed = turnSpeed;
    }

    /** Whether the head can be turned; a soft panel always shines straight out of its mount face. */
    public boolean aimable() {
        return aimable;
    }

    public float minBeam() {
        return minBeam;
    }

    public float maxBeam() {
        return maxBeam;
    }

    public float defaultBeam() {
        return defaultBeam;
    }

    /** How far the light reaches, in blocks. */
    public int range() {
        return range;
    }

    /** Height of the head's pivot above the mount face. */
    public double pivotY() {
        return pivotY;
    }

    /** Distance from the pivot to the lens along the beam. */
    public double lensOffset() {
        return lensOffset;
    }

    public double lensRadius() {
        return lensRadius;
    }

    /** How fast the motorized head turns, in degrees per tick. */
    public float turnSpeed() {
        return turnSpeed;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
