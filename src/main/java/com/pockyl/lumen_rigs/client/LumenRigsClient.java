package com.pockyl.lumen_rigs.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.pockyl.lumen_rigs.LumenRigs;

@Mod(value = LumenRigs.MOD_ID, dist = Dist.CLIENT)
public final class LumenRigsClient {
    public LumenRigsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
