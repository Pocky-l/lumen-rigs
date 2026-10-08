package com.pockyl.lumen_rigs.client.light;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;

/**
 * Passes one block quad through to the chunk mesh with each vertex's color multiplied by the fixture light reaching
 * it. Only the bulk vertex path is used for block quads, so only that one is tinted.
 */
public final class TintingConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float[] tints = new float[12];
    private int vertex;

    private TintingConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    /** The consumer to put {@code quad} of the block at {@code pos} into: tinted when fixture light is near. */
    public static VertexConsumer wrap(VertexConsumer consumer, BlockPos pos, BakedQuad quad, int[] lightmap) {
        if (!FixtureLights.anyNear(pos)) {
            return consumer;
        }
        TintingConsumer tinting = new TintingConsumer(consumer);
        int[] data = quad.getVertices();
        int stride = data.length / 4;
        boolean tinted = false;
        for (int i = 0; i < 4; i++) {
            float x = pos.getX() + Float.intBitsToFloat(data[i * stride]);
            float y = pos.getY() + Float.intBitsToFloat(data[i * stride + 1]);
            float z = pos.getZ() + Float.intBitsToFloat(data[i * stride + 2]);
            int sky = lightmap[i] >>> 20 & 0xF;
            FixtureLights.tint(x, y, z, quad.getDirection(), sky, tinting.tints, i * 3);
            tinted |= tinting.tints[i * 3] < 1 || tinting.tints[i * 3 + 1] < 1 || tinting.tints[i * 3 + 2] < 1;
        }
        return tinted ? tinting : consumer;
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay,
            int packedLight, float normalX, float normalY, float normalZ) {
        int i = Math.min(vertex++, 3) * 3;
        delegate.vertex(x, y, z, red * tints[i], green * tints[i + 1], blue * tints[i + 2], alpha, u, v, packedOverlay, packedLight,
                normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float normalX, float normalY, float normalZ) {
        delegate.normal(normalX, normalY, normalZ);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
