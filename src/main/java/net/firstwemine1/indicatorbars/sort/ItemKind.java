package net.firstwemine1.indicatorbars.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * What an item is, in the order the type metric lists them. Read from tags and components rather than
 * a list of items, so modded items land in the right place. Tags come first: axes and swords carry both
 * tool and weapon components, and the tags say which they are.
 */
public enum ItemKind {
	WEAPON, TOOL, ARMOR, BLOCK, FOOD, OTHER;

	/** Block ids ending in these sort after plain blocks, each shape together. */
	private static final String[] SHAPES = {"_stairs", "_slab", "_wall", "_fence"};

	public static ItemKind of(ItemStack stack) {
		if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
			return TOOL;
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS) || stack.is(ItemTags.ARROWS)
				|| stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
				|| stack.is(ItemTags.TRIDENT_ENCHANTABLE) || stack.is(ItemTags.MACE_ENCHANTABLE)
				|| stack.has(DataComponents.WEAPON) || stack.has(DataComponents.BLOCKS_ATTACKS)) {
			return WEAPON;
		}
		if (stack.has(DataComponents.TOOL) || stack.is(ItemTags.FISHING_ENCHANTABLE)) {
			return TOOL;
		}
		boolean block = stack.getItem() instanceof BlockItem;
		// A carved pumpkin can be worn, but it's a block.
		if (stack.is(ItemTags.ARMOR_ENCHANTABLE) || !block && stack.has(DataComponents.EQUIPPABLE)) {
			return ARMOR;
		}
		if (block) {
			return BLOCK;
		}
		return stack.has(DataComponents.FOOD) ? FOOD : OTHER;
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
