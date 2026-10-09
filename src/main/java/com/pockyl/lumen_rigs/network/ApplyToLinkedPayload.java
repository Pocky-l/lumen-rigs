package com.pockyl.lumen_rigs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.item.LightingRemoteItem;

/**
 * Client to server: give every fixture linked to the held remote the settings of the fixture at {@code pos}. Only the
 * position is sent; the settings are read from that fixture on the server.
 */
public record ApplyToLinkedPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ApplyToLinkedPayload> TYPE = new Type<>(LumenRigs.id("apply_to_linked"));

    public static final StreamCodec<ByteBuf, ApplyToLinkedPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ApplyToLinkedPayload::pos,
            ApplyToLinkedPayload::new);

    @Override
    public Type<ApplyToLinkedPayload> type() {
        return TYPE;
    }

    public static void handle(ApplyToLinkedPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (ConfigureFixturePayload.mayConfigure(player, payload.pos)
                && player.level().getBlockEntity(payload.pos) instanceof FixtureBlockEntity fixture) {
            LightingRemoteItem.applyToLinked(player, fixture);
        }
    }
}
