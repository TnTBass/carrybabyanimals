package dev.jasmine.carrybabyanimals.carry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

public final class CarryTicker {
    private final CarryManager carryManager;
    private final CarryInteractionHandler interactions;

    public CarryTicker(CarryManager carryManager, CarryInteractionHandler interactions) {
        this.carryManager = carryManager;
        this.interactions = interactions;
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            carryManager.carriedEntityId(player.getUUID()).ifPresent(entityId -> {
                Entity entity = player.level().getEntity(entityId);
                if (!(entity instanceof Animal animal) || !animal.isBaby() || entity.getVehicle() != player) {
                    interactions.dropCurrent(player);
                }
            });
        }
    }
}
