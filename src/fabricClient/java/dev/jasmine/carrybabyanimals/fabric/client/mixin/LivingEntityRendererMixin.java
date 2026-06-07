package dev.jasmine.carrybabyanimals.fabric.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void carrybabyanimals$markCarriedBaby(
            LivingEntity entity,
            LivingEntityRenderState renderState,
            float tickDelta,
            CallbackInfo ci
    ) {
        renderState.setData(
                CarriedBabyRenderState.SUPPRESS_VANILLA_RENDER,
                CarriedBabyRenderState.isCarriedBaby(entity.getId())
        );
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carrybabyanimals$suppressVanillaCarriedBabyRender(
            LivingEntityRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(renderState.getDataOrDefault(CarriedBabyRenderState.SUPPRESS_VANILLA_RENDER, false))) {
            ci.cancel();
        }
    }
}
