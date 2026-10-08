package com.pockyl.lumen_rigs.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.client.light.MixinCheck;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;
import com.pockyl.lumen_rigs.registry.ModBlocks;

@Mod.EventBusSubscriber(modid = LumenRigs.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LumenRigsClient {
    /** Color of an unlit lens. */
    private static final int OFF_LENS = 0x383838;

    private LumenRigsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MixinCheck.log();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FIXTURE.get(), FixtureRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (FixtureType type : FixtureType.values()) {
            if (type.aimable()) {
                event.register(FixtureRenderer.part(type, "base"));
                event.register(FixtureRenderer.part(type, "yoke"));
                event.register(FixtureRenderer.part(type, "head"));
            }
        }
    }

    /** The soft panel is part of the chunk mesh; its diffuser takes the light's color (tint index 0). */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null || !(level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture)) {
                return -1;
            }
            return fixture.effectiveBrightness() > 0 ? 0xFF000000 | fixture.settings().color() : 0xFF000000 | OFF_LENS;
        }, ModBlocks.SOFT_PANEL.get());
    }
}
