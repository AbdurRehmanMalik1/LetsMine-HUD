package net.firstwemine1.indicatorbars.render;

import java.util.Locale;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.firstwemine1.indicatorbars.config.IBConfig.SortConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.IFormattableTextComponent;

/** The sort options, beside the inventory window. Choosing one applies it and sorts straight away. */
public final class SortPanel extends Widget {
	public static final int ROW_HEIGHT = 10;
	public static final int PADDING = 3;
	private static final int WIDTH = 68;
	/** A heading and three choices, twice. */
	private static final int ROWS = 8;
	private static final int PRESET_HEADING = 4;
	private static final int BACKGROUND = 0xE0101010;
	private static final int BORDER = 0xFF808080;
	private static final int HEADING = 0xFFA0A0A0;
	private static final int CHOICE = 0xFFFFFFFF;
	private static final int SELECTED = 0xFFFFFF55;
	private static final int HOVER = 0x40FFFFFF;
	private static final String BULLET = "\u2022 ";

	private final SortConfig config;
	private final Runnable changed;

	SortPanel(SortConfig config, Runnable changed) {
		super(0, 0, WIDTH, ROWS * ROW_HEIGHT + 2 * PADDING, new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.button"));
		this.config = config;
		this.changed = changed;
		this.visible = false;
	}

	/** The lang key of a metric or preset. */
	static String key(Enum<?> choice) {
		String group = choice instanceof SortConfig.Metric ? "metric" : "preset";
		return "indicatorbars.sort." + group + "." + choice.name().toLowerCase(Locale.ROOT);
	}

	/** The row of a metric. */
	public static int row(SortConfig.Metric metric) {
		return 1 + metric.ordinal();
	}

	/** The row of a preset. */
	public static int row(SortConfig.Preset preset) {
		return PRESET_HEADING + 1 + preset.ordinal();
	}

	private int rowY(int row) {
		return y + PADDING + row * ROW_HEIGHT;
	}

	private void drawChoice(MatrixStack poseStack, FontRenderer font, Enum<?> choice, int row, boolean selected, int mouseX, int mouseY) {
		int top = rowY(row);
		if (mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < top + ROW_HEIGHT) {
			AbstractGui.fill(poseStack, x + 1, top, x + width - 1, top + ROW_HEIGHT, HOVER);
		}
		IFormattableTextComponent text = new net.minecraft.util.text.StringTextComponent(BULLET).append(new net.minecraft.util.text.TranslationTextComponent(key(choice)));
		font.drawShadow(poseStack, text, x + 2 * PADDING, top + 1, selected ? SELECTED : CHOICE);
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		FontRenderer font = Minecraft.getInstance().font;
		AbstractGui.fill(poseStack, x, y, x + width, y + height, BACKGROUND);
		AbstractGui.fill(poseStack, x, y, x + width, y + 1, BORDER);
		AbstractGui.fill(poseStack, x, y + height - 1, x + width, y + height, BORDER);
		AbstractGui.fill(poseStack, x, y, x + 1, y + height, BORDER);
		AbstractGui.fill(poseStack, x + width - 1, y, x + width, y + height, BORDER);
		font.drawShadow(poseStack, new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.heading.order"), x + PADDING, rowY(0) + 1, HEADING);
		for (SortConfig.Metric metric : SortConfig.Metric.values()) {
			drawChoice(poseStack, font, metric, row(metric), metric == config.metric, mouseX, mouseY);
		}
		font.drawShadow(poseStack, new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.heading.preset"), x + PADDING, rowY(PRESET_HEADING) + 1, HEADING);
		for (SortConfig.Preset preset : SortConfig.Preset.values()) {
			drawChoice(poseStack, font, preset, row(preset), preset == config.preset, mouseX, mouseY);
		}
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		int row = (int) Math.floor((mouseY - y - PADDING) / ROW_HEIGHT);
		for (SortConfig.Metric metric : SortConfig.Metric.values()) {
			if (row == row(metric)) {
				config.metric = metric;
				changed.run();
			}
		}
		for (SortConfig.Preset preset : SortConfig.Preset.values()) {
			if (row == row(preset)) {
				config.preset = preset;
				changed.run();
			}
		}
	}

}
