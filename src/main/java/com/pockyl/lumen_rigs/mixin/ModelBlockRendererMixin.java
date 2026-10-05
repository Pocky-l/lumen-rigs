package com.pockyl.lumen_rigs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.pockyl.lumen_rigs.client.light.TintingConsumer;

/** Colors the vertices of blocks reached by fixture light while chunk sections are meshed. */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
    @WrapOperation(method = "putQuadData", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                    + "Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"))
    private void lumen_rigs$tint(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red,
            float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readAlpha, Operation<Void> original,
            BlockAndTintGetter level, BlockState state, BlockPos pos) {
        original.call(TintingConsumer.wrap(consumer, pos, quad, lightmap), pose, quad, brightness, red, green, blue, alpha, lightmap,
                packedOverlay, readAlpha);
    }
}
