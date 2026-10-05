package com.pockyl.lumen_rigs.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.client.light.ClientLighting;
import com.pockyl.lumen_rigs.client.light.MixinCheck;
import com.pockyl.lumen_rigs.client.light.veil.VeilFixtureLights;
import com.pockyl.lumen_rigs.fixture.FixtureType;
import com.pockyl.lumen_rigs.registry.ModBlockEntities;
import com.pockyl.lumen_rigs.registry.ModBlocks;

@Mod(value = LumenRigs.MOD_ID, dist = Dist.CLIENT)
public final class LumenRigsClient {
    /** Color of an unlit lens. */
    private static final int OFF_LENS = 0x383838;

    public LumenRigsClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(LumenRigsClient::registerRenderers);
        modBus.addListener(LumenRigsClient::registerModels);
        modBus.addListener(LumenRigsClient::registerBlockColors);
        modBus.addListener((FMLClientSetupEvent event) -> MixinCheck.log());
        if (ClientLighting.veilLoaded()) {
            // Only touched when Veil is installed, so its classes are never needed otherwise.
            VeilFixtureLights.register();
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FIXTURE.get(), FixtureRenderer::new);
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        for (FixtureType type : FixtureType.values()) {
            if (type.aimable()) {
                event.register(FixtureRenderer.part(type, "base"));
                event.register(FixtureRenderer.part(type, "yoke"));
                event.register(FixtureRenderer.part(type, "head"));
            }
        }
    }

    /** The soft panel is part of the chunk mesh; its diffuser takes the light's color (tint index 0). */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null || !(level.getBlockEntity(pos) instanceof FixtureBlockEntity fixture)) {
                return -1;
            }
            return fixture.effectiveBrightness() > 0 ? 0xFF000000 | fixture.settings().color() : 0xFF000000 | OFF_LENS;
        }, ModBlocks.SOFT_PANEL.get());
    }
}
