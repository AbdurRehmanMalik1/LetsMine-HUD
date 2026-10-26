package net.firstwemine1.indicatorbars.meter.durability;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What you wear and hold, tick to tick: plays the warning once when an item wears down past
 * {@link DurabilityConfig#warnPercent}. Swapping in an already worn item doesn't count.
 */
public class EquipmentTracker {
	/** The slots the durability HUD lists, top to bottom. */
	public static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	/** A low bell, so it reads as a warning. */
	private static final float WARN_PITCH = 0.6f;
	private static final float WARN_VOLUME = 0.8f;

	private final Item[] items = new Item[SLOTS.length];
	private final int[] damage = new int[SLOTS.length];
	/** False until the first tick in a world, so items already worn when you join stay quiet. */
	private boolean primed;

	public void tick(Minecraft minecraft, DurabilityConfig config) {
		Player player = minecraft.player;
		if (player == null) {
			reset();
			return;
		}
		boolean warn = false;
		for (int i = 0; i < SLOTS.length; i++) {
			ItemStack stack = player.getItemBySlot(SLOTS[i]);
			boolean tracked = Durability.tracked(stack);
			if (primed && tracked && stack.getItem() == items[i] && stack.getDamageValue() > damage[i]
					&& Durability.low(stack, config.warnPercent)
					&& !Durability.isLow(damage[i], stack.getMaxDamage(), config.warnPercent)) {
				warn = true;
			}
			items[i] = tracked ? stack.getItem() : null;
			damage[i] = tracked ? stack.getDamageValue() : 0;
		}
		primed = true;
		if (warn && config.warnSound) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, WARN_PITCH, WARN_VOLUME));
		}
	}

	public void reset() {
		primed = false;
	}
}
