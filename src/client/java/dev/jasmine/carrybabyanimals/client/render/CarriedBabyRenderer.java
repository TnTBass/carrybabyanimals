package dev.jasmine.carrybabyanimals.client.render;

import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfig;
import dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

public final class CarriedBabyRenderer {
    private static final int LOCAL_SLEEPY_VISUAL_DELAY_TICKS = 1200;
    private static final int LOCAL_SLEEPY_VISUAL_DURATION_TICKS = 160;

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
            Vec3 heldPosition = frame.position();
            renderState.x = heldPosition.x;
            renderState.y = heldPosition.y;
            renderState.z = heldPosition.z;
            renderState.distanceToCameraSq = cameraPosition.distanceToSqr(heldPosition);
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
        if (!visualConfig.largeBabyTuckedPoseEnabled()) {
            sizeBucket = CarriedBabySizeBucket.MEDIUM;
        }
        boolean localFirstPerson = carrier == client.player && client.options.getCameraType().isFirstPerson();
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
                LOCAL_SLEEPY_VISUAL_DURATION_TICKS
        );
        boolean sleepyVisual = CarriedBabyRenderState.sleepyVisualActiveFor(
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
                        sleepyVisual,
                        true
                ))
                .orElseGet(() -> CarriedBabyVisualFrame.fromPlacement(placement));
    }
}
