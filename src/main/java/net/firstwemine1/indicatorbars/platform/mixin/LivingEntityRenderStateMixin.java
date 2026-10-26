package net.firstwemine1.indicatorbars.platform.mixin;

import net.firstwemine1.indicatorbars.render.HealthBarState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements HealthBarState {
	@Unique
	private final HealthBarState.Bar indicatorbars$bar = new HealthBarState.Bar();

	@Override
	public HealthBarState.Bar indicatorbars$bar() {
		return indicatorbars$bar;
	}
}
