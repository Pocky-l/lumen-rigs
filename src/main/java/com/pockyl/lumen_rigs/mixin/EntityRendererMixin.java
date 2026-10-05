package com.pockyl.lumen_rigs.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.lumen_rigs.client.light.FixtureLights;

/** Mobs, players and items near a fixture are lit by it. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void lumen_rigs$dynamicLight(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int light = FixtureLights.blockLight(pos);
        if (light > cir.getReturnValueI()) {
            cir.setReturnValue(light);
        }
    }
}
