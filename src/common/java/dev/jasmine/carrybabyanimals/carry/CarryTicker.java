package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.cozy.CozyFeedbackScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

public final class CarryTicker {
    private final CarryManager carryManager;
    private final CarryInteractionHandler interactions;
    private final CarryConfigManager configManager;
    private final CozyFeedbackScheduler cozyFeedbackScheduler;

    public CarryTicker(
            CarryManager carryManager,
            CarryInteractionHandler interactions,
            CarryConfigManager configManager,
            CozyFeedbackScheduler cozyFeedbackScheduler
    ) {
        this.carryManager = carryManager;
        this.interactions = interactions;
        this.configManager = configManager;
        this.cozyFeedbackScheduler = cozyFeedbackScheduler;
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            carryManager.activeCarries().entrySet().stream()
                    .filter(entry -> entry.getKey().equals(player.getUUID()))
                    .findFirst()
                    .ifPresent(entry -> {
                int entityId = entry.getValue().carriedEntityId();
                Entity entity = player.level().getEntity(entityId);
                if (!(entity instanceof Animal animal) || !animal.isBaby() || entity.getVehicle() != player) {
                    interactions.dropCurrent(player);
                    return;
                }
                cozyFeedbackScheduler.tick(player, animal, entry.getValue(), player.level().getGameTime(), configManager.config());
            });
        }
    }
}
