package net.firstwemine1.indicatorbars.render;

import java.util.Locale;

import net.firstwemine1.indicatorbars.config.IBConfig.SortConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** The sort options, beside the inventory window. Choosing one applies it and sorts straight away. */
public final class SortPanel extends AbstractWidget {
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
		super(0, 0, WIDTH, ROWS * ROW_HEIGHT + 2 * PADDING, Component.translatable("indicatorbars.sort.button"));
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
		return getY() + PADDING + row * ROW_HEIGHT;
	}

	private void drawChoice(GuiGraphics graphics, Font font, Enum<?> choice, int row, boolean selected, int mouseX, int mouseY) {
		int y = rowY(row);
		if (mouseX >= getX() && mouseX < getRight() && mouseY >= y && mouseY < y + ROW_HEIGHT) {
			graphics.fill(getX() + 1, y, getRight() - 1, y + ROW_HEIGHT, HOVER);
		}
		graphics.drawString(font, Component.literal(BULLET).append(Component.translatable(key(choice))), getX() + 2 * PADDING, y + 1, selected ? SELECTED : CHOICE, true);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Font font = Minecraft.getInstance().font;
		graphics.fill(getX(), getY(), getRight(), getBottom(), BACKGROUND);
		graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), BORDER);
		graphics.drawString(font, Component.translatable("indicatorbars.sort.heading.order"), getX() + PADDING, rowY(0) + 1, HEADING, true);
		for (SortConfig.Metric metric : SortConfig.Metric.values()) {
			drawChoice(graphics, font, metric, row(metric), metric == config.metric, mouseX, mouseY);
		}
		graphics.drawString(font, Component.translatable("indicatorbars.sort.heading.preset"), getX() + PADDING, rowY(PRESET_HEADING) + 1, HEADING, true);
		for (SortConfig.Preset preset : SortConfig.Preset.values()) {
			drawChoice(graphics, font, preset, row(preset), preset == config.preset, mouseX, mouseY);
		}
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		int row = (int) Math.floor((mouseY - getY() - PADDING) / ROW_HEIGHT);
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

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
