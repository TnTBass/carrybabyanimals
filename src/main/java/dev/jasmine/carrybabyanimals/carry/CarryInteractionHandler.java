package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class CarryInteractionHandler {
    private final CarryManager carryManager;
    private final CarryEligibility eligibility;
    private final CarryConfigManager configManager;
    private final CarryAttachment attachment;
    private final CarryAiController aiController;

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController
    ) {
        this.carryManager = carryManager;
        this.eligibility = eligibility;
        this.configManager = configManager;
        this.attachment = attachment;
        this.aiController = aiController;
    }

    public InteractionResult onEntityInteract(ServerPlayer player, Entity target) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (carryManager.isCarrying(player.getUUID())) {
            return InteractionResult.SUCCESS;
        }
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!eligibility.canPickUp(player, target, configManager.config())) {
            return InteractionResult.PASS;
        }
        if (!carryManager.beginCarry(player.getUUID(), target.getId())) {
            return InteractionResult.PASS;
        }
        if (!attachment.attach(player, target)) {
            carryManager.endCarry(player.getUUID());
            return InteractionResult.PASS;
        }
        if (target instanceof Mob mob) {
            aiController.suppress(mob);
        }
        return InteractionResult.SUCCESS;
    }

    public void dropCurrent(ServerPlayer player) {
        carryManager.carriedEntityId(player.getUUID()).ifPresent(carriedEntityId -> {
            Entity baby = player.level().getEntity(carriedEntityId);
            if (baby == null) {
                // The carried id is stale, so clearing manager state is the cleanup itself.
                carryManager.endCarry(player.getUUID());
                return;
            }

            if (baby instanceof Mob mob) {
                aiController.restore(mob);
            }
            attachment.dropInFront(player, baby);
            carryManager.endCarry(player.getUUID());
        });
    }
}
