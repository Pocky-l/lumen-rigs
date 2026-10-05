package com.pockyl.lumen_rigs.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.Aim;
import com.pockyl.lumen_rigs.fixture.FixtureType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws an aimable fixture: the base, the yoke turned towards the aim and the head tilted on it (motorized, see
 * {@link FixtureBlockEntity#headDirection}), the lens glowing in the light's color, a volumetric beam up to whatever
 * it hits and a lens flare when the beam points at the camera.
 */
public final class FixtureRenderer implements BlockEntityRenderer<FixtureBlockEntity> {
    private static final ResourceLocation FLARE = LumenRigs.id("textures/misc/flare.png");
    private static final RenderType BEAM = RenderType.create(LumenRigs.MOD_ID + "_beam", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final int BEAM_SEGMENTS = 14;
    private static final float OFF_LENS = 0.22F;

    private final Map<FixtureType, BakedModel[]> models = new EnumMap<>(FixtureType.class);

    public FixtureRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The separately rendered parts of an aimable fixture, registered as additional models. */
    public static ModelResourceLocation part(FixtureType type, String part) {
        return ModelResourceLocation.standalone(LumenRigs.id("block/" + type.getSerializedName() + "_" + part));
    }

    @Override
    public void render(FixtureBlockEntity fixture, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FixtureType type = fixture.type();
        if (!type.aimable() || fixture.getLevel() == null) {
            return;
        }
        BakedModel[] parts = models.computeIfAbsent(type, t -> new BakedModel[] {
                model(part(t, "base")), model(part(t, "yoke")), model(part(t, "head"))});
        Vec3 direction = fixture.headDirection(partialTick);
        Quaternionf mount = Aim.mountRotation(fixture.facing());
        Vector3f local = new Quaternionf(mount).conjugate().transform(new Vector3f((float) direction.x, (float) direction.y, (float) direction.z));
        float yokeAngle = (float) Math.atan2(local.x, local.z);
        float tilt = (float) Math.asin(Mth.clamp(local.y, -1, 1));
        int brightness = fixture.effectiveBrightness();
        int color = fixture.settings().color();
        float on = brightness / 15.0F;
        float lensR = Mth.lerp(on, OFF_LENS, (color >> 16 & 0xFF) / 255.0F);
        float lensG = Mth.lerp(on, OFF_LENS, (color >> 8 & 0xFF) / 255.0F);
        float lensB = Mth.lerp(on, OFF_LENS, (color & 0xFF) / 255.0F);

        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        VertexConsumer solid = buffers.getBuffer(Sheets.cutoutBlockSheet());
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(mount);
        pose.translate(-0.5, -0.5, -0.5);
        renderer.renderModel(pose.last(), solid, null, parts[0], 1, 1, 1, light, overlay, ModelData.EMPTY, null);
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(new Quaternionf().rotationY(yokeAngle));
        pose.translate(-0.5, 0, -0.5);
        renderer.renderModel(pose.last(), solid, null, parts[1], 1, 1, 1, light, overlay, ModelData.EMPTY, null);
        pose.translate(0.5, type.pivotY(), 0.5);
        pose.mulPose(new Quaternionf().rotationX(-tilt));
        pose.translate(-0.5, -type.pivotY(), -0.5);
        renderer.renderModel(pose.last(), solid, null, parts[2], lensR, lensG, lensB, light, overlay, ModelData.EMPTY, null);
        pose.popPose();

        if (brightness > 0) {
            renderLight(fixture, direction, brightness, pose, buffers);
        }
    }

    private static BakedModel model(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    // ------------------------------------------------------------------------------------------------
    // Beam and flare
    // ------------------------------------------------------------------------------------------------

    private static void renderLight(FixtureBlockEntity fixture, Vec3 direction, int brightness, PoseStack pose, MultiBufferSource buffers) {
        Level level = fixture.getLevel();
        FixtureType type = fixture.type();
        BlockPos origin = fixture.getBlockPos();
        Vec3 lens = fixture.lens(direction);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int color = fixture.settings().color();
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        // Beams show best in the dark, like real ones.
        BlockPos lensPos = BlockPos.containing(lens);
        float daylight = Math.max(0, level.getBrightness(LightLayer.SKY, lensPos) - level.getSkyDarken()) / 15.0F;
        float visibility = (1.0F - 0.7F * daylight) * brightness / 15.0F * Config.beamStrength();
        Matrix4f matrix = pose.last().pose();

        Vec3 toCamera = camera.subtract(lens);
        double facing = toCamera.lengthSqr() < 1.0E-6 ? 0 : Math.max(0, toCamera.normalize().dot(direction));
        if (Config.beams()) {
            double half = Math.toRadians(fixture.settings().beam() / 2);
            double length = beamLength(level, lens, direction, type.range());
            // Narrow beams concentrate their light; wide ones spread it thin.
            float density = (float) Mth.clamp(18.0 / fixture.settings().beam(), 0.35, 1.8);
            // Looking down the beam it would only be a thin sliver; fade it there, the flare takes over.
            float sideways = (float) Mth.clamp((1 - facing) * 3, 0.15, 1);
            VertexConsumer beam = buffers.getBuffer(BEAM);
            float alpha = 0.32F * visibility * density * sideways;
            strip(beam, matrix, origin, lens, direction, camera, length, half, type.lensRadius(), 0.45, alpha, r, g, b);
            strip(beam, matrix, origin, lens, direction, camera, length, half, type.lensRadius(), 1.0, alpha * 0.45F, r, g, b);
        }
        if (facing > 0.6) {
            float flare = (float) Math.pow((facing - 0.6) / 0.4, 3) * visibility;
            float size = (float) (type.lensRadius() * (2.0 + 7.0 * flare));
            Vec3 at = lens.add(direction.scale(0.02)).subtract(origin.getX(), origin.getY(), origin.getZ());
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            flare(pose, buffers.getBuffer(RenderType.eyes(FLARE)), size, r * flare, g * flare, b * flare);
            pose.popPose();
        }
    }

    /** How far the beam goes before it hits a block. */
    private static double beamLength(Level level, Vec3 lens, Vec3 direction, double range) {
        Vec3 end = lens.add(direction.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(lens, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(lens);
    }

    /**
     * A camera-facing strip along the beam, bright in the middle and transparent at the edges, widening with the cone
     * and fading with distance: from the side it reads as a cone of lit air.
     */
    private static void strip(VertexConsumer consumer, Matrix4f matrix, BlockPos origin, Vec3 lens, Vec3 direction, Vec3 camera, double length,
            double half, double lensRadius, double widthScale, float alpha, float r, float g, float b) {
        double tan = Math.tan(half);
        Vec3 previousCenter = null;
        Vec3 previousSide = null;
        float previousAlpha = 0;
        for (int i = 0; i <= BEAM_SEGMENTS; i++) {
            double t = i / (double) BEAM_SEGMENTS;
            double distance = length * t;
            Vec3 center = lens.add(direction.scale(distance));
            double width = (lensRadius + distance * tan) * widthScale;
            Vec3 side = direction.cross(camera.subtract(center));
            if (side.lengthSqr() < 1.0E-6) {
                side = direction.cross(new Vec3(0, 1, 0));
            }
            side = side.normalize().scale(width);
            // Brightest just in front of the lens, fading out towards the end.
            float a = (float) (alpha * Math.pow(1 - t, 1.4) * Math.min(1.0, t * 8 + 0.35));
            if (previousCenter != null) {
                quad(consumer, matrix, origin, previousCenter.add(previousSide), previousCenter, center, center.add(side), 0, previousAlpha, a, 0,
                        r, g, b);
                quad(consumer, matrix, origin, previousCenter, previousCenter.subtract(previousSide), center.subtract(side), center, previousAlpha, 0,
                        0, a, r, g, b);
            }
            previousCenter = center;
            previousSide = side;
            previousAlpha = a;
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, BlockPos origin, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, float a0, float a1,
            float a2, float a3, float r, float g, float b) {
        vertex(consumer, matrix, origin, v0, r, g, b, a0);
        vertex(consumer, matrix, origin, v1, r, g, b, a1);
        vertex(consumer, matrix, origin, v2, r, g, b, a2);
        vertex(consumer, matrix, origin, v3, r, g, b, a3);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, BlockPos origin, Vec3 at, float r, float g, float b, float a) {
        consumer.addVertex(matrix, (float) (at.x - origin.getX()), (float) (at.y - origin.getY()), (float) (at.z - origin.getZ()))
                .setColor(r, g, b, Mth.clamp(a, 0, 1));
    }

    private static void flare(PoseStack pose, VertexConsumer consumer, float size, float r, float g, float b) {
        PoseStack.Pose last = pose.last();
        flareVertex(consumer, last, -size, -size, 0, 1, r, g, b);
        flareVertex(consumer, last, size, -size, 1, 1, r, g, b);
        flareVertex(consumer, last, size, size, 1, 0, r, g, b);
        flareVertex(consumer, last, -size, size, 0, 0, r, g, b);
    }

    private static void flareVertex(VertexConsumer consumer, PoseStack.Pose last, float x, float y, float u, float v, float r, float g, float b) {
        consumer.addVertex(last.pose(), x, y, 0)
                .setColor(r, g, b, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(last, 0, 1, 0);
    }

    // ------------------------------------------------------------------------------------------------
    // Visibility
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean shouldRenderOffScreen(FixtureBlockEntity fixture) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    /** The beam reaches far beyond the block. */
    @Override
    public AABB getRenderBoundingBox(FixtureBlockEntity fixture) {
        return new AABB(fixture.getBlockPos()).inflate(fixture.type().range());
    }
}
