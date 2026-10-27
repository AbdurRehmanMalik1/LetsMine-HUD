package net.firstwemine1.indicatorbars.render;

import net.minecraft.client.model.geom.ModelPart;

/**
 * Mixed into {@code AgeableListModel}: its head and body parts and the baby transforms its render call
 * applies to them, which {@link PortraitFit} repeats to measure the model.
 */
public interface AgeableParts {
	Iterable<ModelPart> indicatorbars$headParts();

	Iterable<ModelPart> indicatorbars$bodyParts();

	boolean indicatorbars$scaleHead();

	float indicatorbars$babyYHeadOffset();

	float indicatorbars$babyZHeadOffset();

	float indicatorbars$babyHeadScale();

	float indicatorbars$babyBodyScale();

	float indicatorbars$bodyYOffset();
}
