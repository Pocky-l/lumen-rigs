package com.pockyl.lumen_rigs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.Aim;
import com.pockyl.lumen_rigs.fixture.AimMode;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.network.ConfigureFixturePayload;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Settings of one fixture. Every change shows immediately (the head turns while a slider is dragged) and is sent to
 * the server a few times a second, so the screen is also a live remote for the light.
 */
public final class FixtureScreen extends Screen {
    private static final int WIDTH = 284;
    private static final int ROW = 22;
    private static final int SEND_INTERVAL = 3;
    /** Light colors of the dyes, in {@link DyeColor} order; vivid, since they are colors of light. */
    private static final int[] PALETTE = {
            0xF4F6FF, 0xFF7A12, 0xFF3CE6, 0x4FC3FF, 0xFFE12A, 0x9BFF2A, 0xFF7AB8, 0x8C96AA,
            0xC4CCDC, 0x1EE6D8, 0x9B3CFF, 0x2E62FF, 0xE08A2E, 0x2EE84A, 0xFF2A1E, 0x6A2CFF};

    private final FixtureBlockEntity fixture;
    private FixtureSettings settings;
    private boolean dirty;
    private int sinceSend;
    private int left;
    private int top;
    private int colorLabelY;

    private FixtureScreen(FixtureBlockEntity fixture) {
        super(fixture.getBlockState().getBlock().getName());
        this.fixture = fixture;
        this.settings = fixture.settings();
    }

    /** Opens the settings of the fixture at {@code pos}, if it is still there. */
    public static void open(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture) {
            minecraft.setScreen(new FixtureScreen(fixture));
        }
    }

    @Override
    protected void init() {
        FixtureType type = fixture.type();
        boolean aimable = type.aimable();
        boolean sweep = settings.mode() == AimMode.SWEEP;
        int rows = (aimable ? 5 : 2) + (sweep && aimable ? 2 : 0) + 3;
        left = (width - WIDTH) / 2;
        top = Math.max(8, (height - rows * ROW - 40) / 2);
        int y = top + 22;

        if (aimable) {
            addRenderableWidget(Button.builder(modeLabel(), button -> {
                        update(settings.withMode(settings.mode().next()));
                        rebuildWidgets();
                    })
                    .bounds(left, y, 110, 20)
                    .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.mode." + key(settings.mode()) + ".hint")))
                    .build());
        }
        addRenderableWidget(Button.builder(redstoneLabel(), button -> {
                    update(new FixtureSettings(settings.pan(), settings.tilt(), settings.beam(), settings.brightness(), settings.color(),
                            settings.mode(), settings.redstone().next(), settings.sweepWidth(), settings.sweepSpeed()));
                    button.setMessage(redstoneLabel());
                })
                .bounds(aimable ? left + 114 : left, y, aimable ? 100 : WIDTH, 20)
                .build());
        if (aimable) {
            addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.aim_at_me"), button -> {
                        aimAtPlayer();
                        rebuildWidgets();
                    })
                    .bounds(left + 218, y, 66, 20)
                    .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.aim_at_me.hint")))
                    .build());
        }
        y += ROW + 4;

        if (aimable) {
            ValueSlider pan = slider(left, y, WIDTH, "pan", -180, 180, 1, settings.pan(), FixtureScreen::degrees,
                    value -> update(settings.withAim((float) value, settings.tilt(), manualMode())));
            pan.setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.pan.hint")));
            y += ROW;
            slider(left, y, WIDTH, "tilt", -90, 90, 1, settings.tilt(), FixtureScreen::degrees,
                    value -> update(settings.withAim(settings.pan(), (float) value, manualMode())));
            y += ROW;
            slider(left, y, WIDTH, "beam", type.minBeam(), type.maxBeam(), 1, settings.beam(), FixtureScreen::degrees,
                    value -> update(new FixtureSettings(settings.pan(), settings.tilt(), (float) value, settings.brightness(), settings.color(),
                            settings.mode(), settings.redstone(), settings.sweepWidth(), settings.sweepSpeed())));
            y += ROW;
            if (sweep) {
                slider(left, y, WIDTH, "sweep_width", FixtureSettings.MIN_SWEEP_WIDTH, FixtureSettings.MAX_SWEEP_WIDTH, 1, settings.sweepWidth(),
                        FixtureScreen::degrees, value -> update(new FixtureSettings(settings.pan(), settings.tilt(), settings.beam(),
                                settings.brightness(), settings.color(), settings.mode(), settings.redstone(), (float) value, settings.sweepSpeed())));
                y += ROW;
                slider(left, y, WIDTH, "sweep_speed", FixtureSettings.MIN_SWEEP_SPEED, FixtureSettings.MAX_SWEEP_SPEED, 1, settings.sweepSpeed(),
                        value -> String.valueOf((int) value), value -> update(new FixtureSettings(settings.pan(), settings.tilt(), settings.beam(),
                                settings.brightness(), settings.color(), settings.mode(), settings.redstone(), settings.sweepWidth(), (float) value)));
                y += ROW;
            }
        }
        slider(left, y, WIDTH, "brightness", 0, 15, 1, settings.brightness(), value -> String.valueOf((int) value),
                value -> update(new FixtureSettings(settings.pan(), settings.tilt(), settings.beam(), (int) value, settings.color(), settings.mode(),
                        settings.redstone(), settings.sweepWidth(), settings.sweepSpeed())));
        y += ROW + 2;
        colorLabelY = y;
        y += 12;

        // Colors: one swatch per dye, a hue slider and warm white.
        for (int i = 0; i < PALETTE.length; i++) {
            DyeColor dye = DyeColor.byId(i);
            Swatch swatch = addRenderableWidget(new Swatch(left + i * 17 + 2, y, PALETTE[i]));
            swatch.setTooltip(Tooltip.create(Component.translatable("color.minecraft." + dye.getName())));
        }
        y += ROW;
        slider(left, y, 200, "hue", 0, 359, 1, hueOf(settings.color()), FixtureScreen::degrees,
                value -> setColor(Mth.hsvToRgb((float) value / 360.0F, 0.85F, 1.0F)));
        addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.warm_white"), button -> setColor(FixtureSettings.WARM_WHITE))
                .bounds(left + 204, y, 80, 20)
                .build());
        y += ROW + 6;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + WIDTH / 2 - 50, y, 100, 20)
                .build());
    }

    private ValueSlider slider(int x, int y, int width, String name, double min, double max, double step, double value,
            DoubleFunction<String> format, DoubleConsumer onChange) {
        return addRenderableWidget(new ValueSlider(x, y, width, "lumen_rigs.screen." + name, min, max, step, value, format, onChange));
    }

    /** Dragging the aim sliders keeps a sweep sweeping, but takes the aim back from the remote or a followed target. */
    private AimMode manualMode() {
        return settings.mode() == AimMode.SWEEP ? AimMode.SWEEP : AimMode.MANUAL;
    }

    private void aimAtPlayer() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        Vec3 direction = minecraft.player.getEyePosition().subtract(fixture.pivot());
        if (direction.lengthSqr() > 1.0E-4) {
            direction = direction.normalize();
            update(settings.withAim(Aim.pan(direction), Aim.tilt(direction), AimMode.MANUAL));
        }
    }

    private void setColor(int color) {
        update(new FixtureSettings(settings.pan(), settings.tilt(), settings.beam(), settings.brightness(), color, settings.mode(),
                settings.redstone(), settings.sweepWidth(), settings.sweepSpeed()));
    }

    /** Applies the change right away on this client and queues it for the server. */
    private void update(FixtureSettings newSettings) {
        settings = newSettings.clamp(fixture.type());
        fixture.applySettings(settings);
        dirty = true;
    }

    private void send() {
        if (dirty) {
            PacketDistributor.sendToServer(new ConfigureFixturePayload(fixture.getBlockPos(), settings));
            dirty = false;
            sinceSend = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (fixture.isRemoved() || minecraft == null || minecraft.player == null
                || minecraft.player.distanceToSqr(fixture.getBlockPos().getCenter()) > 10 * 10) {
            onClose();
            return;
        }
        if (++sinceSend >= SEND_INTERVAL) {
            send();
        }
    }

    @Override
    public void removed() {
        send();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 6, 0xFFFFFF);
        int colorY = colorLabelY;
        graphics.drawString(font, Component.translatable("lumen_rigs.screen.color"), left + 2, colorY, 0xC0C0C0);
        // The chosen color, next to the label.
        graphics.fill(left + WIDTH - 40, colorY - 1, left + WIDTH - 2, colorY + 9, 0xFF000000 | settings.color());
    }

    private Component modeLabel() {
        return Component.translatable("lumen_rigs.screen.mode", Component.translatable("lumen_rigs.screen.mode." + key(settings.mode())));
    }

    private Component redstoneLabel() {
        return Component.translatable("lumen_rigs.screen.redstone",
                Component.translatable("lumen_rigs.screen.redstone." + key(settings.redstone())));
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String degrees(double value) {
        return (int) value + "°";
    }

    private static double hueOf(int color) {
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        if (max - min < 1.0E-3F) {
            return 0;
        }
        float hue;
        if (max == r) {
            hue = (g - b) / (max - min);
        } else if (max == g) {
            hue = 2 + (b - r) / (max - min);
        } else {
            hue = 4 + (r - g) / (max - min);
        }
        return (hue * 60 + 360) % 360;
    }

    /** A slider over a value range with a step, showing "Label: value". */
    private static final class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int width, String key, double min, double max, double step, double value, DoubleFunction<String> format,
                DoubleConsumer onChange) {
            super(x, y, width, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            updateMessage();
        }

        private double real() {
            return Mth.clamp(Math.round((min + value * (max - min)) / step) * step, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key, format.apply(real())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }

    /** A color to pick. */
    private final class Swatch extends AbstractButton {
        private final int color;

        Swatch(int x, int y, int color) {
            super(x, y, 15, 15, Component.empty());
            this.color = color;
        }

        @Override
        public void onPress() {
            setColor(color);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = settings.color() == color;
            int border = selected ? 0xFFFFFFFF : isHoveredOrFocused() ? 0xFFA0A0A0 : 0xFF202020;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
