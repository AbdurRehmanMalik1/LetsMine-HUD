package net.firstwemine1.indicatorbars.craft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.item.crafting.IRecipe;

/**
 * Fills the crafting grid the way the recipe book does, and nothing more: the result waits in the output
 * slot for the player to take. Whatever was in the grid goes back to the inventory first.
 */
public final class QuickCrafter {
	private QuickCrafter() {
	}

	/** {@code max}: as many crafts as the materials allow, otherwise one more. False if it can't right now. */
	public static boolean fill(Minecraft minecraft, RecipeBookContainer<?> menu, IRecipe<?> recipe, boolean max) {
		ClientPlayerEntity player = minecraft.player;
		if (player == null || minecraft.gameMode == null || player.isSpectator() || player.containerMenu != menu) {
			return false;
		}
		if (!minecraft.player.inventory.getCarried().isEmpty()) {
			player.displayClientMessage(new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.cursor"), true);
			return false;
		}
		minecraft.gameMode.handlePlaceRecipe(menu.containerId, recipe, max);
		return true;
	}
}
