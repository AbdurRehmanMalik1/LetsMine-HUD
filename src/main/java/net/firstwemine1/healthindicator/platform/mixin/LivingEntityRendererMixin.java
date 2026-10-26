package net.firstwemine1.healthindicator.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.healthindicator.ClientHooks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The floating bars: copy health into the render state while it is extracted, then draw the bar
 * right after vanilla submits the mob, inside the mob's own render call. Full descriptors because
 * the generic methods also have bridge methods.
 */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void healthindicator$extractBar(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		ClientHooks.extractBar(entity, state);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("TAIL"))
	private void healthindicator$submitBar(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		ClientHooks.submitBar(state, poseStack, collector, camera);
	}
}
