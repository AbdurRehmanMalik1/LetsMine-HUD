package net.firstwemine1.indicatorbars.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

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
		if (item instanceof SwordItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem
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
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		for (int i = 0; i < SHAPES.length; i++) {
			if (path.endsWith(SHAPES[i])) {
				return i + 1;
			}
		}
		return 0;
	}
}
