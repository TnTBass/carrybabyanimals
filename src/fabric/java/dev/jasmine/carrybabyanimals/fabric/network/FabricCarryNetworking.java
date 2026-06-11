package dev.jasmine.carrybabyanimals.fabric.network;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import dev.jasmine.carrybabyanimals.carry.CarryInteractionHandler;
import dev.jasmine.carrybabyanimals.carry.CarryManager;
import dev.jasmine.carrybabyanimals.carry.CarryState;
import dev.jasmine.carrybabyanimals.network.CarryNetworkSender;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusServerStatus;
import dev.jasmine.carrybabyanimals.internal.modstatus.VersionMismatchSeverity;
import dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatus;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class FabricCarryNetworking {
    public static final CarryNetworkSender SENDER = new CarryNetworkSender() {
        @Override
        public void sendSetCarried(ServerPlayer carrier, Entity baby) {
            FabricCarryNetworking.sendSetCarried(carrier, baby);
        }

        @Override
        public void sendClearCarried(ServerPlayer carrier, Entity baby) {
            FabricCarryNetworking.sendClearCarried(carrier, baby);
        }

        @Override
        public void sendClearCarriedToCarrier(ServerPlayer carrier, int babyEntityId) {
            FabricCarryNetworking.sendClearCarriedToCarrier(carrier, babyEntityId);
        }

        @Override
        public void sendPetFeedbackToCarrier(ServerPlayer carrier, int babyEntityId) {
            FabricCarryNetworking.sendPetFeedbackToCarrier(carrier, babyEntityId);
        }
    };

    private static boolean clientboundPlayPayloadsRegistered;
    private static boolean serverboundPlayPayloadsRegistered;

    private FabricCarryNetworking() {
    }

    public static synchronized void registerS2CPayloads() {
        if (clientboundPlayPayloadsRegistered) {
            return;
        }

        PayloadTypeRegistry.clientboundPlay().register(SetCarriedPayload.TYPE, SetCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClearCarriedPayload.TYPE, ClearCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PetFeedbackPayload.TYPE, PetFeedbackPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerVersionPayload.TYPE, ServerVersionPayload.CODEC);
        clientboundPlayPayloadsRegistered = true;
    }

    public static synchronized void registerC2SPayloads(CarryInteractionHandler interactions) {
        if (serverboundPlayPayloadsRegistered) {
            return;
        }

        PayloadTypeRegistry.serverboundPlay().register(PetCarriedPayload.TYPE, PetCarriedPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PetCarriedPayload.TYPE, (payload, context) ->
                context.server().execute(() -> interactions.onPetRequest(context.player()))
        );
        serverboundPlayPayloadsRegistered = true;
    }

    public static void sendSetCarried(ServerPlayer carrier, Entity baby) {
        sendPassengerSync(carrier, baby);
        sendToCarrierAndTracking(carrier, baby, new SetCarriedPayload(baby.getId(), carrier.getId()));
    }

    public static void sendClearCarried(ServerPlayer carrier, Entity baby) {
        sendPassengerSync(carrier, baby);
        sendToCarrierAndTracking(carrier, baby, new ClearCarriedPayload(baby.getId()));
    }

    public static void sendClearCarriedToCarrier(ServerPlayer carrier, int babyEntityId) {
        sendIfSupported(carrier, new ClearCarriedPayload(babyEntityId));
    }

    public static void sendClearCarriedToPlayer(ServerPlayer player, int babyEntityId) {
        sendIfSupported(player, new ClearCarriedPayload(babyEntityId));
    }

    public static void sendPetFeedbackToCarrier(ServerPlayer carrier, int babyEntityId) {
        sendIfSupported(carrier, new PetFeedbackPayload(babyEntityId));
    }

    public static boolean sendServerVersionIfSupported(ServerPlayer player) {
        return sendConfiguredServerVersionIfSupported(
                channel -> ServerPlayNetworking.canSend(player, ServerVersionPayload.TYPE),
                (channel, payload) -> ServerPlayNetworking.send(player, new ServerVersionPayload(payload))
        );
    }

    static boolean sendConfiguredServerVersionIfSupported(
            ModStatusVersionPayload.PayloadSupport support,
            ModStatusVersionPayload.PayloadSender sender
    ) {
        return ModStatusVersionPayload.sendServerStatusIfSupported(
                CarryBabyAnimalsModStatus.CONFIG,
                VersionMismatchSeverity.WARN,
                support,
                sender
        );
    }

    public static void sendPassengerSync(ServerPlayer carrier, Entity baby) {
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(carrier);
        Collection<ServerPlayer> carrierTrackers = PlayerLookup.tracking(carrier);
        Collection<ServerPlayer> babyTrackers = PlayerLookup.tracking(baby);
        Set<Integer> recipientIds = passengerSyncRecipientIds(
                carrier.getId(),
                carrierTrackers.stream().map(ServerPlayer::getId).collect(Collectors.toSet()),
                babyTrackers.stream().map(ServerPlayer::getId).collect(Collectors.toSet())
        );
        Set<ServerPlayer> recipients = new LinkedHashSet<>();
        if (recipientIds.contains(carrier.getId())) {
            recipients.add(carrier);
        }
        carrierTrackers.stream()
                .filter(player -> recipientIds.contains(player.getId()))
                .forEach(recipients::add);
        babyTrackers.stream()
                .filter(player -> recipientIds.contains(player.getId()))
                .forEach(recipients::add);
        recipients.forEach(player -> player.connection.send(packet));
    }

    static Set<Integer> passengerSyncRecipientIds(
            int carrierId,
            Iterable<Integer> carrierTrackerIds,
            Iterable<Integer> babyTrackerIds
    ) {
        Set<Integer> recipientIds = new LinkedHashSet<>();
        recipientIds.add(carrierId);
        carrierTrackerIds.forEach(recipientIds::add);
        babyTrackerIds.forEach(recipientIds::add);
        return recipientIds;
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

    public record PetCarriedPayload() implements CustomPacketPayload {
        public static final PetCarriedPayload INSTANCE = new PetCarriedPayload();
        public static final CustomPacketPayload.Type<PetCarriedPayload> TYPE = new CustomPacketPayload.Type<>(id("pet_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetCarriedPayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PetFeedbackPayload(int babyEntityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PetFeedbackPayload> TYPE = new CustomPacketPayload.Type<>(id("pet_feedback"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetFeedbackPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                PetFeedbackPayload::babyEntityId,
                PetFeedbackPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerVersionPayload(byte[] encodedVersion) implements CustomPacketPayload {
        static final int MAX_ENCODED_BYTES = 256;

        public static final CustomPacketPayload.Type<ServerVersionPayload> TYPE = new CustomPacketPayload.Type<>(id(CarryBabyAnimalsModStatus.PAYLOAD_PATH));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersionPayload> CODEC = StreamCodec.of(
                ServerVersionPayload::write,
                ServerVersionPayload::read
        );

        public ServerVersionPayload(String serverVersion) {
            this(ModStatusVersionPayload.encodeServerVersion(serverVersion));
        }

        public String serverVersion() {
            return ModStatusVersionPayload.decodeServerVersion(encodedVersion);
        }

        public ModStatusServerStatus serverStatus() {
            return ModStatusVersionPayload.decodeServerStatus(encodedVersion);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buffer, ServerVersionPayload payload) {
            buffer.writeByteArray(payload.encodedVersion());
        }

        private static ServerVersionPayload read(RegistryFriendlyByteBuf buffer) {
            return new ServerVersionPayload(buffer.readByteArray(MAX_ENCODED_BYTES));
        }
    }

}
