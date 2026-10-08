package com.pockyl.lumen_rigs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client: open the settings screen of the fixture at {@code pos}. */
public record OpenFixturePayload(BlockPos pos) {
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static OpenFixturePayload read(FriendlyByteBuf buf) {
        return new OpenFixturePayload(buf.readBlockPos());
    }
}
