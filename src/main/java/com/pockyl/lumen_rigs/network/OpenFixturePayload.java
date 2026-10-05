package com.pockyl.lumen_rigs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.lumen_rigs.LumenRigs;

/** Server to client: open the settings screen of the fixture at {@code pos}. */
public record OpenFixturePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenFixturePayload> TYPE = new Type<>(LumenRigs.id("open_fixture"));

    public static final StreamCodec<ByteBuf, OpenFixturePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenFixturePayload::pos,
            OpenFixturePayload::new);

    @Override
    public Type<OpenFixturePayload> type() {
        return TYPE;
    }
}
