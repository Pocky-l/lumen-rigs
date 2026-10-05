package com.pockyl.lumen_rigs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;

/** Client to server: new settings for a fixture, sent from its settings screen. */
public record ConfigureFixturePayload(BlockPos pos, FixtureSettings settings) implements CustomPacketPayload {
    public static final Type<ConfigureFixturePayload> TYPE = new Type<>(LumenRigs.id("configure_fixture"));
    /** Players further away than this cannot change a fixture (blocks, squared). */
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public static final StreamCodec<ByteBuf, ConfigureFixturePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureFixturePayload::pos,
            FixtureSettings.STREAM_CODEC, ConfigureFixturePayload::settings,
            ConfigureFixturePayload::new);

    @Override
    public Type<ConfigureFixturePayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureFixturePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.isSpectator() || !player.mayBuild() || player.distanceToSqr(payload.pos.getCenter()) > MAX_DISTANCE_SQR
                || !player.level().isLoaded(payload.pos)) {
            return;
        }
        if (player.level().getBlockEntity(payload.pos) instanceof FixtureBlockEntity fixture) {
            fixture.applySettings(payload.settings);
        }
    }
}
