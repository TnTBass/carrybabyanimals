package dev.jasmine.carrybabyanimals.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface CarryNetworkSender {
    CarryNetworkSender NO_OP = new CarryNetworkSender() {
    };

    default void sendSetCarried(ServerPlayer carrier, Entity baby) {
    }

    default void sendClearCarried(ServerPlayer carrier, Entity baby) {
    }

    default void sendClearCarriedToCarrier(ServerPlayer carrier, int babyEntityId) {
    }

    default void sendPetFeedbackToCarrier(ServerPlayer carrier, int babyEntityId) {
    }
}
