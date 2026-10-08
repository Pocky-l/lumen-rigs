package com.pockyl.lumen_rigs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import com.pockyl.lumen_rigs.block.FixtureBlockEntity;
import com.pockyl.lumen_rigs.fixture.FixtureSettings;

import java.util.function.Supplier;

/** Client to server: new settings for a fixture, sent from its settings screen. */
public record ConfigureFixturePayload(BlockPos pos, FixtureSettings settings) {
    /** Players further away than this cannot change a fixture (blocks, squared). */
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        settings.write(buf);
    }

    public static ConfigureFixturePayload read(FriendlyByteBuf buf) {
        return new ConfigureFixturePayload(buf.readBlockPos(), FixtureSettings.read(buf));
    }

    public static void handle(ConfigureFixturePayload payload, Supplier<NetworkEvent.Context> context) {
        Player player = context.get().getSender();
        if (player == null || player.isSpectator() || !player.mayBuild() || player.distanceToSqr(payload.pos.getCenter()) > MAX_DISTANCE_SQR
                || !player.level().isLoaded(payload.pos)) {
            return;
        }
        if (player.level().getBlockEntity(payload.pos) instanceof FixtureBlockEntity fixture) {
            fixture.applySettings(payload.settings);
        }
    }
}
