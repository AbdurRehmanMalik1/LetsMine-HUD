package net.firstwemine1.indicatorbars.platform.mixin;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The slot counter: drawn after vanilla's stack count and durability bar, on every item in a GUI slot. */
@Mixin(ItemRenderer.class)
abstract class ItemRendererMixin {
	@Shadow
	public float blitOffset;

	@Inject(method = "renderGuiItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
			at = @At("TAIL"))
	private void indicatorbars$renderSlot(Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
		ClientHooks.renderSlot(font, stack, x, y, blitOffset);
	}
}
