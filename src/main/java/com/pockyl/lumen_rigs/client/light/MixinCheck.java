package com.pockyl.lumen_rigs.client.light;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraftforge.fml.ModList;

import com.pockyl.lumen_rigs.LumenRigs;

import java.lang.reflect.Method;

/**
 * The light hooks are optional (another mod may replace the same rendering code, e.g. another chunk renderer), so a
 * missing hook never crashes the game. This logs which hooks are active, to explain a missing light or color.
 */
public final class MixinCheck {
    private MixinCheck() {
    }

    public static void log() {
        report("block light", LevelRenderer.class);
        report("block color", ModelBlockRenderer.class);
        report("entity light", EntityRenderer.class);
        if (ModList.get().isLoaded("embeddium")) {
            try {
                report("Embeddium block color",
                        Class.forName("me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"));
            } catch (ClassNotFoundException e) {
                LumenRigs.LOGGER.warn("Embeddium is installed, but its block renderer was not found; the light stays uncolored");
            }
        }
    }

    private static void report(String hook, Class<?> target) {
        boolean applied = false;
        for (Method method : target.getDeclaredMethods()) {
            applied |= method.getName().contains("lumen_rigs$");
        }
        if (applied) {
            LumenRigs.LOGGER.info("Fixture {} hook is active", hook);
        } else {
            LumenRigs.LOGGER.warn("Fixture {} hook could not be applied (another mod replaces {}); that part of the light is off",
                    hook, target.getSimpleName());
        }
    }
}
