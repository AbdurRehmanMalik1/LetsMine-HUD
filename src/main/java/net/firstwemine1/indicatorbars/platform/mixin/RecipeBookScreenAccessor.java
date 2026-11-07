package net.firstwemine1.indicatorbars.platform.mixin;

import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether the recipe book is open, so the sort and quick-craft buttons can get out of its way. */
@Mixin({InventoryScreen.class, CraftingScreen.class})
public interface RecipeBookScreenAccessor {
	@Accessor("recipeBookComponent")
	RecipeBookComponent getRecipeBookComponent();

	@Accessor("widthTooNarrow")
	boolean getWidthTooNarrow();
}
