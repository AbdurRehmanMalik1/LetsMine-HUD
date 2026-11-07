package net.firstwemine1.indicatorbars.craft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Fills the crafting grid the way the recipe book does, and nothing more: the result waits in the output
 * slot for the player to take. Whatever was in the grid goes back to the inventory first.
 */
public final class QuickCrafter {
	private QuickCrafter() {
	}

	/** {@code max}: as many crafts as the materials allow, otherwise one more. False if it can't right now. */
	public static boolean fill(Minecraft minecraft, RecipeBookMenu<?, ?> menu, RecipeHolder<?> recipe, boolean max) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null || player.isSpectator() || player.containerMenu != menu) {
			return false;
		}
		if (!menu.getCarried().isEmpty()) {
			player.displayClientMessage(Component.translatable("indicatorbars.sort.cursor"), true);
			return false;
		}
		minecraft.gameMode.handlePlaceRecipe(menu.containerId, recipe, max);
		return true;
	}
}
