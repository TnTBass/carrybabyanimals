package dev.jasmine.carrybabyanimals.cozy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;

public class CozyFeedbackEmitter {
    public void playIdleSound(Animal baby) {
        baby.playAmbientSound();
    }

    public void showSleepyMessage(ServerPlayer carrier, String message) {
        carrier.sendSystemMessage(Component.literal(message), true);
    }

    public void spawnSleepyParticles(ServerLevel level, Animal baby) {
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                baby.getX(),
                baby.getY() + baby.getBbHeight() * 0.75D,
                baby.getZ(),
                2,
                0.2D,
                0.18D,
                0.2D,
                0.0D
        );
    }
}
