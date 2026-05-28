package dev.jasmine.carrybabyanimals.reunion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;

public final class ParentReunionFeedback {
    public void emit(ServerLevel level, ParentReunionMatch match, boolean particlesEnabled) {
        if (!particlesEnabled) {
            return;
        }
        sendHearts(level, match.baby());
        sendHearts(level, match.adult());
    }

    private static void sendHearts(ServerLevel level, Animal animal) {
        level.sendParticles(
                ParticleTypes.HEART,
                animal.getX(),
                animal.getY() + animal.getBbHeight() * 0.75D,
                animal.getZ(),
                5,
                0.25D,
                0.25D,
                0.25D,
                0.0D
        );
    }
}
