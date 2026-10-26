package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.durability.Durability;
import net.firstwemine1.indicatorbars.meter.durability.EquipmentTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

/**
 * A column of what you wear and hold, each with its uses left: helmet, chestplate, leggings, boots,
 * main hand, offhand. Items at or below the warning line show their number in a pulsing red.
 *
 * <pre>
 * [helmet] 63
 * [chest]  10   (red, pulsing)
 * [sword]  761
 * </pre>
 */
public final class EquipmentHudRenderer {
	private static final int ICON = 16;
	private static final int ROW = 17;
	private static final int TEXT_GAP = 2;
	/** Ticks per pulse of a low item's number. */
	private static final float PULSE_TICKS = 20;

	private EquipmentHudRenderer() {
	}

	public static void render(MatrixStack poseStack, DurabilityConfig config, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		PlayerEntity player = minecraft.player;
		if (!config.showNumbers || config.equipmentHud == DurabilityConfig.Show.OFF || player == null || player.isSpectator()) {
			return;
		}
		List<ItemStack> rows = new ArrayList<>();
		for (EquipmentSlotType slot : EquipmentTracker.SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (Durability.tracked(stack) && (config.equipmentHud == DurabilityConfig.Show.ALWAYS || stack.isDamaged())) {
				rows.add(stack);
			}
		}
		if (rows.isEmpty()) {
			return;
		}

		FontRenderer font = minecraft.font;
		int textWidth = 0;
		for (ItemStack stack : rows) {
			textWidth = Math.max(textWidth, font.width(Durability.text(stack, config.hudText)));
		}
		int width = ICON + TEXT_GAP + textWidth;
		int height = rows.size() * ROW - 1;
		boolean offhand = !player.getOffhandItem().isEmpty();
		boolean rightHanded = player.getMainArm() == HandSide.RIGHT;
		int[] corner = config.hudAnchor.place(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
				width, height, config.hudX, config.hudY, offhand && rightHanded, offhand && !rightHanded);

		float pulse = 0.55f + 0.45f * MathHelper.sin((player.tickCount + partialTick) * (float) (Math.PI * 2) / PULSE_TICKS);
		for (int i = 0; i < rows.size(); i++) {
			ItemStack stack = rows.get(i);
			int x = corner[0];
			int y = corner[1] + i * ROW;
			minecraft.getItemRenderer().renderGuiItem(stack, x, y);
			int color = Durability.low(stack, config.warnPercent)
					? MeterStyle.multiplyAlpha(MeterStyle.RED, pulse)
					: Durability.color(stack);
			font.drawShadow(poseStack, Durability.text(stack, config.hudText), x + ICON + TEXT_GAP, y + (ICON - font.lineHeight) / 2 + 1, color);
		}
	}
}
