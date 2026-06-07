package dev.jasmine.carrybabyanimals.client;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManager;
import dev.jasmine.carrybabyanimals.client.modstatus.ClientModStatusTracker;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderer;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public final class CarryBabyAnimalsClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CarryBabyAnimalsClient");

    @Override
    public void onInitializeClient() {
        try {
            CarryBabyAnimalsModStatus.useCurrentVersion(currentVersion());
            ClientCarryVisualConfigManager.useConfigPath(FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("carrybabyanimals-client.json"));
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
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.ServerVersionPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientModStatusTracker.onServerStatus(payload.serverStatus())
                )
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientModStatusTracker.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CarriedBabyRenderState.clearAll();
            ClientModStatusTracker.onDisconnect();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientModStatusTracker.tick());
        ClientPreAttackCallback.EVENT.register(ClientCarryInteractionHandler::onPreAttack);
        CarriedBabyRenderer.register();
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer("carrybabyanimals")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
