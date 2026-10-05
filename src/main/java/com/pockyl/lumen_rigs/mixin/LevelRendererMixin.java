package com.pockyl.lumen_rigs.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.lumen_rigs.client.light.FixtureLights;

/** Raises the block light used for rendering blocks and block entities to the fixture light at that position. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void lumen_rigs$dynamicLight(BlockAndTintGetter level, BlockState state, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        int light = FixtureLights.blockLight(pos);
        if (light > 0) {
            int packed = cir.getReturnValueI();
            int block = (packed & 0xFFFF) >> 4;
            if (light > block) {
                cir.setReturnValue(packed & 0xFFFF0000 | light << 4);
            }
        }
    }
}
