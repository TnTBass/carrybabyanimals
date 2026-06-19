package dev.jasmine.carrybabyanimals.neoforge.client.network;

import dev.jasmine.carrybabyanimals.client.modstatus.ClientModStatusTracker;
import dev.jasmine.carrybabyanimals.neoforge.client.ClientCarryInteractionHandler;
import dev.jasmine.carrybabyanimals.neoforge.client.render.NeoForgeCarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.neoforge.network.NeoForgeCarryNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;

public final class NeoForgeClientCarryNetworking {
    private NeoForgeClientCarryNetworking() {
    }

    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(NeoForgeCarryNetworking.SetCarriedPayload.TYPE, (payload, context) ->
                NeoForgeCarriedBabyRenderState.set(payload.babyEntityId(), payload.carrierEntityId())
        );
        event.register(NeoForgeCarryNetworking.ClearCarriedPayload.TYPE, (payload, context) ->
                NeoForgeCarriedBabyRenderState.clear(payload.babyEntityId())
        );
        event.register(NeoForgeCarryNetworking.PetFeedbackPayload.TYPE, (payload, context) ->
                ClientCarryInteractionHandler.onPetFeedback(payload.babyEntityId())
        );
        event.register(NeoForgeCarryNetworking.ServerVersionPayload.TYPE, (payload, context) ->
                ClientModStatusTracker.onServerStatus(payload.serverStatus())
        );
    }

    public static void sendPetCarriedIfSupported() {
        if (supports(NeoForgeCarryNetworking.PetCarriedPayload.TYPE)) {
            ClientPacketDistributor.sendToServer(NeoForgeCarryNetworking.PetCarriedPayload.INSTANCE);
        }
    }

    private static boolean supports(CustomPacketPayload.Type<?> type) {
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }
        // NeoForge 26.2.0.6-beta exposes no public can-send helper; recheck this internal API before upgrading.
        NetworkPayloadSetup setup = ChannelAttributes.getPayloadSetup(Minecraft.getInstance().getConnection().getConnection());
        return setup != null && setup.getChannel(ConnectionProtocol.PLAY, type.id()) != null;
    }
}
