package com.pockyl.lumen_rigs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;

/** While a lighting remote is held, its linked fixtures are outlined, so it is clear what it will move. */
@Mod.EventBusSubscriber(modid = LumenRigs.MOD_ID, value = Dist.CLIENT)
public final class RemoteOverlay {
    private static final double MAX_DISTANCE = 128;

    private RemoteOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        ItemStack remote = heldRemote(player);
        if (remote.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.65F + 0.35F * (float) Math.sin((player.tickCount + event.getPartialTick()) * 0.2);
        for (GlobalPos link : LightingRemoteItem.links(remote)) {
            if (link.dimension() != player.level().dimension() || link.pos().getCenter().distanceTo(camera) > MAX_DISTANCE) {
                continue;
            }
            AABB box = new AABB(link.pos()).inflate(0.03).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(pose, lines, box, 0.35F, 0.85F, 1.0F, pulse);
        }
        buffers.endBatch(RenderType.lines());
    }

    private static ItemStack heldRemote(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof LightingRemoteItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
