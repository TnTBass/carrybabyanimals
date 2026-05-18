package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CarryInteractionHandler {
    private final CarryManager carryManager;
    private final CarryEligibility eligibility;
    private final CarryConfigManager configManager;
    private final CarryAttachment attachment;
    private final CarryAiController aiController;
    private final Map<UUID, Long> lastPetTick = new HashMap<>();

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
        InteractionResult decision = entityInteractDecision(
                carryManager.isCarrying(player.getUUID()),
                player.isShiftKeyDown(),
                player.getMainHandItem().isEmpty(),
                player.getOffhandItem().isEmpty()
        );
        if (decision != InteractionResult.SUCCESS || carryManager.isCarrying(player.getUUID())) {
            return decision;
        }
        if (!eligibility.canPickUp(player, target, configManager.config())) {
            return InteractionResult.PASS;
        }
        if (!carryManager.beginCarry(player.getUUID(), target.getId())) {
            return InteractionResult.PASS;
        }
        if (!attachment.attach(player, target)) {
            carryManager.endCarry(player.getUUID());
            clearPetCooldown(player.getUUID());
            return InteractionResult.PASS;
        }
        if (target instanceof Mob mob) {
            aiController.suppress(mob);
        }
        CarryNetworking.sendSetCarried(player, target);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult onAttack(ServerPlayer player) {
        if (!carryManager.isCarrying(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        long gameTime = serverLevel.getGameTime();
        UUID playerId = player.getUUID();
        if (canPet(playerId, gameTime, configManager.config().pettingCooldownTicks())) {
            carryManager.carriedEntityId(playerId).ifPresent(entityId -> {
                Entity baby = serverLevel.getEntity(entityId);
                if (baby == null) {
                    carryManager.endCarry(playerId);
                    clearPetCooldown(playerId);
                    CarryNetworking.sendClearCarriedToCarrier(player, entityId);
                    return;
                }

                serverLevel.sendParticles(
                        ParticleTypes.HEART,
                        baby.getX(),
                        baby.getY() + baby.getBbHeight() * 0.75D,
                        baby.getZ(),
                        5,
                        0.25D,
                        0.25D,
                        0.25D,
                        0.0D
                );
                rememberPet(playerId, gameTime);
            });
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResult onUseWhileCarrying(ServerPlayer player) {
        InteractionResult result = useWhileCarryingDecision(
                carryManager.isCarrying(player.getUUID()),
                player.isShiftKeyDown(),
                player.getMainHandItem().isEmpty(),
                player.getOffhandItem().isEmpty()
        );
        if (result == InteractionResult.SUCCESS) {
            dropCurrent(player);
        }
        return result;
    }

    public boolean isCarrying(ServerPlayer player) {
        return carryManager.isCarrying(player.getUUID());
    }

    public void dropCurrent(ServerPlayer player) {
        dropCurrent(player, true);
    }

    public void dropCurrent(ServerPlayer player, boolean loadDestinationChunk) {
        carryManager.carriedEntityId(player.getUUID()).ifPresent(carriedEntityId -> {
            Entity baby = findCarriedEntity(player, carriedEntityId);
            if (baby == null) {
                // The carried id is stale, so clearing manager state is the cleanup itself.
                carryManager.endCarry(player.getUUID());
                clearPetCooldown(player.getUUID());
                CarryNetworking.sendClearCarriedToCarrier(player, carriedEntityId);
                return;
            }

            if (baby instanceof Mob mob) {
                aiController.restore(mob);
            }
            attachment.dropInFront(player, baby, loadDestinationChunk);
            carryManager.endCarry(player.getUUID());
            clearPetCooldown(player.getUUID());
            CarryNetworking.sendClearCarried(player, baby);
        });
    }

    public void dropCurrentInLevel(ServerPlayer player, Level level, boolean loadDestinationChunk) {
        carryManager.carriedEntityId(player.getUUID()).ifPresent(carriedEntityId -> {
            Entity baby = level.getEntity(carriedEntityId);
            if (baby == null) {
                dropCurrent(player, loadDestinationChunk);
                return;
            }

            if (baby instanceof Mob mob) {
                aiController.restore(mob);
            }
            attachment.dropInPlace(baby, loadDestinationChunk);
            carryManager.endCarry(player.getUUID());
            clearPetCooldown(player.getUUID());
            CarryNetworking.sendClearCarriedToCarrier(player, carriedEntityId);
        });
    }

    private Entity findCarriedEntity(ServerPlayer player, int carriedEntityId) {
        Entity currentLevelEntity = player.level().getEntity(carriedEntityId);
        if (currentLevelEntity != null) {
            return currentLevelEntity;
        }
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(carriedEntityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    boolean canPet(UUID playerId, long gameTime, int cooldownTicks) {
        Long last = lastPetTick.get(playerId);
        return last == null || gameTime - last >= cooldownTicks;
    }

    void rememberPet(UUID playerId, long gameTime) {
        lastPetTick.put(playerId, gameTime);
    }

    void clearPetCooldown(UUID playerId) {
        lastPetTick.remove(playerId);
    }

    static InteractionResult useWhileCarryingDecision(
            boolean isCarrying,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty
    ) {
        if (!isCarrying) {
            return InteractionResult.PASS;
        }
        if (isSneaking && mainHandEmpty && offHandEmpty) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    static InteractionResult entityInteractDecision(
            boolean isCarrying,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty
    ) {
        if (isCarrying) {
            return InteractionResult.SUCCESS;
        }
        return isSneaking && mainHandEmpty && offHandEmpty ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
