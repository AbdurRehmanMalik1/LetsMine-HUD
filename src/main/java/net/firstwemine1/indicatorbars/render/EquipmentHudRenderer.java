package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;

import net.firstwemine1.indicatorbars.config.IBConfig.DurabilityConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.durability.Durability;
import net.firstwemine1.indicatorbars.meter.durability.EquipmentTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

	public static void extract(GuiGraphicsExtractor graphics, DurabilityConfig config, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (config.equipmentHud == DurabilityConfig.Show.OFF || player == null || player.isSpectator()) {
			return;
		}
		List<ItemStack> rows = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentTracker.SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (Durability.tracked(stack) && (config.equipmentHud == DurabilityConfig.Show.ALWAYS || stack.isDamaged())) {
				rows.add(stack);
			}
		}
		if (rows.isEmpty()) {
			return;
		}

		Font font = minecraft.font;
		int textWidth = 0;
		for (ItemStack stack : rows) {
			textWidth = Math.max(textWidth, font.width(Durability.text(stack, config.hudText)));
		}
		int width = ICON + TEXT_GAP + textWidth;
		int height = rows.size() * ROW - 1;
		boolean offhand = !player.getOffhandItem().isEmpty();
		boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
		int[] corner = config.hudAnchor.place(graphics.guiWidth(), graphics.guiHeight(), width, height,
				config.hudX, config.hudY, offhand && rightHanded, offhand && !rightHanded);

		float pulse = 0.55f + 0.45f * Mth.sin((player.tickCount + partialTick) * Mth.TWO_PI / PULSE_TICKS);
		for (int i = 0; i < rows.size(); i++) {
			ItemStack stack = rows.get(i);
			int x = corner[0];
			int y = corner[1] + i * ROW;
			graphics.item(stack, x, y);
			int color = Durability.low(stack, config.warnPercent)
					? ARGB.multiplyAlpha(MeterStyle.RED, pulse)
					: Durability.color(stack);
			graphics.text(font, Durability.text(stack, config.hudText), x + ICON + TEXT_GAP, y + (ICON - font.lineHeight) / 2 + 1, color, true);
		}
	}
}
