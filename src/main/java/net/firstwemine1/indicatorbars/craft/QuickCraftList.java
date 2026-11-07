package net.firstwemine1.indicatorbars.craft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.firstwemine1.indicatorbars.sort.ItemKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;

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
		private final IRecipe<?> recipe;

		Entry(Group group, ItemStack result, IRecipe<?> recipe) {
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

		public IRecipe<?> recipe() {
			return recipe;
		}
	}

	private static final Set<Item> BASICS = new HashSet<>(java.util.Arrays.asList(Items.TORCH, Items.SOUL_TORCH, Items.STICK, Items.CRAFTING_TABLE, Items.CHEST,
			Items.FURNACE, Items.LADDER, Items.FLINT_AND_STEEL));

	private QuickCraftList() {
	}

	/** Grouped, then by name. One entry per item, however many recipes make it. */
	public static List<Entry> list(Minecraft minecraft, RecipeBookContainer<?> menu) {
		ClientPlayerEntity player = minecraft.player;
		RecipeItemHelper contents = new RecipeItemHelper();
		player.inventory.fillStackedContents(contents);
		menu.fillCraftSlotsStackedContents(contents);
		List<Entry> entries = new ArrayList<>();
		Set<Item> seen = new HashSet<>();
		for (RecipeList collection : player.getRecipeBook().getCollections()) {
			for (IRecipe<?> recipe : collection.getRecipes()) {
				if (!(recipe instanceof ICraftingRecipe) || !player.getRecipeBook().contains(recipe)
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
