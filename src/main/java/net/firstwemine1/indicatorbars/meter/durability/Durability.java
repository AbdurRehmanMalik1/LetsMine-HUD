package net.firstwemine1.indicatorbars.meter.durability;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/** Uses left on a tool, weapon or piece of armor. Unbreakable items are not damageable and get nothing. */
public final class Durability {
	private Durability() {
	}

	public static boolean tracked(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem();
	}

	public static int left(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamageValue();
	}

	/** At or below {@code warnPercent} of its durability. */
	public static boolean low(ItemStack stack, int warnPercent) {
		return isLow(stack.getDamageValue(), stack.getMaxDamage(), warnPercent);
	}

	static boolean isLow(int damage, int maxDamage, int warnPercent) {
		return (long) (maxDamage - damage) * 100 <= (long) maxDamage * warnPercent;
	}

	/** Vanilla's durability bar colour, green to red, so the number matches the bar under the item. */
	public static int color(ItemStack stack) {
		return ARGB.opaque(stack.getBarColor());
	}

	public static String text(ItemStack stack, DurabilityConfig.HudText mode) {
		int left = left(stack);
		return switch (mode) {
			case USES -> Integer.toString(left);
			case FRACTION -> left + "/" + stack.getMaxDamage();
			case PERCENT -> Math.round(left * 100f / stack.getMaxDamage()) + "%";
		};
	}
}
