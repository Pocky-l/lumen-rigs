package com.pockyl.lumen_rigs.client.light;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side colored light of the fixtures.
 * <p>
 * Aimable fixtures cast rays through their beam cone; where a ray hits something, the block in front of the hit gets
 * light that fades with distance and towards the cone's edge, softened a little into its neighbors. Soft panels fill
 * the space in front of them like vanilla light. The result feeds the rendering through the mixins: block light is
 * raised to it and block vertices are tinted with the light's color while chunks are meshed. When a fixture's light
 * changes, only the chunk sections whose light actually changed are re-meshed. Meshing runs on worker threads, so the
 * lights are published as an immutable snapshot.
 */
@Mod.EventBusSubscriber(modid = LumenRigs.MOD_ID, value = Dist.CLIENT)
public final class FixtureLights {
    /** Re-cast a still fixture this often, to notice placed or broken blocks. */
    private static final int REFRESH_INTERVAL = 40;
    /** Builds per tick; a turning head is rebuilt at most every other tick. */
    private static final int MAX_BUILDS_PER_TICK = 4;
    private static final double TURN_THRESHOLD = Math.toRadians(0.75);
    private static final int MAX_RAYS_PER_SIDE = 26;
    /** Light lost per block when a lit spot softens into its neighbors. */
    private static final int SOFTEN_STEP = 3;
    private static final double MAX_BLOCK_LIGHT_RANGE = 96;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static volatile LightSource[] sources = new LightSource[0];
    private static final Map<FixtureBlockEntity, Built> BUILT = new IdentityHashMap<>();
    private static ClientLevel lastLevel;
    private static boolean failed;

    private FixtureLights() {
    }

    /** One fixture's light. */
    record LightSource(Long2ByteOpenHashMap light, float red, float green, float blue, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {

        boolean reaches(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    /** What a source was built from, to know when it has to be rebuilt. */
    private record Built(LightSource source, Vec3 direction, int brightness, FixtureSettings settings, long builtAt) {

        /** Whether the settings that shape the light (not the aim, which is compared by direction) changed. */
        boolean shapeChanged(FixtureSettings current) {
            return settings.beam() != current.beam() || settings.color() != current.color() || settings.power() != current.power()
                    || settings.softness() != current.softness() || settings.range() != current.range();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Queries (any thread)
    // ------------------------------------------------------------------------------------------------

    /** Brightest fixture light at a block position, 0 if none. */
    public static int blockLight(BlockPos pos) {
        LightSource[] current = sources;
        if (current.length == 0) {
            return 0;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        long key = BlockPos.asLong(x, y, z);
        int light = 0;
        for (LightSource source : current) {
            if (source.reaches(x, y, z)) {
                light = Math.max(light, source.light().get(key));
            }
        }
        return light;
    }

    /** Whether any fixture light reaches the block at this position or right next to it. */
    public static boolean anyNear(BlockPos pos) {
        LightSource[] current = sources;
        for (LightSource source : current) {
            if (pos.getX() >= source.minX() - 1 && pos.getX() <= source.maxX() + 1 && pos.getY() >= source.minY() - 1
                    && pos.getY() <= source.maxY() + 1 && pos.getZ() >= source.minZ() - 1 && pos.getZ() <= source.maxZ() + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the color multiplier of a vertex into {@code out} (r, g, b): white when no fixture light reaches it,
     * towards the lights' colors where they are strong. The light is sampled in the four blocks the vertex touches on
     * the side its face looks at, like vanilla smooth lighting.
     */
    public static void tint(float x, float y, float z, Direction face, int sky, float[] out, int offset) {
        out[offset] = 1.0F;
        out[offset + 1] = 1.0F;
        out[offset + 2] = 1.0F;
        LightSource[] current = sources;
        float strength = Config.colorStrength();
        if (current.length == 0 || strength <= 0) {
            return;
        }
        float px = x + face.getStepX() * 0.5F;
        float py = y + face.getStepY() * 0.5F;
        float pz = z + face.getStepZ() * 0.5F;
        Direction.Axis axis = face.getAxis();
        float total = 0;
        float r = 0;
        float g = 0;
        float b = 0;
        for (LightSource source : current) {
            int sum = 0;
            int count = 0;
            for (int i = 0; i < 4; i++) {
                float du = (i & 1) == 0 ? -0.5F : 0.5F;
                float dv = (i & 2) == 0 ? -0.5F : 0.5F;
                int bx = Mth.floor(px + (axis == Direction.Axis.X ? 0 : du));
                int by = Mth.floor(py + (axis == Direction.Axis.Y ? 0 : axis == Direction.Axis.X ? du : dv));
                int bz = Mth.floor(pz + (axis == Direction.Axis.Z ? 0 : dv));
                if (!source.reaches(bx, by, bz)) {
                    continue;
                }
                int light = source.light().get(BlockPos.asLong(bx, by, bz));
                if (light > 0) {
                    sum += light;
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }
            float weight = sum / (float) count / 15.0F;
            total += weight;
            r += weight * source.red();
            g += weight * source.green();
            b += weight * source.blue();
        }
        if (total < 0.01F) {
            return;
        }
        // Mixed colors add up like light: the brightest channel is brought back to full.
        float mr = r / total;
        float mg = g / total;
        float mb = b / total;
        float peak = Math.max(mr, Math.max(mg, mb));
        if (peak > 0.001F) {
            mr /= peak;
            mg /= peak;
            mb /= peak;
        }
        float amount = Math.min(1.0F, total * strength) * (1.0F - 0.5F * sky / 15.0F);
        out[offset] = 1.0F - amount * (1.0F - mr);
        out[offset + 1] = 1.0F - amount * (1.0F - mg);
        out[offset + 2] = 1.0F - amount * (1.0F - mb);
    }

    // ------------------------------------------------------------------------------------------------
    // Updates (client thread)
    // ------------------------------------------------------------------------------------------------

    // Never let the light break the game: on any error the lights are dropped and the problem is logged once.
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            update();
        } catch (RuntimeException e) {
            if (!failed) {
                failed = true;
                LumenRigs.LOGGER.error("Fixture lights failed to update; they are turned off for now", e);
            }
            BUILT.clear();
            sources = new LightSource[0];
        }
    }

    private static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            BUILT.clear();
            sources = new LightSource[0];
            lastLevel = level;
        }
        if (level == null) {
            return;
        }
        List<FixtureBlockEntity> active = Config.lighting()
                ? ClientLighting.activeFixtures(level, minecraft.gameRenderer.getMainCamera().getPosition())
                : List.of();
        long time = level.getGameTime();
        Map<FixtureBlockEntity, Built> previous = new IdentityHashMap<>(BUILT);
        BUILT.clear();
        int builds = 0;
        for (FixtureBlockEntity fixture : active) {
            Built old = previous.remove(fixture);
            Vec3 direction = fixture.headDirection(1.0F);
            FixtureSettings settings = fixture.settings();
            int brightness = fixture.effectiveBrightness();
            boolean changed = old == null || old.brightness() != brightness || old.shapeChanged(settings)
                    || Math.acos(Mth.clamp(old.direction().dot(direction), -1, 1)) > TURN_THRESHOLD;
            boolean due = old == null || changed && time - old.builtAt() >= 2 || time - old.builtAt() >= REFRESH_INTERVAL;
            if (due && (old == null || builds < MAX_BUILDS_PER_TICK)) {
                builds++;
                LightSource source = build(level, fixture, direction, brightness);
                markChanged(old != null ? old.source() : null, source);
                BUILT.put(fixture, new Built(source, direction, brightness, settings, time));
            } else {
                BUILT.put(fixture, old);
            }
        }
        for (Built gone : previous.values()) {
            markChanged(gone.source(), null);
        }
        sources = BUILT.values().stream().map(Built::source).toArray(LightSource[]::new);
    }

    // ------------------------------------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------------------------------------

    static LightSource build(ClientLevel level, FixtureBlockEntity fixture, Vec3 direction, int brightness) {
        Long2ByteOpenHashMap light = new Long2ByteOpenHashMap();
        if (fixture.type() == FixtureType.SOFT_PANEL) {
            fillPanel(level, fixture, brightness, light);
        } else {
            castBeam(level, fixture, direction, brightness, light);
            soften(level, light);
        }
        int color = fixture.settings().color();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long key : light.keySet()) {
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return new LightSource(light, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Rays spread over the beam cone; each lights the spot where it hits something. */
    private static void castBeam(ClientLevel level, FixtureBlockEntity fixture, Vec3 direction, int brightness, Long2ByteOpenHashMap light) {
        FixtureSettings settings = fixture.settings();
        double half = Math.toRadians(settings.beam() / 2);
        // Block light cannot show far beams well anyway; long rays would only cost time.
        double range = Math.min(settings.range(), MAX_BLOCK_LIGHT_RANGE);
        double edgeStart = half * (1 - 0.9 * settings.softness());
        double power = 0.6 + 0.4 * settings.power();
        Vec3 origin = fixture.lens(direction);
        Vec3 u = Math.abs(direction.y) < 0.95 ? direction.cross(new Vec3(0, 1, 0)).normalize() : direction.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 v = u.cross(direction).normalize();
        // Rays roughly a block apart at full range, but never more than the cap.
        double step = Math.max(Math.atan(0.9 / range), half / MAX_RAYS_PER_SIDE);
        int n = (int) Math.ceil(half / step);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                double ax = i * step;
                double ay = j * step;
                double angle = Math.sqrt(ax * ax + ay * ay);
                if (angle > half + 1.0E-6) {
                    continue;
                }
                Vec3 ray = direction.add(u.scale(Math.tan(ax))).add(v.scale(Math.tan(ay))).normalize();
                // The softness sets how much of the cone fades out towards its edge.
                double edge = 1 - smoothstep(edgeStart, half + 1.0E-6, angle);
                cast(level, origin, ray, range, brightness * power, edge, light, pos);
            }
        }
    }

    /** Walks the ray block by block until it hits an opaque block, then lights the last open block before it. */
    private static void cast(ClientLevel level, Vec3 origin, Vec3 ray, double range, double brightness, double edge, Long2ByteOpenHashMap light,
            BlockPos.MutableBlockPos pos) {
        int x = Mth.floor(origin.x);
        int y = Mth.floor(origin.y);
        int z = Mth.floor(origin.z);
        int stepX = ray.x > 0 ? 1 : -1;
        int stepY = ray.y > 0 ? 1 : -1;
        int stepZ = ray.z > 0 ? 1 : -1;
        double deltaX = Math.abs(1 / ray.x);
        double deltaY = Math.abs(1 / ray.y);
        double deltaZ = Math.abs(1 / ray.z);
        double maxX = (ray.x > 0 ? x + 1 - origin.x : origin.x - x) * deltaX;
        double maxY = (ray.y > 0 ? y + 1 - origin.y : origin.y - y) * deltaY;
        double maxZ = (ray.z > 0 ? z + 1 - origin.z : origin.z - z) * deltaZ;
        int lastX = x;
        int lastY = y;
        int lastZ = z;
        double travelled = 0;
        while (travelled <= range) {
            if (maxX < maxY && maxX < maxZ) {
                x += stepX;
                travelled = maxX;
                maxX += deltaX;
            } else if (maxY < maxZ) {
                y += stepY;
                travelled = maxY;
                maxY += deltaY;
            } else {
                z += stepZ;
                travelled = maxZ;
                maxZ += deltaZ;
            }
            if (travelled > range || !level.isInWorldBounds(pos.set(x, y, z))) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getLightBlock(level, pos) >= 15 || !state.getCollisionShape(level, pos).isEmpty() && !state.propagatesSkylightDown(level, pos)) {
                double falloff = 1 - 0.55 * (travelled / range) * (travelled / range);
                int value = (int) Math.min(15, Math.round(brightness * edge * falloff));
                if (value > 0) {
                    long key = BlockPos.asLong(lastX, lastY, lastZ);
                    if (value > light.get(key)) {
                        light.put(key, (byte) value);
                    }
                }
                return;
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    /** Lets every lit spot bleed a little into the open blocks around it, so the pool of light has soft edges. */
    private static void soften(ClientLevel level, Long2ByteOpenHashMap light) {
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        for (long key : light.keySet()) {
            queue.enqueue(key);
        }
        spread(level, light, queue, SOFTEN_STEP, null);
    }

    /** Vanilla-like light from the panel's face, only into the half-space in front of it. */
    private static void fillPanel(ClientLevel level, FixtureBlockEntity fixture, int brightness, Long2ByteOpenHashMap light) {
        BlockPos front = fixture.getBlockPos();
        int level0 = (int) Math.min(15, Math.round(brightness * (0.6 + 0.4 * fixture.settings().power())));
        light.put(front.asLong(), (byte) level0);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(front.asLong());
        spread(level, light, queue, 1, fixture);
    }

    private static void spread(ClientLevel level, Long2ByteOpenHashMap light, LongArrayFIFOQueue queue, int loss, FixtureBlockEntity panel) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Direction facing = panel != null ? panel.facing() : null;
        BlockPos origin = panel != null ? panel.getBlockPos() : null;
        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();
            int value = light.get(current);
            if (value <= loss) {
                continue;
            }
            for (Direction direction : DIRECTIONS) {
                pos.set(current).move(direction);
                if (origin != null && (pos.getX() - origin.getX()) * facing.getStepX() + (pos.getY() - origin.getY()) * facing.getStepY()
                        + (pos.getZ() - origin.getZ()) * facing.getStepZ() < 0) {
                    continue;
                }
                long next = pos.asLong();
                BlockState state = level.getBlockState(pos);
                int spread = value - Math.max(loss, state.getLightBlock(level, pos));
                if (spread > light.get(next)) {
                    light.put(next, (byte) spread);
                    queue.enqueue(next);
                }
            }
        }
    }

    private static double smoothstep(double from, double to, double value) {
        double t = Mth.clamp((value - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    // ------------------------------------------------------------------------------------------------
    // Re-meshing
    // ------------------------------------------------------------------------------------------------

    /** Re-meshes the chunk sections where the light differs between the old and the new source. */
    private static void markChanged(LightSource old, LightSource current) {
        LongSet sections = new LongOpenHashSet();
        collectChanged(old, current, sections);
        collectChanged(current, old, sections);
        Minecraft minecraft = Minecraft.getInstance();
        for (long section : sections) {
            minecraft.levelRenderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
        }
    }

    private static void collectChanged(LightSource from, LightSource other, LongSet sections) {
        if (from == null) {
            return;
        }
        for (Long2ByteMap.Entry entry : from.light().long2ByteEntrySet()) {
            long key = entry.getLongKey();
            if (other != null && other.light().get(key) == entry.getByteValue()) {
                continue;
            }
            // The light of a block also shades its neighbors' faces (smooth lighting), which may be in the next section.
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dy = -1; dy <= 1; dy += 2) {
                    for (int dz = -1; dz <= 1; dz += 2) {
                        sections.add(SectionPos.asLong(SectionPos.blockToSectionCoord(x + dx), SectionPos.blockToSectionCoord(y + dy),
                                SectionPos.blockToSectionCoord(z + dz)));
                    }
                }
            }
        }
    }
}
