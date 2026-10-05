package com.pockyl.lumen_rigs.fixture;

/** How a fixture reacts to redstone. */
public enum RedstoneMode {
    IGNORE,
    ON_WHEN_POWERED,
    OFF_WHEN_POWERED,
    /** The signal strength sets the brightness. */
    DIMMER;

    public static RedstoneMode byId(int id) {
        RedstoneMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : IGNORE;
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Brightness the fixture actually gives with this redstone signal. */
    public int apply(int brightness, int signal) {
        return switch (this) {
            case IGNORE -> brightness;
            case ON_WHEN_POWERED -> signal > 0 ? brightness : 0;
            case OFF_WHEN_POWERED -> signal > 0 ? 0 : brightness;
            case DIMMER -> Math.round(brightness * signal / 15.0F);
        };
    }
}
