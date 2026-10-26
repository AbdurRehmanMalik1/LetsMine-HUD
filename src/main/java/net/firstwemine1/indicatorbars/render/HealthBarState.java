package net.firstwemine1.indicatorbars.render;

/**
 * Mixed into {@code LivingEntityRenderState}. Since 1.21.4 the renderer only sees the render state, not
 * the entity, so the bar's data is copied in while the state is extracted.
 */
public interface HealthBarState {
	Bar indicatorbars$bar();

	final class Bar {
		/** Whether this mob is in the target panel, so its model is captured for the portrait. */
		public boolean target;
		public int entityId;
		public boolean visible;
		public float fraction;
		public int color;
	}
}
