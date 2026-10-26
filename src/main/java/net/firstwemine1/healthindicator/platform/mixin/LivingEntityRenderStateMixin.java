package net.firstwemine1.healthindicator.platform.mixin;

import net.firstwemine1.healthindicator.render.HealthBarState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements HealthBarState {
	@Unique
	private final HealthBarState.Bar healthindicator$bar = new HealthBarState.Bar();

	@Override
	public HealthBarState.Bar healthindicator$bar() {
		return healthindicator$bar;
	}
}
