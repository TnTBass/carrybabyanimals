package dev.jasmine.carrybabyanimals.neoforge.mixin;

import dev.jasmine.carrybabyanimals.carry.CarryAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityStartRidingMixin {
    @Shadow
    private Entity vehicle;

    @Shadow
    public abstract boolean isPassenger();

    @Shadow
    public abstract void stopRiding();

    @Shadow
    public abstract boolean hasIndirectPassenger(Entity passenger);

    @Shadow
    public abstract void setPose(Pose pose);

    @Shadow
    protected abstract void addPassenger(Entity passenger);

    @Shadow
    protected abstract boolean couldAcceptPassenger();

    @Inject(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carrybabyanimals$attachExpectedBabyToPlayer(
            Entity entityToRide,
            boolean force,
            boolean sendEventAndTriggers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity passenger = (Entity) (Object) this;
        if (!(entityToRide instanceof Player)
                || !CarryAttachment.isExpectedPlayerPassengerAttachment(passenger.getId())) {
            return;
        }
        if (entityToRide == this.vehicle
                || !((EntityStartRidingMixin) (Object) entityToRide).couldAcceptPassenger()) {
            cir.setReturnValue(false);
            return;
        }
        if (this.hasIndirectPassenger(entityToRide)) {
            cir.setReturnValue(false);
            return;
        }
        if (this.isPassenger()) {
            this.stopRiding();
        }
        this.setPose(Pose.STANDING);
        this.vehicle = entityToRide;
        ((EntityStartRidingMixin) (Object) entityToRide).addPassenger(passenger);
        if (sendEventAndTriggers) {
            passenger.level().gameEvent(passenger, GameEvent.ENTITY_MOUNT, entityToRide.position());
        }
        cir.setReturnValue(true);
    }
}
