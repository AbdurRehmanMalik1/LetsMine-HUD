package net.firstwemine1.indicatorbars.meter;

import com.google.gson.annotations.SerializedName;

/** Where a HUD element sits. Shared by every meter's HUD, so they all move the same way. */
public enum HudAnchor {
	@SerializedName("top_left") TOP_LEFT,
	@SerializedName("top_center") TOP_CENTER,
	@SerializedName("top_right") TOP_RIGHT,
	@SerializedName("bottom_left") BOTTOM_LEFT,
	@SerializedName("bottom_right") BOTTOM_RIGHT,
	/** Beside the hotbar, bottoms lined up, clear of the offhand slot. */
	@SerializedName("hotbar_left") HOTBAR_LEFT,
	@SerializedName("hotbar_right") HOTBAR_RIGHT;

	/** Half of vanilla's hotbar width. */
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int HOTBAR_HEIGHT = 22;
	/** Gap to the hotbar, plus room for the offhand slot on that side when it shows. */
	private static final int HOTBAR_GAP = 6;
	private static final int OFFHAND_SLOT = 29;

	/**
	 * The top-left corner for an element of {@code width} by {@code height}, offset by {@code dx, dy}
	 * (away from the anchored edge). {@code offhandLeft} / {@code offhandRight}: whether the offhand
	 * slot shows on that side of the hotbar.
	 */
	public int[] place(int guiWidth, int guiHeight, int width, int height, int dx, int dy,
			boolean offhandLeft, boolean offhandRight) {
		int centre = guiWidth / 2;
		int hotbarY = Math.min(guiHeight - (HOTBAR_HEIGHT - height) / 2 - height, guiHeight - height) - dy;
		switch (this) {
			case TOP_LEFT:
				return new int[] {dx, dy};
			case TOP_CENTER:
				return new int[] {(guiWidth - width) / 2 + dx, dy};
			case TOP_RIGHT:
				return new int[] {guiWidth - width - dx, dy};
			case BOTTOM_LEFT:
				return new int[] {dx, guiHeight - height - dy};
			case BOTTOM_RIGHT:
				return new int[] {guiWidth - width - dx, guiHeight - height - dy};
			case HOTBAR_LEFT:
				return new int[] {centre - HOTBAR_HALF_WIDTH - HOTBAR_GAP - (offhandLeft ? OFFHAND_SLOT : 0) - width - dx, hotbarY};
			default:
				return new int[] {centre + HOTBAR_HALF_WIDTH + HOTBAR_GAP + (offhandRight ? OFFHAND_SLOT : 0) + dx, hotbarY};
		}
	}
}
