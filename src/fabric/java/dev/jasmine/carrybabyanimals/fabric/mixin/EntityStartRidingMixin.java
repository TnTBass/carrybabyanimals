package dev.jasmine.carrybabyanimals.fabric.mixin;

import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityStartRidingMixin {
    @Redirect(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"
            )
    )
    private boolean carrybabyanimals$allowCarriedBabyToRidePlayer(
            EntityType<?> entityType,
            Entity entityToRide,
            boolean force,
            boolean sendEventAndTriggers
    ) {
        Entity passenger = (Entity) (Object) this;
        if (entityToRide instanceof Player && CarryAttachment.isExpectedPlayerPassengerAttachment(passenger.getId())) {
            return true;
        }
        return entityType.canSerialize();
    }
}
