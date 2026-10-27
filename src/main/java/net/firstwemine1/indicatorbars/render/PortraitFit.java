package net.firstwemine1.indicatorbars.render;

import net.minecraft.world.entity.LivingEntity;

/**
 * Sizes and centres the panel's portrait. Later versions measure the mob's model; on 1.16.5 a
 * {@code ModelRenderer} doesn't expose its cubes, so this uses the hitbox with its width counted 1.5
 * times, which leaves room for heads and tails that stick out past it.
 */
public final class PortraitFit {
	/**
	 * The inventory preview turns the mob by a mouse offset: body {@code atan(x / 40) * 20} degrees, head
	 * twice that, view pitch {@code atan(y / 40) * 20}. These give a three-quarter view (body 30 degrees)
	 * looking slightly down (10 degrees).
	 */
	public static final float MOUSE_X = (float) (40 * Math.tan(30.0 / 20));
	public static final float MOUSE_Y = (float) (40 * Math.tan(-10.0 / 20));

	/** How much of the box the mob fills. */
	private static final float FILL = 0.85f;
	/** Pixels per block, so tiny mobs (silverfish, endermites) don't fill the whole box. */
	private static final float MAX_SCALE = 40;
	/** How much longer than its hitbox a mob's model usually is (heads, tails). */
	private static final float HITBOX_LENGTH = 1.5f;

	/** Pixels per block, and where the entity's feet go so it is centred in the box. */
	public static final class Fit {
		public final int scale;
		public final int x;
		public final int y;

		Fit(int scale, int x, int y) {
			this.scale = scale;
			this.x = x;
			this.y = y;
		}
	}

	private PortraitFit() {
	}

	public static void reset() {
	}

	public static Fit fit(LivingEntity target, int x0, int y0, int x1, int y1) {
		float size = Math.max(target.getBbHeight(), target.getBbWidth() * HITBOX_LENGTH);
		int scale = Math.max(1, Math.round(Math.min((x1 - x0) * FILL / size, MAX_SCALE)));
		return new Fit(scale, (x0 + x1) / 2, (y0 + y1) / 2 + Math.round(target.getBbHeight() * scale / 2));
	}
}
