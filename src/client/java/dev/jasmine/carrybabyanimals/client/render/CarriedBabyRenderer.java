package dev.jasmine.carrybabyanimals.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class CarriedBabyRenderer {
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
        for (Map.Entry<Integer, Integer> entry : carriedBabies.entrySet()) {
            Entity baby = client.level.getEntity(entry.getKey());
            Entity carrier = client.level.getEntity(entry.getValue());
            if (baby == null || carrier == null || baby == carrier || !baby.isAlive() || !carrier.isAlive()) {
                CarriedBabyRenderState.clear(entry.getKey());
                continue;
            }

            EntityRenderState renderState = dispatcher.extractEntity(baby, tickDelta);
            Vec3 heldPosition = heldPosition(carrier, baby, tickDelta);
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

    private static Vec3 heldPosition(Entity carrier, Entity baby, float tickDelta) {
        Vec3 base = carrier.getPosition(tickDelta);
        Vec3 forward = carrier.getViewVector(tickDelta);
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            horizontalForward = Vec3.directionFromRotation(0.0F, carrier.getViewYRot(tickDelta));
        }
        horizontalForward = horizontalForward.normalize();

        boolean leftMainArm = carrier instanceof LivingEntity living && living.getMainArm() == HumanoidArm.LEFT;
        return CarriedBabyPlacement.heldPosition(
                base,
                horizontalForward,
                carrier.getBbHeight(),
                baby.getBbHeight(),
                leftMainArm
        );
    }
}
