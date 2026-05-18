package dev.jasmine.carrybabyanimals.client;

import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderer;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CarryBabyAnimalsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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
