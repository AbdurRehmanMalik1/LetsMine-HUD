package net.firstwemine1.indicatorbars.platform.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container window is on screen, to place buttons by its slots. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor
	int getLeftPos();

	@Accessor
	int getTopPos();
}
