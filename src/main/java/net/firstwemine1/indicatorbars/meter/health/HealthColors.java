package net.firstwemine1.indicatorbars.meter.health;

import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;

/** The health bars' fill colour, shared by the floating bars and the target panel. */
public final class HealthColors {
	private HealthColors() {
	}

	public static int fill(LivingEntity entity, float fraction, IBConfig.HealthConfig.BarColor mode) {
		switch (mode) {
			case GRADIENT:
				return fraction < 0.5f
						? MeterStyle.lerp(fraction * 2, MeterStyle.RED, MeterStyle.YELLOW)
						: MeterStyle.lerp((fraction - 0.5f) * 2, MeterStyle.YELLOW, MeterStyle.GREEN);
			case HOSTILITY:
				return entity instanceof IMob ? MeterStyle.RED : entity instanceof IAngerable ? MeterStyle.YELLOW : MeterStyle.GREEN;
			default:
				return MeterStyle.GREEN;
		}
	}
}
