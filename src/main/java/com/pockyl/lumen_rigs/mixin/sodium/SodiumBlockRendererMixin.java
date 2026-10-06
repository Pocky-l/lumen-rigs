package com.pockyl.lumen_rigs.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.lumen_rigs.client.light.FixtureLights;

/**
 * Sodium meshes chunks with its own block renderer, which skips {@code ModelBlockRendererMixin}: this colors its quads
 * the same way. The brightness needs no hook, Sodium reads it through {@code LevelRenderer.getLightColor}.
 * Only applied when Sodium is installed ({@link SodiumMixinPlugin}).
 */
@Mixin(BlockRenderer.class)
abstract class SodiumBlockRendererMixin {
    // One renderer per chunk-build thread, so plain fields are enough.
    @Unique
    private final BlockPos.MutableBlockPos lumen_rigs$offset = new BlockPos.MutableBlockPos();
    @Unique
    private final float[] lumen_rigs$tints = new float[12];
    @Unique
    private boolean lumen_rigs$near;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void lumen_rigs$startBlock(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        // Vertex positions are relative to the chunk section, the block's corner is at origin: this turns them into world ones.
        lumen_rigs$offset.set(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
        lumen_rigs$near = FixtureLights.anyNear(pos);
    }

    // Runs once the vertices of a quad are filled in, before they go to the mesh (opaque and translucent alike).
    @Inject(method = "bufferQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/"
            + "BlockRenderer;attemptPassDowngrade(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
            + "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)"
            + "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;"))
    private void lumen_rigs$tint(CallbackInfo ci, @Local ChunkVertexEncoder.Vertex[] vertices, @Local ModelQuadFacing normalFace) {
        if (!lumen_rigs$near) {
            return;
        }
        Direction face = lumen_rigs$direction(normalFace);
        float[] tints = lumen_rigs$tints;
        for (int i = 0; i < 4; i++) {
            ChunkVertexEncoder.Vertex vertex = vertices[i];
            FixtureLights.tint(vertex.x + lumen_rigs$offset.getX(), vertex.y + lumen_rigs$offset.getY(), vertex.z + lumen_rigs$offset.getZ(),
                    face, vertex.light >>> 20 & 0xF, tints, i * 3);
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
