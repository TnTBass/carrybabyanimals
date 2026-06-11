package dev.jasmine.carrybabyanimals.neoforge.client.mixin;

import dev.jasmine.carrybabyanimals.neoforge.client.render.NeoForgeCarriedBabyRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerModelMixin {
    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void carrybabyanimals$poseCarrierArms(HumanoidRenderState renderState, CallbackInfo ci) {
        if (!(renderState instanceof AvatarRenderState avatarRenderState)
                || !NeoForgeCarriedBabyRenderState.isCarrier(avatarRenderState.id)) {
            return;
        }

        rightArm.xRot = -1.05F;
        rightArm.yRot = -0.35F;
        rightArm.zRot = 0.1F;
        leftArm.xRot = -1.05F;
        leftArm.yRot = 0.35F;
        leftArm.zRot = -0.1F;
    }
}
