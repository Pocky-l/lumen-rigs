package com.pockyl.lumen_rigs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.pockyl.lumen_rigs.client.light.Illumination;

/**
 * A soft puff of stage haze. It drifts, spreads and fades slowly; where a beam passes through it, it glows in the
 * light's color, which makes beams visible like in a real haze.
 */
public final class HazeParticle extends TextureSheetParticle {
    /** How often the light on the puff is re-evaluated, in ticks. */
    private static final int LIGHT_INTERVAL = 4;
    private static final float BASE_GRAY = 0.55F;
    private static final float BASE_ALPHA = 0.035F;

    private final float startSize;
    private final float endSize;
    private final float[] light = new float[3];
    private final float[] targetLight = new float[3];
    private final int phase;

    private HazeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.96F;
        this.gravity = 0;
        this.hasPhysics = true;
        this.lifetime = 200 + random.nextInt(120);
        this.startSize = 0.5F + random.nextFloat() * 0.4F;
        this.endSize = 2.2F + random.nextFloat() * 1.4F;
        this.quadSize = startSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.phase = random.nextInt(LIGHT_INTERVAL);
        pickSprite(sprites);
        setColor(BASE_GRAY, BASE_GRAY, BASE_GRAY);
        setAlpha(0);
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        quadSize = Mth.lerp((float) Math.sqrt(life), startSize, endSize);
        oRoll = roll;
        roll += 0.004F;
        // Gentle wandering, as if the air moved.
        xd += (random.nextDouble() - 0.5) * 0.002;
        zd += (random.nextDouble() - 0.5) * 0.002;
        yd += (random.nextDouble() - 0.45) * 0.0008;

        if ((age + phase) % LIGHT_INTERVAL == 0) {
            targetLight[0] = 0;
            targetLight[1] = 0;
            targetLight[2] = 0;
            Illumination.at(level, new Vec3(x, y, z), true, targetLight);
        }
        for (int i = 0; i < 3; i++) {
            light[i] += (targetLight[i] - light[i]) * 0.3F;
        }
        float lit = Math.min(1.5F, Math.max(light[0], Math.max(light[1], light[2])));
        setColor(Math.min(1, BASE_GRAY * (1 - lit) + light[0]), Math.min(1, BASE_GRAY * (1 - lit) + light[1]),
                Math.min(1, BASE_GRAY * (1 - lit) + light[2]));
        // Fade in and out over the first and last fifth of the life.
        float fade = Math.min(1, Math.min(life * 5, (1 - life) * 5));
        setAlpha(fade * (BASE_ALPHA + 0.22F * Math.min(1, lit)));
    }

    @Override
    protected int getLightColor(float partialTick) {
        float lit = Math.max(light[0], Math.max(light[1], light[2]));
        return lit > 0.05F ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy,
                double vz) {
            return new HazeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
