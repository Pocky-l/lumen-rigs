package com.pockyl.lumen_rigs.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.pockyl.lumen_rigs.client.FixtureScreen;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ConfigureFixturePayload.TYPE, ConfigureFixturePayload.STREAM_CODEC, ConfigureFixturePayload::handle);
        registrar.playToServer(ApplyToLinkedPayload.TYPE, ApplyToLinkedPayload.STREAM_CODEC, ApplyToLinkedPayload::handle);
        // The client-bound handler lives in client code; the lambda only resolves it when a packet arrives on a client.
        registrar.playToClient(OpenFixturePayload.TYPE, OpenFixturePayload.STREAM_CODEC,
                (payload, context) -> FixtureScreen.open(payload.pos()));
    }
}
