package net.firstwemine1.indicatorbars.meter.durability;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvents;

/**
 * What you wear and hold, tick to tick: plays the warning once when an item wears down past
 * {@link DurabilityConfig#warnPercent}. Swapping in an already worn item doesn't count.
 */
public class EquipmentTracker {
	/** The slots the durability HUD lists, top to bottom. */
	public static final EquipmentSlotType[] SLOTS = {
			EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET,
			EquipmentSlotType.MAINHAND, EquipmentSlotType.OFFHAND};
	/** A low bell, so it reads as a warning. */
	private static final float WARN_PITCH = 0.6f;
	private static final float WARN_VOLUME = 0.8f;

	private final Item[] items = new Item[SLOTS.length];
	private final int[] damage = new int[SLOTS.length];
	/** False until the first tick in a world, so items already worn when you join stay quiet. */
	private boolean primed;

	public void tick(Minecraft minecraft, DurabilityConfig config) {
		PlayerEntity player = minecraft.player;
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
			minecraft.getSoundManager().play(SimpleSound.forUI(SoundEvents.NOTE_BLOCK_BELL, WARN_PITCH, WARN_VOLUME));
		}
	}

	public void reset() {
		primed = false;
	}
}
