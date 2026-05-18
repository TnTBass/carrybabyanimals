package dev.jasmine.carrybabyanimals.client;

import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderState;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacement;
import dev.jasmine.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ClientCarryInteractionHandler {
    private ClientCarryInteractionHandler() {
    }

    public static boolean onPreAttack(Minecraft client, LocalPlayer player, int clickCount) {
        if (!shouldCancelPreAttack(CarriedBabyRenderState.isCarrier(player.getId()))) {
            return false;
        }
        if (ClientPlayNetworking.canSend(CarryNetworking.PetCarriedPayload.TYPE)) {
            ClientPlayNetworking.send(CarryNetworking.PetCarriedPayload.INSTANCE);
        }
        return true;
    }

    public static void onPetFeedback(int babyEntityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (!shouldShowLocalPetFeedback(
                CarriedBabyRenderState.isCarrier(player.getId()),
                CarriedBabyRenderState.carrierFor(babyEntityId).orElse(-1) == player.getId()
        )) {
            return;
        }

        Entity baby = client.level.getEntity(babyEntityId);
        if (baby == null) {
            return;
        }
        Vec3 heldPosition = heldPosition(player, baby, client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        Vec3 particlePosition = CarriedBabyPlacement.petFeedbackPosition(heldPosition, baby.getBbHeight());
        RandomSource random = baby.getRandom();
        for (int i = 0; i < 5; i++) {
            client.level.addParticle(
                    ParticleTypes.HEART,
                    particlePosition.x + (random.nextDouble() - 0.5D) * 0.25D,
                    particlePosition.y + (random.nextDouble() - 0.5D) * 0.25D,
                    particlePosition.z + (random.nextDouble() - 0.5D) * 0.25D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    static boolean shouldCancelPreAttack(boolean localPlayerIsCarrier) {
        return localPlayerIsCarrier;
    }

    static boolean shouldShowLocalPetFeedback(boolean localPlayerIsCarrier, boolean feedbackBabyBelongsToLocalPlayer) {
        return localPlayerIsCarrier && feedbackBabyBelongsToLocalPlayer;
    }

    private static Vec3 heldPosition(Entity carrier, Entity baby, float tickDelta) {
        Vec3 base = carrier.getPosition(tickDelta);
        Vec3 forward = carrier.getViewVector(tickDelta);
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            horizontalForward = Vec3.directionFromRotation(0.0F, carrier.getViewYRot(tickDelta));
        }

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
