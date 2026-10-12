package com.pockyl.lumen_rigs.client;

import net.minecraft.util.Mth;
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
        if (ClientLighting.veilUsable()) {
            // Only touched when Veil is installed, so its classes are never needed otherwise.
            VeilFixtureLights.register();
        } else if (ClientLighting.veilLoaded()) {
            LumenRigs.LOGGER.info("Veil found, but Iris is installed and Veil turns its lights off then: using the built-in light");
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
            int brightness = fixture.effectiveBrightness();
            if (brightness <= 0) {
                return 0xFF000000 | OFF_LENS;
            }
            // Pulse and strobe dim the diffuser towards its off look; the mesh is rebuilt a few times a second for it.
            return 0xFF000000 | lerpColor(OFF_LENS, fixture.lightColor(0), fixture.lightLevel(0) / brightness);
        }, ModBlocks.SOFT_PANEL.get());
    }

    private static int lerpColor(int from, int to, float amount) {
        int r = Math.round(Mth.lerp(amount, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(amount, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }
}
