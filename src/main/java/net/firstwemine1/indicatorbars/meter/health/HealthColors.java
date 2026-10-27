package net.firstwemine1.indicatorbars.meter.health;

import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

/** The health bars' fill colour, shared by the floating bars and the target panel. */
public final class HealthColors {
	private HealthColors() {
	}

	public static int fill(LivingEntity entity, float fraction, IBConfig.HealthConfig.BarColor mode) {
		return switch (mode) {
			case GREEN -> MeterStyle.GREEN;
			case GRADIENT -> fraction < 0.5f
					? FastColor.ARGB32.lerp(fraction * 2, MeterStyle.RED, MeterStyle.YELLOW)
					: FastColor.ARGB32.lerp((fraction - 0.5f) * 2, MeterStyle.YELLOW, MeterStyle.GREEN);
			case HOSTILITY -> entity instanceof Enemy ? MeterStyle.RED : entity instanceof NeutralMob ? MeterStyle.YELLOW : MeterStyle.GREEN;
		};
	}
}
