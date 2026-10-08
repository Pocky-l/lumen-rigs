package com.pockyl.lumen_rigs.mixin.embeddium;

import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.lumen_rigs.client.light.FixtureLights;

/**
 * Embeddium meshes chunks with its own block renderer, which skips {@code ModelBlockRendererMixin}: this colors its
 * quads the same way. The brightness needs no hook, Embeddium reads it through {@code LevelRenderer.getLightColor}.
 * Only applied when Embeddium is installed ({@link EmbeddiumMixinPlugin}).
 */
@Mixin(value = BlockRenderer.class, remap = false)
abstract class EmbeddiumBlockRendererMixin {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;
    // One renderer per chunk-build thread, so a plain field is enough.
    @Unique
    private final float[] lumen_rigs$tints = new float[12];

    // Runs once the vertices of a quad are filled in, before they go to the mesh (opaque and translucent alike).
    @Inject(method = "writeGeometry", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/"
            + "ChunkModelBuilder;getVertexBuffer(Lme/jellysquid/mods/sodium/client/model/quad/properties/ModelQuadFacing;)"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;"))
    private void lumen_rigs$tint(BlockRenderContext ctx, ChunkModelBuilder builder, Vec3 offset, Material material, BakedQuadView quad,
            int[] colors, QuadLightData light, CallbackInfo ci) {
        BlockPos pos = ctx.pos();
        if (!FixtureLights.anyNear(pos)) {
            return;
        }
        // Vertex positions are relative to the chunk section, the block's corner is at origin: this turns them into world ones.
        Vector3fc origin = ctx.origin();
        float offsetX = pos.getX() - origin.x();
        float offsetY = pos.getY() - origin.y();
        float offsetZ = pos.getZ() - origin.z();
        Direction face = lumen_rigs$direction(quad.getNormalFace());
        float[] tints = lumen_rigs$tints;
        for (int i = 0; i < 4; i++) {
            ChunkVertexEncoder.Vertex vertex = vertices[i];
            FixtureLights.tint(vertex.x + offsetX, vertex.y + offsetY, vertex.z + offsetZ, face, vertex.light >>> 20 & 0xF, tints, i * 3);
            // ABGR: red in the lowest byte.
            int color = vertex.color;
            int red = (int) ((color & 0xFF) * tints[i * 3]);
            int green = (int) ((color >>> 8 & 0xFF) * tints[i * 3 + 1]);
            int blue = (int) ((color >>> 16 & 0xFF) * tints[i * 3 + 2]);
            vertex.color = color & 0xFF000000 | blue << 16 | green << 8 | red;
        }
    }

    @Unique
    private static Direction lumen_rigs$direction(ModelQuadFacing facing) {
        return switch (facing) {
            case POS_X -> Direction.EAST;
            case NEG_X -> Direction.WEST;
            case POS_Y -> Direction.UP;
            case NEG_Y -> Direction.DOWN;
            case POS_Z -> Direction.SOUTH;
            case NEG_Z -> Direction.NORTH;
            default -> Direction.UP;
        };
    }
}
