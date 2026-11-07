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
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/**
 * What the quick-craft panel offers: the weapons, tools and basics you could craft right now in this
 * grid. Recipes come from the client's recipe book, the only ones a server tells the client about, so a
 * recipe that isn't unlocked yet isn't offered.
 */
public final class QuickCraftList {
	public enum Group {
		WEAPONS, TOOLS, BASICS
	}

	public record Entry(Group group, ItemStack result, RecipeDisplayEntry recipe) {
	}

	private static final Set<Item> BASICS = Set.of(Items.TORCH, Items.SOUL_TORCH, Items.STICK, Items.CRAFTING_TABLE, Items.CHEST,
			Items.FURNACE, Items.LADDER, Items.FLINT_AND_STEEL);

	private QuickCraftList() {
	}

	/** Grouped, then by name. One entry per item, however many recipes make it. */
	public static List<Entry> list(Minecraft minecraft, AbstractCraftingMenu menu) {
		LocalPlayer player = minecraft.player;
		StackedItemContents contents = contents(player, menu);
		var context = SlotDisplayContext.fromLevel(minecraft.level);
		List<Entry> entries = new ArrayList<>();
		Set<Item> seen = new HashSet<>();
		for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
			for (RecipeDisplayEntry recipe : collection.getRecipes()) {
				if (!fits(recipe.display(), menu.getGridWidth(), menu.getGridHeight()) || !recipe.canCraft(contents)) {
					continue;
				}
				List<ItemStack> results = recipe.resultItems(context);
				if (results.isEmpty() || results.get(0).isEmpty()) {
					continue;
				}
				ItemStack result = results.get(0);
				Group group = group(result);
				if (group != null && seen.add(result.getItem())) {
					entries.add(new Entry(group, result, recipe));
				}
			}
		}
		entries.sort(Comparator.comparing(Entry::group).thenComparing(entry -> entry.result().getHoverName().getString()));
		return entries;
	}

	/** Everything a recipe could draw on: the inventory, and what's already in the grid. */
	private static StackedItemContents contents(LocalPlayer player, AbstractCraftingMenu menu) {
		StackedItemContents contents = new StackedItemContents();
		player.getInventory().fillStackedContents(contents);
		menu.fillCraftSlotsStackedContents(contents);
		return contents;
	}

	/** The server refuses a recipe bigger than the grid, so the inventory's 2x2 never offers a 3x3 one. */
	private static boolean fits(RecipeDisplay display, int width, int height) {
		if (display instanceof ShapedCraftingRecipeDisplay shaped) {
			return shaped.width() <= width && shaped.height() <= height;
		}
		if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
			return shapeless.ingredients().size() <= width * height;
		}
		return false;
	}

	/** Null: not something this panel offers. */
	private static Group group(ItemStack result) {
		if (BASICS.contains(result.getItem()) || result.is(ItemTags.PLANKS)) {
			return Group.BASICS;
		}
		return switch (ItemKind.of(result)) {
			case WEAPON -> Group.WEAPONS;
			case TOOL -> Group.TOOLS;
			default -> null;
		};
	}
}
