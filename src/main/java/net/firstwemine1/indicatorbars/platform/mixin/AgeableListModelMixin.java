package net.firstwemine1.indicatorbars.platform.mixin;

import net.firstwemine1.indicatorbars.render.AgeableParts;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Lends the model's parts and baby transforms to the portrait fit. */
@Mixin(AgeableListModel.class)
abstract class AgeableListModelMixin implements AgeableParts {
	@Shadow
	@Final
	private boolean scaleHead;
	@Shadow
	@Final
	private float babyYHeadOffset;
	@Shadow
	@Final
	private float babyZHeadOffset;
	@Shadow
	@Final
	private float babyHeadScale;
	@Shadow
	@Final
	private float babyBodyScale;
	@Shadow
	@Final
	private float bodyYOffset;

	@Shadow
	protected abstract Iterable<ModelPart> headParts();

	@Shadow
	protected abstract Iterable<ModelPart> bodyParts();

	@Override
	public Iterable<ModelPart> indicatorbars$headParts() {
		return headParts();
	}

	@Override
	public Iterable<ModelPart> indicatorbars$bodyParts() {
		return bodyParts();
	}

	@Override
	public boolean indicatorbars$scaleHead() {
		return scaleHead;
	}

	@Override
	public float indicatorbars$babyYHeadOffset() {
		return babyYHeadOffset;
	}

	@Override
	public float indicatorbars$babyZHeadOffset() {
		return babyZHeadOffset;
	}

	@Override
	public float indicatorbars$babyHeadScale() {
		return babyHeadScale;
	}

	@Override
	public float indicatorbars$babyBodyScale() {
		return babyBodyScale;
	}

	@Override
	public float indicatorbars$bodyYOffset() {
		return bodyYOffset;
	}
}
