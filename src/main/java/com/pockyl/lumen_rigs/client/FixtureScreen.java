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
import org.jetbrains.annotations.Nullable;

import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.Aim;
import com.pockyl.lumen_rigs.fixture.AimMode;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.network.ConfigureFixturePayload;
import com.pockyl.lumen_rigs.network.ModNetwork;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Settings of one fixture: aim on the left, the light itself on the right, colors below. Every change shows
 * immediately (the head turns while a slider is dragged) and is sent to the server a few times a second, so the screen
 * also works as a live control desk for the light.
 */
public final class FixtureScreen extends Screen {
    private static final int COLUMN = 186;
    private static final int GAP = 12;
    private static final int ROW = 22;
    private static final int SEND_INTERVAL = 3;
    /** Light colors of the dyes, in {@link DyeColor} order; vivid, since they are colors of light. */
    private static final int[] PALETTE = {
            0xF4F6FF, 0xFF7A12, 0xFF3CE6, 0x4FC3FF, 0xFFE12A, 0x9BFF2A, 0xFF7AB8, 0x8C96AA,
            0xC4CCDC, 0x1EE6D8, 0x9B3CFF, 0x2E62FF, 0xE08A2E, 0x2EE84A, 0xFF2A1E, 0x6A2CFF};

    /** Settings copied with the Copy button, kept for the game session. */
    @Nullable
    private static FixtureSettings clipboard;

    private final FixtureBlockEntity fixture;
    private FixtureSettings settings;
    private boolean dirty;
    private int sinceSend;
    private int top;
    private int aimX;
    private int lightX;
    private int colorY;

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
        int width = aimable ? COLUMN * 2 + GAP : COLUMN;
        int left = (this.width - width) / 2;
        aimX = left;
        lightX = aimable ? left + COLUMN + GAP : left;
        int rows = 8;
        top = Math.max(6, (height - (rows * ROW + 2 * ROW + 50)) / 2);
        int y0 = top + 26;

        if (aimable) {
            initAim(type, y0);
        }
        initLight(type, y0);

        // Colors: one swatch per dye, a hue slider and warm white, across the whole width.
        colorY = y0 + 7 * ROW + 8;
        int swatchY = colorY + 12;
        int step = Math.min(17, width / PALETTE.length);
        for (int i = 0; i < PALETTE.length; i++) {
            Swatch swatch = addRenderableWidget(new Swatch(left + i * step, swatchY, Math.max(10, step - 2), PALETTE[i]));
            swatch.setTooltip(Tooltip.create(Component.translatable("color.minecraft." + DyeColor.byId(i).getName())));
        }
        int hueY = swatchY + 20;
        slider(left, hueY, width - 84, "hue", 0, 359, 1, hueOf(settings.color()), FixtureScreen::degrees,
                value -> update(settings.withColor(Mth.hsvToRgb((float) value / 360.0F, 0.85F, 1.0F))));
        addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.warm_white"),
                        button -> update(settings.withColor(FixtureSettings.WARM_WHITE)))
                .bounds(left + width - 80, hueY, 80, 20)
                .build());

        int bottomY = hueY + ROW + 6;
        addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.copy"), button -> {
                    clipboard = settings;
                    rebuildWidgets();
                })
                .bounds(this.width / 2 - 50 - 4 - 70, bottomY, 70, 20)
                .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.copy.hint")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 50, bottomY, 100, 20)
                .build());
        Button paste = addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.paste"), button -> {
                    update(FixtureSettings.paste(clipboard, fixture.type()));
                    rebuildWidgets();
                })
                .bounds(this.width / 2 + 50 + 4, bottomY, 70, 20)
                .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.paste.hint")))
                .build());
        paste.active = clipboard != null;
    }

    /** Left column: where the fixture points. */
    private void initAim(FixtureType type, int y) {
        addRenderableWidget(Button.builder(modeLabel(), button -> {
                    update(settings.withMode(settings.mode().next()));
                    rebuildWidgets();
                })
                .bounds(aimX, y, COLUMN - 70, 20)
                .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.mode." + key(settings.mode()) + ".hint")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("lumen_rigs.screen.aim_at_me"), button -> {
                    aimAtPlayer();
                    rebuildWidgets();
                })
                .bounds(aimX + COLUMN - 66, y, 66, 20)
                .tooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.aim_at_me.hint")))
                .build());
        y += ROW;
        slider(aimX, y, COLUMN, "pan", -180, 180, 1, settings.pan(), FixtureScreen::degrees,
                value -> update(settings.withAim((float) value, settings.tilt(), manualMode())))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.pan.hint")));
        y += ROW;
        slider(aimX, y, COLUMN, "tilt", -90, 90, 1, settings.tilt(), FixtureScreen::degrees,
                value -> update(settings.withAim(settings.pan(), (float) value, manualMode())));
        y += ROW;
        slider(aimX, y, COLUMN, "beam", type.minBeam(), type.maxBeam(), 1, settings.beam(), FixtureScreen::degrees,
                value -> update(settings.withBeam((float) value)))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.beam.hint")));
        y += ROW;
        if (settings.mode() == AimMode.SWEEP) {
            slider(aimX, y, COLUMN, "sweep_width", FixtureSettings.MIN_SWEEP_WIDTH, FixtureSettings.MAX_SWEEP_WIDTH, 1, settings.sweepWidth(),
                    FixtureScreen::degrees, value -> update(settings.withSweep((float) value, settings.sweepSpeed())));
            y += ROW;
            slider(aimX, y, COLUMN, "sweep_speed", FixtureSettings.MIN_SWEEP_SPEED, FixtureSettings.MAX_SWEEP_SPEED, 1, settings.sweepSpeed(),
                    value -> String.valueOf((int) value), value -> update(settings.withSweep(settings.sweepWidth(), (float) value)));
        }
    }

    /** Right column: the light itself. */
    private void initLight(FixtureType type, int y) {
        addRenderableWidget(Button.builder(redstoneLabel(), button -> {
                    update(settings.withRedstone(settings.redstone().next()));
                    button.setMessage(redstoneLabel());
                })
                .bounds(lightX, y, COLUMN, 20)
                .build());
        y += ROW;
        slider(lightX, y, COLUMN, "brightness", 0, 15, 1, settings.brightness(), value -> String.valueOf((int) value),
                value -> update(settings.withBrightness((int) value)))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.brightness.hint")));
        y += ROW;
        slider(lightX, y, COLUMN, "power", FixtureSettings.MIN_POWER * 100, FixtureSettings.MAX_POWER * 100, 5, settings.power() * 100,
                FixtureScreen::percent, value -> update(settings.withPower((float) value / 100)))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.power.hint")));
        y += ROW;
        slider(lightX, y, COLUMN, "softness", 0, 100, 1, settings.softness() * 100, FixtureScreen::percent,
                value -> update(settings.withSoftness((float) value / 100)))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.softness.hint")));
        y += ROW;
        slider(lightX, y, COLUMN, "range", type.minRange(), type.maxRange(), 1, settings.range(), value -> (int) value + " m",
                value -> update(settings.withRange((float) value)))
                .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.range.hint")));
        y += ROW;
        if (type.aimable()) {
            slider(lightX, y, COLUMN, "haze", 0, FixtureSettings.MAX_HAZE * 100, 5, settings.haze() * 100, FixtureScreen::percent,
                    value -> update(settings.withHaze((float) value / 100)))
                    .setTooltip(Tooltip.create(Component.translatable("lumen_rigs.screen.haze.hint")));
        }
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

    /** Applies the change right away on this client and queues it for the server. */
    private void update(FixtureSettings newSettings) {
        settings = newSettings.clamp(fixture.type());
        fixture.applySettings(settings);
        dirty = true;
    }

    private void send() {
        if (dirty) {
            ModNetwork.sendToServer(new ConfigureFixturePayload(fixture.getBlockPos(), settings));
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
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 2, 0xFFFFFF);
        if (fixture.type().aimable()) {
            graphics.drawString(font, Component.translatable("lumen_rigs.screen.section.aim"), aimX + 2, top + 15, 0xA0C8FF);
        }
        graphics.drawString(font, Component.translatable("lumen_rigs.screen.section.light"), lightX + 2, top + 15, 0xFFD48A);
        graphics.drawString(font, Component.translatable("lumen_rigs.screen.color"), aimX + 2, colorY + 2, 0xC0C0C0);
        int right = fixture.type().aimable() ? lightX + COLUMN : aimX + COLUMN;
        graphics.fill(right - 40, colorY + 1, right - 2, colorY + 10, 0xFF000000 | settings.color());
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

    private static String percent(double value) {
        return (int) Math.round(value) + "%";
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

        Swatch(int x, int y, int size, int color) {
            super(x, y, size, size, Component.empty());
            this.color = color;
        }

        @Override
        public void onPress() {
            update(settings.withColor(color));
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
