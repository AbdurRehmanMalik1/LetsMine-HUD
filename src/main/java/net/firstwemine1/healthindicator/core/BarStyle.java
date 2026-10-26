package net.firstwemine1.healthindicator.core;

import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

/** Bar colours, shared by the floating bars and the target panel. */
public final class BarStyle {
	public static final int GREEN = 0xFF3FC93F;
	public static final int YELLOW = 0xFFE8C832;
	public static final int RED = 0xFFD83A3A;
	/** Behind the floating bars. */
	public static final int BACKGROUND = 0xB0000000;
	/** The missing part of the panel's bar. */
	public static final int MISSING = 0xFF5A5A5A;

	private BarStyle() {
	}

	public static float fraction(float health, float maxHealth) {
		return maxHealth > 0 ? Math.clamp(health / maxHealth, 0, 1) : 0;
	}

	public static int fillColor(LivingEntity entity, float fraction, HIConfig.BarColor mode) {
		return switch (mode) {
			case GREEN -> GREEN;
			case GRADIENT -> fraction < 0.5f
					? ARGB.srgbLerp(fraction * 2, RED, YELLOW)
					: ARGB.srgbLerp((fraction - 0.5f) * 2, YELLOW, GREEN);
			case HOSTILITY -> entity instanceof Enemy ? RED : entity instanceof NeutralMob ? YELLOW : GREEN;
		};
	}
}
