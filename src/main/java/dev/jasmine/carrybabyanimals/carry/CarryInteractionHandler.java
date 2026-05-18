package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CarryInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarryInteractionHandler.class);

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

    public InteractionResult onEntityInteract(ServerPlayer player, Entity target, InteractionHand hand) {
        boolean isCarrying = carryManager.isCarrying(player.getUUID());
        boolean isSneaking = player.isShiftKeyDown();
        boolean mainHandEmpty = player.getMainHandItem().isEmpty();
        boolean offHandEmpty = player.getOffhandItem().isEmpty();
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        InteractionResult decision = entityInteractDecision(
            isCarrying,
            isMainHand,
            isSneaking,
            mainHandEmpty,
            offHandEmpty
        );
        if (shouldDropFromEntityInteract(isCarrying, isMainHand, isSneaking, mainHandEmpty, offHandEmpty)) {
            dropCurrent(player);
            showActionBar(player, dropFeedbackText());
            return InteractionResult.SUCCESS;
        }
        if (decision != InteractionResult.SUCCESS || carryManager.isCarrying(player.getUUID())) {
            logSkippedBabyAnimalPickup(player, target, "interaction_gate", isCarrying, isSneaking, mainHandEmpty, offHandEmpty);
            return decision;
        }
        if (!eligibility.canPickUp(player, target, configManager.config())) {
            logSkippedBabyAnimalPickup(player, target, eligibilityReason(player, target), isCarrying, isSneaking, mainHandEmpty, offHandEmpty);
            return InteractionResult.PASS;
        }
        if (!carryManager.beginCarry(player.getUUID(), target.getId())) {
            logSkippedBabyAnimalPickup(player, target, "already_carried_or_busy", isCarrying, isSneaking, mainHandEmpty, offHandEmpty);
            return InteractionResult.PASS;
        }
        if (!attachment.attach(player, target)) {
            carryManager.endCarry(player.getUUID());
            clearPetCooldown(player.getUUID());
            logSkippedBabyAnimalPickup(player, target, "attachment_failed", isCarrying, isSneaking, mainHandEmpty, offHandEmpty);
            return InteractionResult.PASS;
        }
        if (target instanceof Mob mob) {
            aiController.suppress(mob);
        }
        CarryNetworking.sendSetCarried(player, target);
        showActionBar(player, pickupFeedbackText(target.getDisplayName().getString()));
        LOGGER.info(
                "Carry Baby Animals pickup started: player={} target={} entityId={}",
                player.getName().getString(),
                entityTypeName(target),
                target.getId()
        );
        return InteractionResult.SUCCESS;
    }

    public InteractionResult onAttack(ServerPlayer player) {
        return onPetRequest(player);
    }

    public InteractionResult onPetRequest(ServerPlayer player) {
        if (!carryManager.isCarrying(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        long gameTime = serverLevel.getGameTime();
        UUID playerId = player.getUUID();
        int entityId = carryManager.carriedEntityId(playerId).orElse(-1);
        Entity baby = serverLevel.getEntity(entityId);
        if (baby == null) {
            carryManager.endCarry(playerId);
            clearPetCooldown(playerId);
            CarryNetworking.sendClearCarriedToCarrier(player, entityId);
            return InteractionResult.PASS;
        }
        if (canPet(playerId, gameTime, configManager.config().pettingCooldownTicks())) {
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
            Vec3 firstPersonFeedbackPosition = firstPersonPetFeedbackPosition(
                    player.getEyePosition(),
                    player.getViewVector(1.0F)
            );
            serverLevel.sendParticles(
                    player,
                    ParticleTypes.HEART,
                    false,
                    false,
                    firstPersonFeedbackPosition.x,
                    firstPersonFeedbackPosition.y,
                    firstPersonFeedbackPosition.z,
                    3,
                    0.18D,
                    0.12D,
                    0.18D,
                    0.0D
            );
            rememberPet(playerId, gameTime);
            CarryNetworking.sendPetFeedbackToCarrier(player, baby.getId());
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
            showActionBar(player, dropFeedbackText());
        }
        return result;
    }

    public InteractionResult onUseBlockWhileCarrying(ServerPlayer player, BlockState state) {
        InteractionResult result = useBlockWhileCarryingDecision(
                carryManager.isCarrying(player.getUUID()),
                player.isShiftKeyDown(),
                player.getMainHandItem().isEmpty(),
                player.getOffhandItem().isEmpty(),
                isNavigationUseBlock(state)
        );
        if (result == InteractionResult.SUCCESS) {
            dropCurrent(player);
            showActionBar(player, dropFeedbackText());
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
                LOGGER.warn(
                        "Carried baby {} was not found in {} during level-change cleanup for {}; clearing carry state",
                        carriedEntityId,
                        level.dimension(),
                        player.getName().getString()
                );
                Entity carriedElsewhere = findCarriedEntity(player, carriedEntityId);
                if (carriedElsewhere instanceof Mob mob) {
                    aiController.restore(mob);
                }
                if (carriedElsewhere != null) {
                    attachment.dropInPlace(carriedElsewhere, loadDestinationChunk);
                }
                carryManager.endCarry(player.getUUID());
                clearPetCooldown(player.getUUID());
                CarryNetworking.sendClearCarriedToCarrier(player, carriedEntityId);
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

    private void logSkippedBabyAnimalPickup(
            ServerPlayer player,
            Entity target,
            String reason,
            boolean isCarrying,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty
    ) {
        if (!(target instanceof Animal animal) || !animal.isBaby()) {
            return;
        }
        LOGGER.info(
                "Carry Baby Animals pickup skipped: reason={} player={} target={} entityId={} sneaking={} mainHandEmpty={} offHandEmpty={} carrying={}",
                reason,
                player.getName().getString(),
                entityTypeName(target),
                target.getId(),
                isSneaking,
                mainHandEmpty,
                offHandEmpty,
                isCarrying
        );
    }

    private String eligibilityReason(ServerPlayer player, Entity target) {
        if (!CarryPermissions.canCarry(player)) {
            return "carry_permission_denied";
        }
        if (!(target instanceof Animal animal)) {
            return "not_an_animal";
        }
        if (!animal.isBaby()) {
            return "not_a_baby";
        }
        Identifier entityId = EntityType.getKey(target.getType());
        boolean tamed = animal instanceof TamableAnimal tamable && tamable.isTame();
        boolean ownedByPlayer = animal instanceof TamableAnimal tamable && tamable.isOwnedBy(player);
        CarryEligibility.PermissionSnapshot permissions = new CarryEligibility.PermissionSnapshot(
                CarryPermissions.canCarryTamed(player),
                CarryPermissions.canCarryOthersTamed(player)
        );
        CarryEligibility.PickupDecision decision = eligibility.pickupDecision(
                new CarryEligibility.CarryCandidate(entityId, tamed, ownedByPlayer),
                configManager.config(),
                permissions
        );
        return decision.name().toLowerCase();
    }

    private String entityTypeName(Entity entity) {
        return EntityType.getKey(entity.getType()).toString();
    }

    private void showActionBar(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
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

    static Vec3 firstPersonPetFeedbackPosition(Vec3 eyePosition, Vec3 viewVector) {
        return eyePosition.add(viewVector.normalize().scale(0.75D)).add(0.0D, -0.15D, 0.0D);
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

    static InteractionResult useBlockWhileCarryingDecision(
            boolean isCarrying,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty,
            boolean navigationUseBlock
    ) {
        InteractionResult carriedUseDecision = useWhileCarryingDecision(
                isCarrying,
                isSneaking,
                mainHandEmpty,
                offHandEmpty
        );
        if (carriedUseDecision == InteractionResult.FAIL && navigationUseBlock) {
            return InteractionResult.PASS;
        }
        return carriedUseDecision;
    }

    static InteractionResult entityInteractDecision(
            boolean isCarrying,
            boolean isMainHand,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty
    ) {
        if (isCarrying) {
            return InteractionResult.SUCCESS;
        }
        return isMainHand && isSneaking && mainHandEmpty && offHandEmpty ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    static boolean shouldDropFromEntityInteract(
            boolean isCarrying,
            boolean isMainHand,
            boolean isSneaking,
            boolean mainHandEmpty,
            boolean offHandEmpty
    ) {
        return isCarrying && isMainHand && isSneaking && mainHandEmpty && offHandEmpty;
    }

    static String pickupFeedbackText(String targetName) {
        return "Carrying " + targetName;
    }

    static String dropFeedbackText() {
        return "Set down baby animal";
    }

    private static boolean isNavigationUseBlock(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock;
    }
}
