package dev.jasmine.carrybabyanimals.reunion;

import dev.jasmine.carrybabyanimals.config.CarryConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class ParentReunionFinder {
    public Optional<ParentReunionMatch> find(ServerLevel level, Animal baby, Vec3 dropPosition, CarryConfig config) {
        if (!config.parentReunionEnabled() || !baby.isBaby()) {
            return Optional.empty();
        }
        int radius = config.parentReunionRadiusBlocks();
        AABB searchBox = AABB.ofSize(dropPosition, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        Optional<UUID> babyOwner = ownerId(baby);
        return level.getEntitiesOfClass(Animal.class, searchBox, candidate -> candidate != baby && candidate.isAlive())
                .stream()
                .filter(candidate -> {
                    Optional<UUID> adultOwner = ownerId(candidate);
                    return compatibleCandidate(
                            entityId(baby),
                            baby.isBaby(),
                            babyOwner,
                            entityId(candidate),
                            candidate.isBaby(),
                            adultOwner,
                            dropPosition.distanceTo(candidate.position()),
                            radius
                    );
                })
                .findFirst()
                .map(adult -> new ParentReunionMatch(baby, adult, feedbackName(baby)));
    }

    static boolean compatibleCandidate(
            Identifier babyType,
            boolean babyIsBaby,
            Optional<UUID> babyOwner,
            Identifier adultType,
            boolean candidateIsBaby,
            Optional<UUID> adultOwner,
            double distance,
            int radiusBlocks
    ) {
        if (!babyIsBaby || candidateIsBaby || !babyType.equals(adultType) || distance > radiusBlocks) {
            return false;
        }
        if (babyOwner.isPresent() || adultOwner.isPresent()) {
            return babyOwner.isPresent() && babyOwner.equals(adultOwner);
        }
        return true;
    }

    private static Optional<UUID> ownerId(Animal animal) {
        if (animal instanceof TamableAnimal tamable && tamable.isTame()) {
            try {
                return Optional.ofNullable(tamable.getOwnerReference()).map(reference -> reference.getUUID());
            } catch (IllegalStateException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Identifier entityId(Animal animal) {
        return EntityType.getKey(animal.getType());
    }

    private static String feedbackName(Animal baby) {
        if (baby.hasCustomName()) {
            return baby.getDisplayName().getString();
        }
        Identifier id = entityId(baby);
        return "baby " + id.getPath();
    }
}
