package com.pockyl.lumen_rigs.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.lumen_rigs.LumenRigs;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, LumenRigs.MOD_ID);

    /** A drifting puff of stage haze that glows where light beams pass through it. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAZE = PARTICLES.register("haze",
            () -> new SimpleParticleType(true));

    private ModParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
