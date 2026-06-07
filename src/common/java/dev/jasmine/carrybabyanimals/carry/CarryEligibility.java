package dev.jasmine.carrybabyanimals.carry;

import dev.jasmine.carrybabyanimals.config.AnimalAliasRegistry;
import dev.jasmine.carrybabyanimals.config.CarryConfig;
import dev.jasmine.carrybabyanimals.permissions.CarryPermissions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

public final class CarryEligibility {
    private final AnimalAliasRegistry aliases;

    public CarryEligibility(AnimalAliasRegistry aliases) {
        this.aliases = aliases;
    }

    public boolean canPickUp(ServerPlayer player, Entity entity, CarryConfig config) {
        if (!CarryPermissions.canCarry(player)) {
            return false;
        }
        if (!(entity instanceof Animal animal)) {
            return false;
        }
        if (!animal.isBaby()) {
            return false;
        }
        Identifier entityId = EntityType.getKey(entity.getType());
        boolean tamed = animal instanceof TamableAnimal tamable && tamable.isTame();
        boolean ownedByPlayer = animal instanceof TamableAnimal tamable && tamable.isOwnedBy(player);
        PermissionSnapshot permissions = new PermissionSnapshot(
                CarryPermissions.canCarryTamed(player),
                CarryPermissions.canCarryOthersTamed(player)
        );
        return canPickUpResolved(new CarryCandidate(entityId, tamed, ownedByPlayer), config, permissions);
    }

    boolean canPickUpResolved(CarryCandidate candidate, CarryConfig config, PermissionSnapshot permissions) {
        return pickupDecision(candidate, config, permissions) == PickupDecision.ALLOWED;
    }

    PickupDecision pickupDecision(CarryCandidate candidate, CarryConfig config, PermissionSnapshot permissions) {
        if (matchesAny(config.blockedAnimals(), candidate)) {
            return PickupDecision.BLOCKED_BY_CONFIG;
        }
        if (config.restrictToAllowedAnimals() && !matchesAny(config.allowedAnimals(), candidate)) {
            return PickupDecision.NOT_IN_ALLOWED_CONFIG;
        }
        if (!config.restrictToAllowedAnimals() && !isDefaultSupported(candidate)) {
            return PickupDecision.UNSUPPORTED_ANIMAL;
        }
        return tamedRulesDecision(candidate, config, permissions);
    }

    private boolean matchesAny(Iterable<String> names, CarryCandidate candidate) {
        for (String name : names) {
            if (aliases.resolve(name).filter(resolved -> matches(resolved, candidate)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(AnimalAliasRegistry.ResolvedAnimal resolved, CarryCandidate candidate) {
        return resolved.id().equals(candidate.entityId()) && (!resolved.requiresTamed() || candidate.tamed());
    }

    private boolean isDefaultSupported(CarryCandidate candidate) {
        return aliases.aliases().values().stream().anyMatch(resolved -> matches(resolved, candidate));
    }

    private boolean passesTamedRules(CarryCandidate candidate, CarryConfig config, PermissionSnapshot permissions) {
        return tamedRulesDecision(candidate, config, permissions) == PickupDecision.ALLOWED;
    }

    private PickupDecision tamedRulesDecision(CarryCandidate candidate, CarryConfig config, PermissionSnapshot permissions) {
        if (!candidate.tamed()) {
            return PickupDecision.ALLOWED;
        }
        if (candidate.ownedByPlayer()) {
            return permissions.canCarryTamed() ? PickupDecision.ALLOWED : PickupDecision.TAMED_PERMISSION_DENIED;
        }
        if (!config.allowCarryingOtherPlayersTamedAnimals()) {
            return PickupDecision.OTHER_TAMED_DISABLED;
        }
        return permissions.canCarryOthersTamed() ? PickupDecision.ALLOWED : PickupDecision.OTHER_TAMED_PERMISSION_DENIED;
    }

    record CarryCandidate(Identifier entityId, boolean tamed, boolean ownedByPlayer) {
    }

    record PermissionSnapshot(boolean canCarryTamed, boolean canCarryOthersTamed) {
    }

    enum PickupDecision {
        ALLOWED,
        BLOCKED_BY_CONFIG,
        NOT_IN_ALLOWED_CONFIG,
        UNSUPPORTED_ANIMAL,
        TAMED_PERMISSION_DENIED,
        OTHER_TAMED_DISABLED,
        OTHER_TAMED_PERMISSION_DENIED
    }
}
