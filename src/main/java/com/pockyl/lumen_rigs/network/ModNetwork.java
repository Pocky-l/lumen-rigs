package com.pockyl.lumen_rigs.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.pockyl.lumen_rigs.LumenRigs;
import com.pockyl.lumen_rigs.client.FixtureScreen;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(LumenRigs.id("main"), () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ConfigureFixturePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureFixturePayload::write)
                .decoder(ConfigureFixturePayload::read)
                .consumerMainThread(ConfigureFixturePayload::handle)
                .add();
        CHANNEL.messageBuilder(OpenFixturePayload.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenFixturePayload::write)
                .decoder(OpenFixturePayload::read)
                // The client-bound handler lives in client code; it is only resolved when a packet arrives on a client.
                .consumerMainThread((payload, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> FixtureScreen.open(payload.pos())))
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }
}
