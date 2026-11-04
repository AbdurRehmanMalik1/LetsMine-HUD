package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;

import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Problem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

	public static void extract(GuiGraphicsExtractor graphics, Crop crop, CropsConfig config) {
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
		int[] corner = config.panelAnchor.place(graphics.guiWidth(), graphics.guiHeight(), width, height,
				config.panelX, config.panelY, true, true);
		int x = corner[0];
		int y = corner[1];

		graphics.fill(x, y, x + width, y + height, MeterStyle.PANEL);
		graphics.outline(x, y, width, height, MeterStyle.FRAME);

		int left = x + PADDING;
		int right = x + width - PADDING;
		ItemStack icon = crop.state().getCloneItemStack(minecraft.level, crop.pos(), false);
		graphics.item(icon, left, y + PADDING);
		int textY = y + PADDING + (ICON - font.lineHeight) / 2 + 1;
		graphics.text(font, name, left + ICON + PADDING, textY, MeterStyle.TEXT);
		if (stage != null) {
			graphics.text(font, stage, right - font.width(stage), textY, MeterStyle.TEXT);
		}

		int barY = y + PADDING + ICON + LINE_GAP;
		graphics.fill(left, barY, right, barY + BAR_HEIGHT, MeterStyle.MISSING);
		graphics.fill(left, barY, left + Math.round((right - left) * crop.fraction()), barY + BAR_HEIGHT, crop.color());
		if (warn) {
			int dotY = barY + BAR_HEIGHT + LINE_GAP;
			for (int dash = left; dash < right; dash += 2 * DASH) {
				graphics.fill(dash, dotY, Math.min(dash + DASH, right), dotY + 1, MeterStyle.RED);
			}
		}

		int lineY = barY + barBlock + LINE_GAP;
		for (Line line : lines) {
			graphics.text(font, line.text(), left, lineY, line.color());
			lineY += font.lineHeight + LINE_GAP;
		}
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
