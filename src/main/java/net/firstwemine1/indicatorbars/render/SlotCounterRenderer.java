package net.firstwemine1.indicatorbars.render;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.firstwemine1.indicatorbars.meter.durability.Durability;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Uses left, as a half-size number in the slot's top-left corner, on every damageable item drawn in a
 * GUI: hotbar and inventory screens. The stack count keeps the bottom-right corner.
 */
public final class SlotCounterRenderer {
	private static final float SCALE = 0.5f;
	/** Vanilla draws the stack count this far forward, over the item. */
	private static final float TEXT_Z = 200;

	private SlotCounterRenderer() {
	}

	public static void render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, DurabilityConfig config) {
		if (config.slotCounter == DurabilityConfig.Show.OFF || !Durability.tracked(stack)
				|| config.slotCounter == DurabilityConfig.Show.DAMAGED && !stack.isDamaged()) {
			return;
		}
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x + 1, y + 1, TEXT_Z);
		pose.scale(SCALE, SCALE, 1);
		graphics.drawString(font, Integer.toString(Durability.left(stack)), 0, 0, Durability.color(stack), true);
		pose.popPose();
	}
}
