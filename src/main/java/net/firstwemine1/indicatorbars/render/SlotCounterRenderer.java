package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.firstwemine1.indicatorbars.meter.durability.Durability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;

/**
 * Uses left, as a half-size number in the slot's top-left corner, on every damageable item drawn in a
 * GUI: hotbar and inventory screens. The stack count keeps the bottom-right corner. Drawn the way
 * vanilla draws the stack count on this version: its own matrix and buffer, in front of the item.
 */
public final class SlotCounterRenderer {
	/** Full block and sky light, packed (1.17+ has it as {@code LightTexture.FULL_BRIGHT}). */
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final float SCALE = 0.5f;
	/** Vanilla draws the stack count this far in front of the item's blit offset. */
	private static final float TEXT_Z = 200;

	private SlotCounterRenderer() {
	}

	/** Hotbar and offhand slots, where vanilla's hotbar draws them (1.16.5 has no item decorations). */
	public static void renderHotbar(int width, int height, DurabilityConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		PlayerEntity player = minecraft.player;
		if (player == null || player.isSpectator() || minecraft.options.hideGui) {
			return;
		}
		int centre = width / 2;
		int y = height - 16 - 3;
		for (int i = 0; i < 9; i++) {
			render(minecraft.font, player.inventory.items.get(i), centre - 90 + i * 20 + 2, y, 0, config);
		}
		int offhandX = player.getMainArm() == HandSide.RIGHT ? centre - 91 - 26 : centre + 91 + 10;
		render(minecraft.font, player.getOffhandItem(), offhandX, y, 0, config);
	}

	public static void render(FontRenderer font, ItemStack stack, int x, int y, float blitOffset, DurabilityConfig config) {
		if (!config.showNumbers || config.slotCounter == DurabilityConfig.Show.OFF || !Durability.tracked(stack)
				|| config.slotCounter == DurabilityConfig.Show.DAMAGED && !stack.isDamaged()) {
			return;
		}
		MatrixStack pose = new MatrixStack();
		pose.translate(x + 1, y + 1, blitOffset + TEXT_Z);
		pose.scale(SCALE, SCALE, 1);
		IRenderTypeBuffer.Impl buffers = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
		font.drawInBatch(Integer.toString(Durability.left(stack)), 0, 0, Durability.color(stack), true, pose.last().pose(),
				buffers, false, 0, FULL_BRIGHT);
		buffers.endBatch();
	}
}
