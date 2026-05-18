package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.carry.CarryState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CarryNetworking {
    private static boolean clientboundPlayPayloadsRegistered;

    private CarryNetworking() {
    }

    public static synchronized void registerS2CPayloads() {
        if (clientboundPlayPayloadsRegistered) {
            return;
        }

        PayloadTypeRegistry.clientboundPlay().register(SetCarriedPayload.TYPE, SetCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClearCarriedPayload.TYPE, ClearCarriedPayload.CODEC);
        clientboundPlayPayloadsRegistered = true;
    }

    public static void sendSetCarried(ServerPlayer carrier, Entity baby) {
        sendToCarrierAndTracking(carrier, baby, new SetCarriedPayload(baby.getId(), carrier.getId()));
    }

    public static void sendClearCarried(ServerPlayer carrier, Entity baby) {
        sendToCarrierAndTracking(carrier, baby, new ClearCarriedPayload(baby.getId()));
    }

    public static void sendClearCarriedToCarrier(ServerPlayer carrier, int babyEntityId) {
        sendIfSupported(carrier, new ClearCarriedPayload(babyEntityId));
    }

    public static void sendClearCarriedToPlayer(ServerPlayer player, int babyEntityId) {
        sendIfSupported(player, new ClearCarriedPayload(babyEntityId));
    }

    public static void replayTrackedCarry(CarryManager carryManager, Entity trackedEntity, ServerPlayer trackingPlayer) {
        carryManager.carrierIdFor(trackedEntity.getId()).ifPresent(carrierId -> {
            ServerPlayer carrier = trackingPlayer.level().getServer().getPlayerList().getPlayer(carrierId);
            if (carrier != null) {
                sendIfSupported(trackingPlayer, new SetCarriedPayload(trackedEntity.getId(), carrier.getId()));
            }
        });
    }

    public static void replayVisibleCarries(CarryManager carryManager, ServerPlayer player) {
        for (Map.Entry<UUID, CarryState> entry : carryManager.activeCarries().entrySet()) {
            ServerPlayer carrier = player.level().getServer().getPlayerList().getPlayer(entry.getKey());
            if (carrier == null) {
                continue;
            }

            Entity baby = carrier.level().getEntity(entry.getValue().carriedEntityId());
            if (baby == null || !isVisibleCarryRecipient(player, carrier, baby)) {
                continue;
            }

            sendIfSupported(player, new SetCarriedPayload(baby.getId(), carrier.getId()));
        }
    }

    private static void sendToCarrierAndTracking(ServerPlayer carrier, Entity baby, CustomPacketPayload payload) {
        Set<ServerPlayer> recipients = new LinkedHashSet<>();
        recipients.add(carrier);
        recipients.addAll(PlayerLookup.tracking(baby));
        recipients.forEach(player -> sendIfSupported(player, payload));
    }

    private static boolean isVisibleCarryRecipient(ServerPlayer player, ServerPlayer carrier, Entity baby) {
        return player == carrier || (sameLevel(player.level(), baby.level()) && PlayerLookup.tracking(baby).contains(player));
    }

    private static boolean sameLevel(Level first, Level second) {
        return first.dimension().equals(second.dimension());
    }

    private static void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, path);
    }

    public record SetCarriedPayload(int babyEntityId, int carrierEntityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetCarriedPayload> TYPE = new CustomPacketPayload.Type<>(id("set_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetCarriedPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SetCarriedPayload::babyEntityId,
                ByteBufCodecs.VAR_INT,
                SetCarriedPayload::carrierEntityId,
                SetCarriedPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClearCarriedPayload(int babyEntityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClearCarriedPayload> TYPE = new CustomPacketPayload.Type<>(id("clear_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearCarriedPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ClearCarriedPayload::babyEntityId,
                ClearCarriedPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
