package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.CarryConfigManager;
import dev.jasmine.carrybabyanimals.cozy.CozyFeedbackMessageCatalog;
import dev.jasmine.carrybabyanimals.cozy.CozyFeedbackScheduler;
import dev.jasmine.carrybabyanimals.network.CarryNetworkSender;
import dev.jasmine.carrybabyanimals.nursery.NurseryMessageCatalog;
import dev.jasmine.carrybabyanimals.nursery.NurserySafetyChecker;
import dev.jasmine.carrybabyanimals.nursery.NurserySafetyDecision;
import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import dev.jasmine.carrybabyanimals.reunion.ParentReunionCooldowns;
import dev.jasmine.carrybabyanimals.reunion.ParentReunionFeedback;
import dev.jasmine.carrybabyanimals.reunion.ParentReunionFinder;
import dev.jasmine.carrybabyanimals.reunion.ParentReunionMatch;
import dev.jasmine.carrybabyanimals.reunion.ParentReunionMessageCatalog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
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
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class CarryInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarryInteractionHandler.class);

    private final CarryManager carryManager;
    private final CarryEligibility eligibility;
    private final CarryConfigManager configManager;
    private final CarryAttachment attachment;
    private final CarryAiController aiController;
    private final CozyFeedbackMessageCatalog messageCatalog;
    private final CozyFeedbackScheduler cozyFeedbackScheduler;
    private final NurserySafetyChecker nurserySafetyChecker;
    private final NurseryMessageCatalog nurseryMessageCatalog;
    private final ParentReunionFinder parentReunionFinder;
    private final ParentReunionCooldowns parentReunionCooldowns;
    private final ParentReunionFeedback parentReunionFeedback;
    private final ParentReunionMessageCatalog parentReunionMessageCatalog;
    private final CarryNetworkSender networkSender;
    private final Map<UUID, Long> lastPetTick = new HashMap<>();

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController
    ) {
        this(
                carryManager,
                eligibility,
                configManager,
                attachment,
                aiController,
                new CozyFeedbackMessageCatalog(),
                new CozyFeedbackScheduler(),
                new NurserySafetyChecker(),
                new NurseryMessageCatalog(),
                new ParentReunionFinder(),
                new ParentReunionCooldowns(),
                new ParentReunionFeedback(),
                new ParentReunionMessageCatalog(),
                CarryNetworkSender.NO_OP
        );
    }

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController,
            CozyFeedbackScheduler cozyFeedbackScheduler
    ) {
        this(carryManager, eligibility, configManager, attachment, aiController, cozyFeedbackScheduler, CarryNetworkSender.NO_OP);
    }

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController,
            CozyFeedbackScheduler cozyFeedbackScheduler,
            CarryNetworkSender networkSender
    ) {
        this(
                carryManager,
                eligibility,
                configManager,
                attachment,
                aiController,
                new CozyFeedbackMessageCatalog(),
                cozyFeedbackScheduler,
                new NurserySafetyChecker(),
                new NurseryMessageCatalog(),
                new ParentReunionFinder(),
                new ParentReunionCooldowns(),
                new ParentReunionFeedback(),
                new ParentReunionMessageCatalog(),
                networkSender
        );
    }

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController,
            CarryNetworkSender networkSender
    ) {
        this(
                carryManager,
                eligibility,
                configManager,
                attachment,
                aiController,
                new CozyFeedbackMessageCatalog(),
                new CozyFeedbackScheduler(),
                new NurserySafetyChecker(),
                new NurseryMessageCatalog(),
                new ParentReunionFinder(),
                new ParentReunionCooldowns(),
                new ParentReunionFeedback(),
                new ParentReunionMessageCatalog(),
                networkSender
        );
    }

    CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController,
            CozyFeedbackMessageCatalog messageCatalog,
            CozyFeedbackScheduler cozyFeedbackScheduler,
            NurserySafetyChecker nurserySafetyChecker,
            NurseryMessageCatalog nurseryMessageCatalog,
            ParentReunionFinder parentReunionFinder,
            ParentReunionCooldowns parentReunionCooldowns,
            ParentReunionFeedback parentReunionFeedback,
            ParentReunionMessageCatalog parentReunionMessageCatalog
    ) {
        this(
                carryManager,
                eligibility,
                configManager,
                attachment,
                aiController,
                messageCatalog,
                cozyFeedbackScheduler,
                nurserySafetyChecker,
                nurseryMessageCatalog,
                parentReunionFinder,
                parentReunionCooldowns,
                parentReunionFeedback,
                parentReunionMessageCatalog,
                CarryNetworkSender.NO_OP
        );
    }

    CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController,
            CozyFeedbackMessageCatalog messageCatalog,
            CozyFeedbackScheduler cozyFeedbackScheduler,
            NurserySafetyChecker nurserySafetyChecker,
            NurseryMessageCatalog nurseryMessageCatalog,
            ParentReunionFinder parentReunionFinder,
            ParentReunionCooldowns parentReunionCooldowns,
            ParentReunionFeedback parentReunionFeedback,
            ParentReunionMessageCatalog parentReunionMessageCatalog,
            CarryNetworkSender networkSender
    ) {
        this.carryManager = carryManager;
        this.eligibility = eligibility;
        this.configManager = configManager;
        this.attachment = attachment;
        this.aiController = aiController;
        this.messageCatalog = messageCatalog;
        this.cozyFeedbackScheduler = cozyFeedbackScheduler;
        this.nurserySafetyChecker = nurserySafetyChecker;
        this.nurseryMessageCatalog = nurseryMessageCatalog;
        this.parentReunionFinder = parentReunionFinder;
        this.parentReunionCooldowns = parentReunionCooldowns;
        this.parentReunionFeedback = parentReunionFeedback;
        this.parentReunionMessageCatalog = parentReunionMessageCatalog;
        this.networkSender = networkSender == null ? CarryNetworkSender.NO_OP : networkSender;
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
            dropCurrentWithFeedback(player);
            return InteractionResult.SUCCESS;
        }
        if (decision != InteractionResult.SUCCESS || carryManager.isCarrying(player.getUUID())) {
            return decision;
        }
        if (!eligibility.canPickUp(player, target, configManager.config())) {
            return InteractionResult.PASS;
        }
        OptionalLong startedAtTick = pickupStartedAtTick(player.level());
        if (startedAtTick.isEmpty()) {
            LOGGER.warn("Cannot start carrying baby {} for {} outside a server level", target.getId(), player.getName().getString());
            return InteractionResult.PASS;
        }
        if (!carryManager.beginCarry(player.getUUID(), target.getId(), startedAtTick.getAsLong())) {
            return InteractionResult.PASS;
        }
        if (!attachment.attach(player, target)) {
            carryManager.endCarry(player.getUUID());
            clearCarryFeedbackState(player.getUUID());
            return InteractionResult.PASS;
        }
        if (target instanceof Mob mob) {
            aiController.suppress(mob);
        }
        networkSender.sendSetCarried(player, target);
        showActionBar(player, pickupFeedbackText(target.getDisplayName().getString(), target.hasCustomName()));
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
            clearCarryFeedbackState(playerId);
            networkSender.sendClearCarriedToCarrier(player, entityId);
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
            networkSender.sendPetFeedbackToCarrier(player, baby.getId());
            showActionBar(player, messageCatalog.petMessage(
                    baby.getDisplayName().getString(),
                    baby.hasCustomName(),
                    configManager.config(),
                    (int) gameTime
            ));
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
            dropCurrentWithFeedback(player);
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
            dropCurrentWithFeedback(player);
        }
        return result;
    }

    public boolean isCarrying(ServerPlayer player) {
        return carryManager.isCarrying(player.getUUID());
    }

    public void dropCurrent(ServerPlayer player) {
        dropCurrent(player, true);
    }

    private void dropCurrentWithFeedback(ServerPlayer player) {
        Optional<String> feedbackName = carriedBabyFeedbackName(player);
        Optional<Integer> carriedEntityId = carryManager.carriedEntityId(player.getUUID());
        Entity baby = carriedEntityId.map(id -> findCarriedEntity(player, id)).orElse(null);
        if (baby != null && refuseUnsafeNurseryDrop(
                player,
                feedbackName.orElse("baby animal"),
                baby,
                attachment.previewDropPosition(player, baby)
        )) {
            return;
        }
        dropCurrent(player);
        showActionBar(player, feedbackName.map(CarryInteractionHandler::dropFeedbackText).orElse(dropFeedbackText()));
        tryParentReunion(player, baby);
    }

    private boolean refuseUnsafeNurseryDrop(ServerPlayer player, String feedbackName, Entity baby, Vec3 dropPosition) {
        NurserySafetyDecision safetyDecision = nurserySafetyChecker.evaluate(
                player.level(),
                baby,
                dropPosition,
                configManager.config(),
                CarryPermissions.canBypassNursery(player)
        );
        DropAttemptDecision decision = dropAttemptDecision(true, safetyDecision, configManager.config().nurseryMessagesEnabled());
        if (decision.shouldShowRefusalMessage()) {
            var hazard = safetyDecision.hazard()
                    .orElseThrow(() -> new IllegalStateException("Nursery refusal missing hazard"));
            showActionBar(player, nurseryMessageCatalog.message(
                    hazard,
                    feedbackName,
                    (int) player.level().getGameTime()
            ));
        }
        return !decision.shouldDrop();
    }

    private void tryParentReunion(ServerPlayer player, Entity droppedBaby) {
        if (!(droppedBaby instanceof Animal baby) || !(player.level() instanceof ServerLevel serverLevel) || !baby.isAlive()) {
            return;
        }
        if (!configManager.config().parentReunionEnabled()) {
            return;
        }
        Optional<ParentReunionMatch> match = parentReunionFinder.find(
                serverLevel,
                baby,
                baby.position(),
                configManager.config()
        );
        long gameTime = serverLevel.getGameTime();
        boolean cooldownReady = parentReunionCooldowns.canReunite(
                player.getUUID(),
                baby.getUUID(),
                gameTime,
                configManager.config().parentReunionCooldownTicks()
        );
        ReunionAttemptDecision decision = reunionAttemptDecision(
                true,
                match.isPresent(),
                cooldownReady,
                configManager.config().parentReunionMessagesEnabled(),
                configManager.config().parentReunionParticlesEnabled()
        );
        if (!decision.shouldRememberCooldown()) {
            return;
        }
        ParentReunionMatch reunionMatch = match.orElseThrow();
        parentReunionCooldowns.remember(player.getUUID(), baby.getUUID(), gameTime);
        parentReunionFeedback.emit(serverLevel, reunionMatch, decision.shouldSendParticles());
        if (decision.shouldShowMessage()) {
            showActionBar(player, parentReunionMessageCatalog.message(
                    reunionMatch.babyFeedbackName(),
                    messageVariantIndex(gameTime)
            ));
        }
    }

    public void dropCurrent(ServerPlayer player, boolean loadDestinationChunk) {
        carryManager.carriedEntityId(player.getUUID()).ifPresent(carriedEntityId -> {
            Entity baby = findCarriedEntity(player, carriedEntityId);
            if (baby == null) {
                // The carried id is stale, so clearing manager state is the cleanup itself.
                carryManager.endCarry(player.getUUID());
                clearCarryFeedbackState(player.getUUID());
                networkSender.sendClearCarriedToCarrier(player, carriedEntityId);
                return;
            }

            // Cleanup drops intentionally bypass Nursery Mode so invalid carry state can always be cleared.
            if (baby instanceof Mob mob) {
                aiController.restore(mob);
            }
            attachment.dropInFront(player, baby, loadDestinationChunk);
            carryManager.endCarry(player.getUUID());
            clearCarryFeedbackState(player.getUUID());
            networkSender.sendClearCarried(player, baby);
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
                clearCarryFeedbackState(player.getUUID());
                networkSender.sendClearCarriedToCarrier(player, carriedEntityId);
                return;
            }

            if (baby instanceof Mob mob) {
                aiController.restore(mob);
            }
            attachment.dropInPlace(baby, loadDestinationChunk);
            carryManager.endCarry(player.getUUID());
            clearCarryFeedbackState(player.getUUID());
            networkSender.sendClearCarriedToCarrier(player, carriedEntityId);
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

    private Optional<String> carriedBabyFeedbackName(ServerPlayer player) {
        return carryManager.carriedEntityId(player.getUUID())
                .map(carriedEntityId -> findCarriedEntity(player, carriedEntityId))
                .map(CarryInteractionHandler::feedbackName);
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

    void clearCarryFeedbackState(UUID playerId) {
        clearPetCooldown(playerId);
        cozyFeedbackScheduler.clear(playerId);
    }

    static OptionalLong pickupStartedAtTick(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return OptionalLong.of(serverLevel.getGameTime());
        }
        return OptionalLong.empty();
    }

    static Vec3 firstPersonPetFeedbackPosition(Vec3 eyePosition, Vec3 viewVector) {
        return eyePosition.add(viewVector.normalize().scale(0.75D)).add(0.0D, -0.15D, 0.0D);
    }

    static int messageVariantIndex(long gameTime) {
        return (int) (gameTime & 0x7FFFFFFFL);
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

    static DropAttemptDecision dropAttemptDecision(
            boolean currentlyCarrying,
            NurserySafetyDecision safetyDecision,
            boolean messagesEnabled
    ) {
        if (!currentlyCarrying || safetyDecision.allowed()) {
            return new DropAttemptDecision(true, false, false);
        }
        return new DropAttemptDecision(false, true, messagesEnabled);
    }

    record DropAttemptDecision(boolean shouldDrop, boolean keepCarrying, boolean shouldShowRefusalMessage) {
    }

    static ReunionAttemptDecision reunionAttemptDecision(
            boolean dropSucceeded,
            boolean matchFound,
            boolean cooldownReady,
            boolean messagesEnabled,
            boolean particlesEnabled
    ) {
        if (!dropSucceeded || !matchFound || !cooldownReady) {
            return new ReunionAttemptDecision(false, false, false);
        }
        return new ReunionAttemptDecision(particlesEnabled, messagesEnabled, true);
    }

    record ReunionAttemptDecision(boolean shouldSendParticles, boolean shouldShowMessage, boolean shouldRememberCooldown) {
    }

    private static String feedbackName(Entity baby) {
        return feedbackName(baby.getDisplayName().getString(), baby.hasCustomName());
    }

    static String feedbackName(String displayName, boolean hasCustomName) {
        return hasCustomName ? displayName : "baby " + displayName;
    }

    static String pickupFeedbackText(String displayName, boolean hasCustomName) {
        return "Carrying " + feedbackName(displayName, hasCustomName);
    }

    static String dropFeedbackText(String displayName, boolean hasCustomName) {
        return "Set down " + feedbackName(displayName, hasCustomName);
    }

    static String petFeedbackText(String displayName, boolean hasCustomName) {
        String feedbackName = hasCustomName ? displayName : "Baby " + displayName;
        return feedbackName + " loves you.";
    }

    private static String dropFeedbackText(String feedbackName) {
        return "Set down " + feedbackName;
    }

    static String dropFeedbackText() {
        return "Set down baby animal";
    }

    private static boolean isNavigationUseBlock(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock;
    }
}
