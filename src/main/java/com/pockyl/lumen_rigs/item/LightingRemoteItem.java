package com.pockyl.lumen_rigs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.lumen_rigs.Config;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.registry.ModDataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Aims groups of fixtures. Use on a fixture to link or unlink it; use on any block to point the linked fixtures at
 * that spot; use on a mob or player to make them follow it; use into the air to point them where you look (e.g. the
 * sky). Sneak-use into the air forgets all links.
 */
public final class LightingRemoteItem extends Item {
    public static final int MAX_LINKS = 64;

    public LightingRemoteItem(Properties properties) {
        super(properties);
    }

    public static List<GlobalPos> links(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LINKS.get(), List.of());
    }

    /** Links the fixture if it is not linked yet, unlinks it otherwise; returns whether it is linked now. */
    public static boolean toggleLink(ItemStack stack, GlobalPos fixture) {
        List<GlobalPos> links = new ArrayList<>(links(stack));
        boolean linked = !links.remove(fixture);
        if (linked) {
            if (links.size() >= MAX_LINKS) {
                return false;
            }
            links.add(fixture);
        }
        stack.set(ModDataComponents.LINKS.get(), List.copyOf(links));
        return linked;
    }

    /** Runs {@code action} on every linked fixture that is loaded in the player's dimension and within range. */
    public static int forEachLinked(ItemStack stack, Player player, Consumer<FixtureBlockEntity> action) {
        int count = 0;
        double range = Config.remoteRange();
        for (GlobalPos link : links(stack)) {
            BlockPos pos = link.pos();
            if (link.dimension() != player.level().dimension() || !player.level().isLoaded(pos)
                    || player.distanceToSqr(pos.getCenter()) > range * range) {
                continue;
            }
            if (player.level().getBlockEntity(pos) instanceof FixtureBlockEntity fixture) {
                action.accept(fixture);
                count++;
            }
        }
        return count;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof FixtureBlockEntity) {
            if (!level.isClientSide()) {
                int before = links(stack).size();
                boolean linked = toggleLink(stack, GlobalPos.of(level.dimension(), pos));
                if (linked || links(stack).size() < before) {
                    feedback(player, linked ? "linked" : "unlinked", links(stack).size());
                } else {
                    feedback(player, "too_many", MAX_LINKS);
                }
                beep(level, player, linked ? 1.6F : 1.1F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            Vec3 point = context.getClickLocation();
            int aimed = forEachLinked(stack, player, fixture -> fixture.aimAt(point));
            report(player, level, aimed, "aimed");
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide()) {
            // The stack passed here may be a copy in creative mode; change the one in the hand.
            int following = forEachLinked(player.getItemInHand(hand), player, fixture -> fixture.follow(target));
            report(player, player.level(), following, "following");
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            stack.remove(ModDataComponents.LINKS.get());
            feedback(player, "cleared", 0);
            beep(level, player, 0.8F);
            return InteractionResultHolder.success(stack);
        }
        // Far away blocks are aimed at; otherwise the fixtures shine in the looked-at direction (into the sky).
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(Config.remoteRange())), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        int aimed;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 point = hit.getLocation();
            aimed = forEachLinked(stack, player, fixture -> fixture.aimAt(point));
        } else {
            aimed = forEachLinked(stack, player, fixture -> fixture.aimAlong(look));
        }
        report(player, level, aimed, "aimed");
        return InteractionResultHolder.success(stack);
    }

    private static void report(Player player, Level level, int count, String key) {
        if (count == 0) {
            feedback(player, "none_linked", 0);
            beep(level, player, 0.6F);
        } else {
            feedback(player, key, count);
            beep(level, player, 1.3F);
        }
    }

    private static void feedback(Player player, String key, int count) {
        player.displayClientMessage(Component.translatable("item.lumen_rigs.lighting_remote." + key, count), true);
    }

    private static void beep(Level level, Player player, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.25F,
                pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.lumen_rigs.lighting_remote.links", links(stack).size()).withStyle(ChatFormatting.AQUA));
        for (int i = 1; i <= 4; i++) {
            tooltip.add(Component.translatable("item.lumen_rigs.lighting_remote.help" + i).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !links(stack).isEmpty();
    }
}
