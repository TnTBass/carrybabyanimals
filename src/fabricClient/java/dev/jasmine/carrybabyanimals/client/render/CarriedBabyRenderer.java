package dev.jasmine.carrybabyanimals.client.render;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfig;
import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class CarriedBabyRenderer {
    private static final int LOCAL_SLEEPY_VISUAL_DELAY_TICKS = 1200;
    private static final int LOCAL_ASLEEP_VISUAL_DELAY_TICKS = 160;

    private CarriedBabyRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(CarriedBabyRenderer::collectSubmits);
    }

    private static void collectSubmits(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || context.levelState().cameraRenderState == null) {
            return;
        }
        CarriedBabyRenderState.rememberLevel(client.level, entityId -> client.level.getEntity(entityId) != null);

        Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
        if (cameraPosition == null) {
            return;
        }

        CarriedBabyRenderState.pruneMissingEntities(entityId -> client.level.getEntity(entityId) != null);
        Map<Integer, Integer> carriedBabies = CarriedBabyRenderState.carriedBabies();
        if (carriedBabies.isEmpty()) {
            return;
        }

        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        ClientCarryVisualConfig visualConfig = ClientCarryVisualConfigManager.config();
        for (Map.Entry<Integer, Integer> entry : carriedBabies.entrySet()) {
            Entity baby = client.level.getEntity(entry.getKey());
            Entity carrier = client.level.getEntity(entry.getValue());
            if (baby == null || carrier == null || baby == carrier || !baby.isAlive() || !carrier.isAlive()) {
                CarriedBabyRenderState.clear(entry.getKey());
                continue;
            }

            EntityRenderState renderState = dispatcher.extractEntity(baby, tickDelta);
            CarriedBabyVisualFrame frame = visualFrame(client, carrier, baby, tickDelta, visualConfig);
            if (frame.suppressForLocalFirstPerson()) {
                continue;
            }
            applyVisualFrame(renderState, frame, cameraPosition);
            renderState.nameTag = null;
            renderState.scoreText = null;
            renderState.shadowRadius = 0.0F;
            renderState.shadowPieces.clear();
            renderState.setData(CarriedBabyRenderState.SUPPRESS_VANILLA_RENDER, false);

            dispatcher.submit(
                    renderState,
                    context.levelState().cameraRenderState,
                    renderState.x - cameraPosition.x,
                    renderState.y - cameraPosition.y,
                    renderState.z - cameraPosition.z,
                    context.poseStack(),
                    context.submitNodeCollector()
            );
        }
    }

    static void applyVisualFrame(EntityRenderState renderState, CarriedBabyVisualFrame frame, Vec3 cameraPosition) {
        Vec3 heldPosition = frame.position();
        renderState.x = heldPosition.x;
        renderState.y = heldPosition.y;
        renderState.z = heldPosition.z;
        renderState.distanceToCameraSq = cameraPosition.distanceToSqr(heldPosition);
        if (renderState instanceof LivingEntityRenderState livingRenderState) {
            // Frame yaw/pitch are visual deltas layered over the entity's extracted orientation.
            // LivingEntityRenderState has no roll channel, so roll remains a frame-only/future pose-stack value.
            livingRenderState.yRot += (float) frame.yawDegrees();
            livingRenderState.bodyRot += (float) frame.yawDegrees();
            if (!usesSleepyModelPose(renderState, frame.sleepyVisualPhase())) {
                livingRenderState.xRot += (float) frame.pitchDegrees();
            }
        }
        applySleepyModelPose(renderState, frame.sleepyVisualPhase());
    }

    private static boolean usesSleepyModelPose(
            EntityRenderState renderState,
            CarriedBabySleepyVisualPhase sleepyVisualPhase
    ) {
        return sleepyVisualPhase != CarriedBabySleepyVisualPhase.ALERT && renderState instanceof FelineRenderState;
    }

    private static void applySleepyModelPose(
            EntityRenderState renderState,
            CarriedBabySleepyVisualPhase sleepyVisualPhase
    ) {
        if (!(renderState instanceof FelineRenderState felineRenderState)) {
            return;
        }

        float lieDownAmount = switch (sleepyVisualPhase) {
            case ALERT -> 0.0F;
            case SLEEPY -> 0.45F;
            case ASLEEP -> 1.0F;
        };
        if (lieDownAmount <= 0.0F) {
            return;
        }

        // Vanilla feline models and CatRenderer.setupRotations read these fields for the curled lie-down pose.
        felineRenderState.isSprinting = false;
        felineRenderState.isSitting = false;
        felineRenderState.lieDownAmount = Math.max(felineRenderState.lieDownAmount, lieDownAmount);
        felineRenderState.lieDownAmountTail = Math.max(felineRenderState.lieDownAmountTail, lieDownAmount);
        felineRenderState.relaxStateOneAmount = Math.max(felineRenderState.relaxStateOneAmount, lieDownAmount);
    }

    private static CarriedBabyVisualFrame visualFrame(
            Minecraft client,
            Entity carrier,
            Entity baby,
            float tickDelta,
            ClientCarryVisualConfig visualConfig
    ) {
        Vec3 base = carrier.getPosition(tickDelta);
        Vec3 forward = carrier.getViewVector(tickDelta);
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            horizontalForward = Vec3.directionFromRotation(0.0F, carrier.getViewYRot(tickDelta));
        }
        horizontalForward = horizontalForward.normalize();

        boolean leftMainArm = carrier instanceof LivingEntity living && living.getMainArm() == HumanoidArm.LEFT;
        CarriedBabySizeBucket sizeBucket = CarriedBabySizeClassifier.classify(
                EntityType.getKey(baby.getType()).toString(),
                baby.getBbHeight(),
                baby.getBbWidth()
        );
        boolean localFirstPerson = carrier == client.player && client.options.getCameraType().isFirstPerson();
        sizeBucket = effectiveSizeBucket(sizeBucket, localFirstPerson, visualConfig);
        CarriedBabyPlacement.PlacementResult placement = CarriedBabyPlacement.placement(
                base,
                horizontalForward,
                carrier.getBbHeight(),
                baby.getBbHeight(),
                baby.getBbWidth(),
                leftMainArm,
                baby.tickCount + tickDelta,
                sizeBucket,
                localFirstPerson,
                visualConfig.firstPersonLargeBabyVisibilityMode()
        );
        CarriedBabyRenderState.ensureLocalSleepyVisual(
                baby.getId(),
                baby.tickCount + LOCAL_SLEEPY_VISUAL_DELAY_TICKS,
                LOCAL_ASLEEP_VISUAL_DELAY_TICKS
        );
        CarriedBabySleepyVisualPhase sleepyVisualPhase = CarriedBabyRenderState.sleepyVisualPhaseFor(
                baby.getId(),
                baby.tickCount,
                visualConfig.sleepyCarryVisualsEnabled()
        );
        return CarriedBabyRenderState.localReactionFor(baby.getId())
                .filter(reactionState -> baby.tickCount - reactionState.startTick() < reactionState.durationTicks())
                .map(reactionState -> CarriedBabyVisualFrame.evaluate(
                        placement,
                        CarriedBabyReactionRegistry.reactionFor(
                                EntityType.getKey(baby.getType()).toString(),
                                visualConfig.carriedBabyReactionsEnabled(),
                                Set.copyOf(visualConfig.disabledCarriedReactionAnimals()),
                                visualConfig.animalReactionIntensity()
                        ),
                        reactionState.startTick(),
                        baby.tickCount,
                        sleepyVisualPhase,
                        true
                ))
                .orElseGet(() -> CarriedBabyVisualFrame.evaluate(
                        placement,
                        null,
                        0,
                        baby.tickCount,
                        sleepyVisualPhase,
                        true
                ));
    }

    static CarriedBabySizeBucket effectiveSizeBucket(
            CarriedBabySizeBucket classifiedSizeBucket,
            boolean localFirstPerson,
            ClientCarryVisualConfig visualConfig
    ) {
        if (localFirstPerson || visualConfig.largeBabyTuckedPoseEnabled()) {
            return classifiedSizeBucket;
        }
        return CarriedBabySizeBucket.MEDIUM;
    }
}
