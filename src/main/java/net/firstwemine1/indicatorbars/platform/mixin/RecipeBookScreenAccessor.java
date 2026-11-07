package net.firstwemine1.indicatorbars.platform.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether the recipe book is open, so the sort button can get out of its way. */
@Mixin(AbstractRecipeBookScreen.class)
public interface RecipeBookScreenAccessor {
	@Accessor
	RecipeBookComponent<?> getRecipeBookComponent();

	@Accessor
	boolean getWidthTooNarrow();
}
