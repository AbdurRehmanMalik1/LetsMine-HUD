package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Mixed into {@code LivingEntityRenderer}: what {@link PortraitFit} needs to measure a mob's model. */
public interface PortraitRenderer {
	/** The model the renderer last drew with. Baby and variant models are chosen inside {@code submit}. */
	EntityModel<?> indicatorbars$model();

	/** The renderer's own scale hook (ghasts, slimes, giants). */
	void indicatorbars$scale(LivingEntityRenderState state, PoseStack poseStack);
}
