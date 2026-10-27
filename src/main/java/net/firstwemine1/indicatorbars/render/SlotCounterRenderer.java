package net.firstwemine1.indicatorbars.render;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.firstwemine1.indicatorbars.meter.durability.Durability;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * Uses left, as a half-size number in the slot's top-left corner, on every damageable item drawn in a
 * GUI: hotbar and inventory screens. The stack count keeps the bottom-right corner.
 */
public final class SlotCounterRenderer {
	private static final float SCALE = 0.5f;

	private SlotCounterRenderer() {
	}

	public static void render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, DurabilityConfig config) {
		if (!config.showNumbers || config.slotCounter == DurabilityConfig.Show.OFF || !Durability.tracked(stack)
				|| config.slotCounter == DurabilityConfig.Show.DAMAGED && !stack.isDamaged()) {
			return;
		}
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x + 1, y + 1);
		pose.scale(SCALE, SCALE);
		graphics.drawString(font, Integer.toString(Durability.left(stack)), 0, 0, Durability.color(stack), true);
		pose.popMatrix();
	}
}
