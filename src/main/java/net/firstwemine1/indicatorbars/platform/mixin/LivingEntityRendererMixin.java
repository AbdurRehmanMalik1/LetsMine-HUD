package net.firstwemine1.indicatorbars.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The floating bars: drawn right after vanilla renders the mob, inside the mob's own render call. Full
 * descriptors because the generic methods also have bridge methods. Also hides the name tag in the
 * panel's portrait.
 */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("TAIL"))
	private void indicatorbars$renderBar(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
			int light, CallbackInfo ci) {
		ClientHooks.renderBar(entity, poseStack, buffers);
	}

	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void indicatorbars$hidePortraitName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (TargetPanelRenderer.drawingPortrait()) {
			cir.setReturnValue(false);
		}
	}
}
