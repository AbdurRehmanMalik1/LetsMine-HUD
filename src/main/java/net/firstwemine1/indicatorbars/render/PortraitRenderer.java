package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;

/** Mixed into {@code LivingEntityRenderer}: what {@link PortraitFit} needs to measure a mob's model. */
public interface PortraitRenderer {
	/** The renderer's own scale hook (ghasts, slimes, giants). */
	void indicatorbars$scale(LivingEntity entity, PoseStack poseStack, float partialTick);
}
