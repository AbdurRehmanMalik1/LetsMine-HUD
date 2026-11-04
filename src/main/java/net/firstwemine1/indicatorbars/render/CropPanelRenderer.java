package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;

import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Problem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The HUD panel for the crop under the crosshair: its seed or fruit, name, stage and bar, then a line
 * for each thing slowing it down. Shown in place of the mob panel while you look at a crop.
 *
 * <pre>
 * +--------------------------------+
 * | (seeds) Wheat Crops        4/7 |
 * | [=========---------]           |
 * | - - - - - - - - - -            |  red, when it warns
 * | Too dark to grow: light 6/9    |
 * +--------------------------------+
 * </pre>
 */
public final class CropPanelRenderer {
	private static final int PADDING = 4;
	private static final int ICON = 16;
	private static final int MIN_WIDTH = 100;
	private static final int BAR_HEIGHT = 5;
	private static final int DASH = 3;
	private static final int LINE_GAP = 2;
	/** A problem that doesn't warn on the bar (crowding, by default). */
	private static final int NOTE = 0xFFA0A0A0;

	private CropPanelRenderer() {
	}

	public static void render(PoseStack poseStack, Crop crop, CropsConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Component name = crop.state().getBlock().getName();
		String stage = stageText(crop, config.stageText);
		List<Line> lines = lines(crop, config);
		boolean warn = crop.warns(config);

		int headerWidth = ICON + PADDING + font.width(name) + (stage != null ? PADDING * 2 + font.width(stage) : 0);
		int width = Math.max(MIN_WIDTH, headerWidth);
		for (Line line : lines) {
			width = Math.max(width, font.width(line.text()));
		}
		width += 2 * PADDING;
		int barBlock = BAR_HEIGHT + (warn ? LINE_GAP + 1 : 0);
		int height = PADDING + ICON + LINE_GAP + barBlock + lines.size() * (font.lineHeight + LINE_GAP) + PADDING;
		int[] corner = config.panelAnchor.place(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), width, height,
				config.panelX, config.panelY, true, true);
		int x = corner[0];
		int y = corner[1];

		GuiComponent.fill(poseStack, x, y, x + width, y + height, MeterStyle.PANEL);
		outline(poseStack, x, y, width, height, MeterStyle.FRAME);

		int left = x + PADDING;
		int right = x + width - PADDING;
		ItemStack icon = new ItemStack(crop.state().getBlock());
		minecraft.getItemRenderer().renderGuiItem(icon, left, y + PADDING);
		int textY = y + PADDING + (ICON - font.lineHeight) / 2 + 1;
		font.drawShadow(poseStack, name, left + ICON + PADDING, textY, MeterStyle.TEXT);
		if (stage != null) {
			font.drawShadow(poseStack, stage, right - font.width(stage), textY, MeterStyle.TEXT);
		}

		int barY = y + PADDING + ICON + LINE_GAP;
		GuiComponent.fill(poseStack, left, barY, right, barY + BAR_HEIGHT, MeterStyle.MISSING);
		GuiComponent.fill(poseStack, left, barY, left + Math.round((right - left) * crop.fraction()), barY + BAR_HEIGHT, crop.color());
		if (warn) {
			int dotY = barY + BAR_HEIGHT + LINE_GAP;
			for (int dash = left; dash < right; dash += 2 * DASH) {
				GuiComponent.fill(poseStack, dash, dotY, Math.min(dash + DASH, right), dotY + 1, MeterStyle.RED);
			}
		}

		int lineY = barY + barBlock + LINE_GAP;
		for (Line line : lines) {
			font.drawShadow(poseStack, line.text(), left, lineY, line.color());
			lineY += font.lineHeight + LINE_GAP;
		}
	}

	private static void outline(PoseStack poseStack, int x, int y, int width, int height, int color) {
		GuiComponent.fill(poseStack, x, y, x + width, y + 1, color);
		GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, color);
		GuiComponent.fill(poseStack, x, y + 1, x + 1, y + height - 1, color);
		GuiComponent.fill(poseStack, x + width - 1, y + 1, x + width, y + height - 1, color);
	}

	private record Line(Component text, int color) {
	}

	private static List<Line> lines(Crop crop, CropsConfig config) {
		List<Line> lines = new ArrayList<>();
		if (crop.ripe() && !crop.stem()) {
			lines.add(new Line(Component.translatable("indicatorbars.crops.ripe"), MeterStyle.GREEN));
			return lines;
		}
		for (Problem problem : crop.problems()) {
			Component text = problem == Problem.TOO_DARK
					? Component.translatable(problem.translationKey(), crop.light(), crop.lightNeeded())
					: Component.translatable(problem.translationKey());
			lines.add(new Line(text, problem.warns(config) ? MeterStyle.RED : NOTE));
		}
		if (crop.problems().isEmpty()) {
			lines.add(new Line(Component.translatable(crop.ripe() ? "indicatorbars.crops.fruiting" : "indicatorbars.crops.optimal"),
					MeterStyle.GREEN));
		}
		return lines;
	}

	private static String stageText(Crop crop, CropsConfig.StageText mode) {
		return switch (mode) {
			case FRACTION -> crop.age() + "/" + crop.maxAge();
			case PERCENT -> Math.round(crop.fraction() * 100) + "%";
			case OFF -> null;
		};
	}
}
