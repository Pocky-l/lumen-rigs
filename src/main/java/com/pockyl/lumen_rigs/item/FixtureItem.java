package com.pockyl.lumen_rigs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.pockyl.lumen_rigs.block.FixtureBlock;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;
import com.pockyl.lumen_rigs.fixture.LightEffect;
import com.pockyl.lumen_rigs.registry.ModDataComponents;

import java.util.List;
import java.util.Locale;

/** A fixture as an item; one broken after being set up keeps its settings and shows them. */
public final class FixtureItem extends BlockItem {
    public FixtureItem(FixtureBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.get(ModDataComponents.SETTINGS);
        if (tag == null) {
            return;
        }
        FixtureSettings settings = FixtureSettings.load(tag, ((FixtureBlock) getBlock()).type());
        tooltip.add(Component.translatable("item.lumen_rigs.fixture.saved").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("■ ").withStyle(style -> style.withColor(TextColor.fromRgb(settings.color())))
                .append(Component.translatable("item.lumen_rigs.fixture.light", Math.round(settings.beam()), settings.brightness())
                        .withStyle(ChatFormatting.GRAY)));
        if (settings.effect() != LightEffect.STEADY) {
            tooltip.add(Component.translatable("item.lumen_rigs.fixture.effect",
                    Component.translatable("lumen_rigs.screen.effect." + settings.effect().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GRAY));
        }
    }
}
