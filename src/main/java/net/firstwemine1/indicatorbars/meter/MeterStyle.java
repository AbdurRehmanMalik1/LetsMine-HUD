package net.firstwemine1.indicatorbars.meter;

import net.minecraft.util.Mth;

/** Colours and helpers shared by every meter. */
public final class MeterStyle {
	public static final int GREEN = 0xFF3FC93F;
	public static final int YELLOW = 0xFFE8C832;
	public static final int RED = 0xFFD83A3A;
	/** Behind bars drawn in the world. */
	public static final int BACKGROUND = 0xB0000000;
	/** The missing part of a bar drawn on the HUD. */
	public static final int MISSING = 0xFF5A5A5A;
	/** HUD panels: fill and frame. */
	public static final int PANEL = 0xB0101010;
	public static final int FRAME = 0xFF606060;
	public static final int TEXT = 0xFFFFFFFF;

	private MeterStyle() {
	}

	/** {@code color} with its alpha scaled by {@code alpha} (26.x has this as {@code ARGB.multiplyAlpha}). */
	public static int multiplyAlpha(int color, float alpha) {
		int a = Math.round((color >>> 24) * alpha);
		return a << 24 | color & 0xFFFFFF;
	}

	/** {@code value / max}, clamped to 0..1; 0 when there is no maximum. */
	public static float fraction(float value, float max) {
		return max > 0 ? Mth.clamp(value / max, 0, 1) : 0;
	}
}
