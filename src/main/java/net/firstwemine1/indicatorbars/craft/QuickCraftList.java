package net.firstwemine1.indicatorbars.craft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.firstwemine1.indicatorbars.sort.ItemKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

/**
 * What the quick-craft panel offers: the weapons, tools and basics you could craft right now in this
 * grid, from the recipes the recipe book has unlocked.
 */
public final class QuickCraftList {
	public enum Group {
		WEAPONS, TOOLS, BASICS
	}

	public static final class Entry {
		private final Group group;
		private final ItemStack result;
		private final Recipe<?> recipe;

		Entry(Group group, ItemStack result, Recipe<?> recipe) {
			this.group = group;
			this.result = result;
			this.recipe = recipe;
		}

		public Group group() {
			return group;
		}

		public ItemStack result() {
			return result;
		}

		public Recipe<?> recipe() {
			return recipe;
		}
	}

	private static final Set<Item> BASICS = new HashSet<>(java.util.Arrays.asList(Items.TORCH, Items.SOUL_TORCH, Items.STICK, Items.CRAFTING_TABLE, Items.CHEST,
			Items.FURNACE, Items.LADDER, Items.FLINT_AND_STEEL));

	private QuickCraftList() {
	}

	/** Grouped, then by name. One entry per item, however many recipes make it. */
	public static List<Entry> list(Minecraft minecraft, RecipeBookMenu<?> menu) {
		LocalPlayer player = minecraft.player;
		StackedContents contents = new StackedContents();
		player.inventory.fillStackedContents(contents);
		menu.fillCraftSlotsStackedContents(contents);
		List<Entry> entries = new ArrayList<>();
		Set<Item> seen = new HashSet<>();
		for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
			for (Recipe<?> recipe : collection.getRecipes()) {
				if (!(recipe instanceof CraftingRecipe) || !player.getRecipeBook().contains(recipe)
						|| !recipe.canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight()) || !contents.canCraft(recipe, null)) {
					continue;
				}
				ItemStack result = recipe.getResultItem();
				Group group = result.isEmpty() ? null : group(result);
				if (group != null && seen.add(result.getItem())) {
					entries.add(new Entry(group, result, recipe));
				}
			}
		}
		entries.sort(Comparator.comparing(Entry::group).thenComparing(entry -> entry.result().getHoverName().getString()));
		return entries;
	}

	/** Null: not something this panel offers. */
	private static Group group(ItemStack result) {
		if (BASICS.contains(result.getItem()) || ItemTags.PLANKS.contains(result.getItem())) {
			return Group.BASICS;
		}
		ItemKind kind = ItemKind.of(result);
		return kind == ItemKind.WEAPON ? Group.WEAPONS : kind == ItemKind.TOOL ? Group.TOOLS : null;
	}
}
