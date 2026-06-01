package dev.jasmine.carrybabyanimals.client;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManager;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderer;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class CarryBabyAnimalsClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CarryBabyAnimalsClient");

    @Override
    public void onInitializeClient() {
        try {
            ClientCarryVisualConfigManager.load();
        } catch (IOException exception) {
            LOGGER.warn("Could not load Carry Baby Animals client visual config; using defaults.", exception);
        }
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.SetCarriedPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        CarriedBabyRenderState.set(payload.babyEntityId(), payload.carrierEntityId())
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.ClearCarriedPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        CarriedBabyRenderState.clear(payload.babyEntityId())
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.PetFeedbackPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientCarryInteractionHandler.onPetFeedback(payload.babyEntityId())
                )
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CarriedBabyRenderState.clearAll());
        ClientPreAttackCallback.EVENT.register(ClientCarryInteractionHandler::onPreAttack);
        CarriedBabyRenderer.register();
    }
}
