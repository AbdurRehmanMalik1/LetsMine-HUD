package net.firstwemine1.indicatorbars.sort;

import net.minecraft.util.registry.Registry;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShootableItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;

/**
 * What an item is, in the order the type metric lists them. Read from the item's class, so modded items
 * that extend the vanilla ones land in the right place.
 */
public enum ItemKind {
	WEAPON, TOOL, ARMOR, BLOCK, FOOD, OTHER;

	/** Block ids ending in these sort after plain blocks, each shape together. */
	private static final String[] SHAPES = {"_stairs", "_slab", "_wall", "_fence"};

	public static ItemKind of(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem
				|| item instanceof ShearsItem || item instanceof FishingRodItem) {
			return TOOL;
		}
		if (item instanceof SwordItem || item instanceof ShootableItem || item instanceof TridentItem
				|| item instanceof ArrowItem || item instanceof ShieldItem) {
			return WEAPON;
		}
		if (item instanceof ArmorItem || item instanceof ElytraItem) {
			return ARMOR;
		}
		if (item instanceof BlockItem) {
			return BLOCK;
		}
		return item.isEdible() ? FOOD : OTHER;
	}

	/** 0 for a plain block, then one number per shape in {@link #SHAPES}. */
	public static int shape(ItemStack stack) {
		String path = Registry.ITEM.getKey(stack.getItem()).getPath();
		for (int i = 0; i < SHAPES.length; i++) {
			if (path.endsWith(SHAPES[i])) {
				return i + 1;
			}
		}
		return 0;
	}
}
