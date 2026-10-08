package com.pockyl.lumen_rigs.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlock;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.Aim;
import com.pockyl.lumen_rigs.fixture.AimMode;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.fixture.RedstoneMode;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;
import com.pockyl.lumen_rigs.registry.ModBlocks;
import com.pockyl.lumen_rigs.registry.ModItems;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure and build what they need around it; every test has its own batch.
 * The light and beams are client-side and are not covered here.
 */
@GameTestHolder(LumenRigs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private static final double EPSILON = 1.0E-3;

    private ModGameTests() {
    }

    @GameTest(template = "empty", batch = "modLoads")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "aimMathRoundTrips")
    public static void aimMathRoundTrips(GameTestHelper helper) {
        Vec3 direction = Aim.direction(35, -20);
        helper.assertTrue(Math.abs(Aim.pan(direction) - 35) < EPSILON && Math.abs(Aim.tilt(direction) + 20) < EPSILON, "pan/tilt survive a round trip");
        helper.assertTrue(Aim.direction(0, 0).distanceTo(new Vec3(0, 0, 1)) < EPSILON, "pan 0 looks south");
        Vec3 turned = Aim.turnTowards(new Vec3(0, 0, 1), new Vec3(1, 0, 0), 10);
        helper.assertTrue(Math.abs(Math.toDegrees(Math.acos(turned.z)) - 10) < 0.01, "the head turns 10 degrees per step");
        helper.assertTrue(Aim.turnTowards(new Vec3(0, 0, 1), new Vec3(0, 0, 1), 10).distanceTo(new Vec3(0, 0, 1)) < EPSILON, "no turn needed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mountFacesMoveThePivot")
    public static void mountFacesMoveThePivot(GameTestHelper helper) {
        FixtureBlockEntity floor = place(helper, new BlockPos(1, 2, 1), ModBlocks.SPOTLIGHT.get().defaultBlockState());
        FixtureBlockEntity ceiling = place(helper, new BlockPos(3, 3, 1),
                ModBlocks.SPOTLIGHT.get().defaultBlockState().setValue(FixtureBlock.FACING, Direction.DOWN));
        Vec3 floorPivot = floor.pivot().subtract(Vec3.atLowerCornerOf(floor.getBlockPos()));
        Vec3 ceilingPivot = ceiling.pivot().subtract(Vec3.atLowerCornerOf(ceiling.getBlockPos()));
        double height = FixtureType.SPOTLIGHT.pivotY();
        helper.assertTrue(Math.abs(floorPivot.y - height) < EPSILON, "standing on the floor, the pivot is above the base");
        helper.assertTrue(Math.abs(ceilingPivot.y - (1 - height)) < EPSILON, "hanging from the ceiling, it is below it");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "aimingAtAPointPointsThere")
    public static void aimingAtAPointPointsThere(GameTestHelper helper) {
        FixtureBlockEntity fixture = place(helper, new BlockPos(1, 2, 1), ModBlocks.SPOTLIGHT.get().defaultBlockState());
        Vec3 target = fixture.pivot().add(5, -3, 2);
        fixture.aimAt(target);
        helper.assertTrue(fixture.settings().mode() == AimMode.POINT, "point mode");
        Vec3 expected = target.subtract(fixture.pivot()).normalize();
        helper.assertTrue(fixture.desiredDirection(0).distanceTo(expected) < EPSILON, "aims at the point");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "softPanelShinesOutOfItsFace")
    public static void softPanelShinesOutOfItsFace(GameTestHelper helper) {
        FixtureBlockEntity panel = place(helper, new BlockPos(1, 2, 1),
                ModBlocks.SOFT_PANEL.get().defaultBlockState().setValue(FixtureBlock.FACING, Direction.EAST));
        panel.aimAt(panel.pivot().add(0, 5, 0));
        helper.assertTrue(panel.desiredDirection(0).distanceTo(new Vec3(1, 0, 0)) < EPSILON, "a panel cannot be turned");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sweepSwingsAroundThePan")
    public static void sweepSwingsAroundThePan(GameTestHelper helper) {
        FixtureBlockEntity fixture = place(helper, new BlockPos(1, 2, 1), ModBlocks.SEARCHLIGHT.get().defaultBlockState());
        fixture.applySettings(FixtureSettings.defaults(FixtureType.SEARCHLIGHT).withAim(0, 30, AimMode.SWEEP).withSweep(40, 6));
        // 6 sweeps per minute: a quarter swing after 50 ticks puts it at the full width.
        float pan = Aim.pan(fixture.desiredDirection(50));
        helper.assertTrue(Math.abs(Math.abs(pan) - 40) < 0.5, "swings out by the sweep width, got " + pan);
        helper.assertTrue(Math.abs(Aim.pan(fixture.desiredDirection(0))) < 0.5, "starts in the middle");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "redstoneModesControlTheLight")
    public static void redstoneModesControlTheLight(GameTestHelper helper) {
        helper.assertTrue(RedstoneMode.IGNORE.apply(12, 0) == 12, "ignore keeps the brightness");
        helper.assertTrue(RedstoneMode.ON_WHEN_POWERED.apply(12, 0) == 0 && RedstoneMode.ON_WHEN_POWERED.apply(12, 3) == 12, "on when powered");
        helper.assertTrue(RedstoneMode.OFF_WHEN_POWERED.apply(12, 5) == 0, "off when powered");
        helper.assertTrue(RedstoneMode.DIMMER.apply(15, 5) == 5, "the dimmer follows the signal");

        FixtureBlockEntity fixture = place(helper, new BlockPos(1, 2, 1), ModBlocks.FLOODLIGHT.get().defaultBlockState());
        fixture.applySettings(FixtureSettings.defaults(FixtureType.FLOODLIGHT).withRedstone(RedstoneMode.ON_WHEN_POWERED));
        helper.assertTrue(fixture.effectiveBrightness() == 0, "dark without a signal");
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertTrue(fixture.effectiveBrightness() == 15, "lit by the redstone block"));
    }

    @GameTest(template = "empty", batch = "settingsFromClientsAreClamped")
    public static void settingsFromClientsAreClamped(GameTestHelper helper) {
        FixtureBlockEntity fixture = place(helper, new BlockPos(1, 2, 1), ModBlocks.SPOTLIGHT.get().defaultBlockState());
        fixture.applySettings(new FixtureSettings(Float.NaN, 400, 999, 99, 0x7F123456, AimMode.MANUAL, RedstoneMode.IGNORE, -5, 1000,
                50, -1, Float.POSITIVE_INFINITY, 9999));
        FixtureSettings settings = fixture.settings();
        helper.assertTrue(settings.power() == FixtureSettings.MAX_POWER && settings.softness() == 0, "power and softness clamped");
        helper.assertTrue(settings.haze() == 1 && settings.range() == FixtureType.SPOTLIGHT.maxRange(), "haze and range clamped");
        helper.assertTrue(settings.pan() == 0 && settings.tilt() == 90, "aim clamped");
        helper.assertTrue(settings.beam() == FixtureType.SPOTLIGHT.maxBeam() && settings.brightness() == 15, "beam and brightness clamped");
        helper.assertTrue(settings.color() == 0x123456, "only RGB is kept");
        helper.assertTrue(settings.sweepWidth() == FixtureSettings.MIN_SWEEP_WIDTH && settings.sweepSpeed() == FixtureSettings.MAX_SWEEP_SPEED,
                "sweep clamped");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "pastedSettingsCopyEverything")
    public static void pastedSettingsCopyEverything(GameTestHelper helper) {
        FixtureSettings source = FixtureSettings.defaults(FixtureType.SPOTLIGHT).withColor(0xFF2A1E).withBrightness(9).withPower(2.5F)
                .withSoftness(0.8F).withHaze(2.0F).withRedstone(RedstoneMode.DIMMER).withBeam(12).withRange(20)
                .withAim(170, -60, AimMode.SWEEP);

        FixtureSettings pasted = FixtureSettings.paste(source, FixtureType.SPOTLIGHT);
        helper.assertTrue(pasted.equals(source), "a fixture of the same kind gets every setting");

        FixtureSettings onFlood = FixtureSettings.paste(source, FixtureType.FLOODLIGHT);
        helper.assertTrue(onFlood.pan() == 170 && onFlood.tilt() == -60 && onFlood.mode() == AimMode.SWEEP, "the aim is pasted");
        helper.assertTrue(onFlood.color() == 0xFF2A1E && onFlood.power() == 2.5F, "the look is pasted");
        helper.assertTrue(onFlood.beam() == FixtureType.FLOODLIGHT.minBeam(), "a too narrow beam is widened to what a floodlight can do");

        FixtureSettings pointing = source.withMode(AimMode.POINT);
        helper.assertTrue(FixtureSettings.paste(pointing, FixtureType.SPOTLIGHT).mode() == AimMode.MANUAL,
                "a point aim becomes a manual aim in the same direction");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "remoteLinksAimsAndFollows")
    public static void remoteLinksAimsAndFollows(GameTestHelper helper) {
        FixtureBlockEntity first = place(helper, new BlockPos(1, 2, 1), ModBlocks.SPOTLIGHT.get().defaultBlockState());
        FixtureBlockEntity second = place(helper, new BlockPos(3, 2, 1), ModBlocks.FLOODLIGHT.get().defaultBlockState());
        Player player = helper.makeMockPlayer();
        player.moveTo(helper.absoluteVec(new Vec3(2, 2, 3)));
        ItemStack remote = new ItemStack(ModItems.LIGHTING_REMOTE.get());

        helper.assertTrue(LightingRemoteItem.toggleLink(remote, GlobalPos.of(helper.getLevel().dimension(), first.getBlockPos())), "first linked");
        helper.assertTrue(LightingRemoteItem.toggleLink(remote, GlobalPos.of(helper.getLevel().dimension(), second.getBlockPos())), "second linked");
        helper.assertTrue(LightingRemoteItem.links(remote).size() == 2, "two links");

        Vec3 target = helper.absoluteVec(new Vec3(8, 1, 8));
        int aimed = LightingRemoteItem.forEachLinked(remote, player, fixture -> fixture.aimAt(target));
        helper.assertTrue(aimed == 2, "both fixtures aimed");
        helper.assertTrue(first.settings().mode() == AimMode.POINT && second.settings().mode() == AimMode.POINT, "both in point mode");

        Cow cow = helper.spawn(EntityType.COW, new Vec3(6, 2, 6));
        LightingRemoteItem.forEachLinked(remote, player, fixture -> fixture.follow(cow));
        helper.assertTrue(first.settings().mode() == AimMode.FOLLOW && first.followId() == cow.getId(), "follows the cow");

        helper.assertFalse(LightingRemoteItem.toggleLink(remote, GlobalPos.of(helper.getLevel().dimension(), first.getBlockPos())), "unlinked again");
        helper.assertTrue(LightingRemoteItem.links(remote).size() == 1, "one link left");
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "lightSettingsSurviveSaving")
    public static void lightSettingsSurviveSaving(GameTestHelper helper) {
        FixtureSettings settings = FixtureSettings.defaults(FixtureType.SEARCHLIGHT).withPower(2.5F).withSoftness(0.1F).withHaze(2)
                .withRange(200).withColor(0x2E62FF);
        FixtureSettings loaded = FixtureSettings.load(settings.save(), FixtureType.SEARCHLIGHT);
        helper.assertTrue(loaded.equals(settings.clamp(FixtureType.SEARCHLIGHT)), "saved and loaded: " + loaded);
        helper.assertTrue(FixtureSettings.load(new CompoundTag(), FixtureType.SOFT_PANEL).softness() == 1,
                "a new panel is fully diffuse");
        helper.succeed();
    }

    private static FixtureBlockEntity place(GameTestHelper helper, BlockPos relative, BlockState state) {
        helper.setBlock(relative, state);
        if (!(helper.getBlockEntity(relative) instanceof FixtureBlockEntity fixture)) {
            throw new IllegalStateException("No fixture at " + relative);
        }
        return fixture;
    }
}
