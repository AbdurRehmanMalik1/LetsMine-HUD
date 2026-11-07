package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.firstwemine1.indicatorbars.craft.QuickCraftList;
import net.firstwemine1.indicatorbars.craft.QuickCrafter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookMenu;

/** What you can craft right now, by group, beside the window. A click fills the grid; Shift fills it as far as it goes. */
public final class QuickCraftPanel extends AbstractWidget {
	private static final int CELL = 18;
	private static final int ICON_INSET = 1;
	private static final int PADDING = 3;
	private static final int HEADING_HEIGHT = 10;
	private static final int MAX_COLUMNS = 5;
	private static final int MIN_COLUMNS = 3;
	private static final long REFRESH_MILLIS = 250;
	private static final int BACKGROUND = 0xE0101010;
	private static final int BORDER = 0xFF808080;
	private static final int HEADING = 0xFFA0A0A0;
	private static final int HOVER = 0x60FFFFFF;

	/** One clickable item, relative to the panel. */
	private record Cell(QuickCraftList.Entry entry, int x, int y) {
	}

	/** One group's title, relative to the panel. */
	private record Heading(QuickCraftList.Group group, int y) {
	}

	private final RecipeBookMenu<?, ?> menu;
	private final List<Cell> cells = new ArrayList<>();
	private final List<Heading> headings = new ArrayList<>();
	private long nextRefresh;

	QuickCraftPanel(RecipeBookMenu<?, ?> menu) {
		super(0, 0, MAX_COLUMNS * CELL + 2 * PADDING, 2 * PADDING + HEADING_HEIGHT, Component.translatable("indicatorbars.craft.button"));
		this.menu = menu;
		this.visible = false;
	}

	/** Lays the panel out from what can be crafted now, a few times a second while it's open. */
	void refresh(int room) {
		long now = System.currentTimeMillis();
		if (!visible || now < nextRefresh) {
			return;
		}
		nextRefresh = now + REFRESH_MILLIS;
		int columns = Math.clamp((room - 2 * PADDING) / CELL, MIN_COLUMNS, MAX_COLUMNS);
		cells.clear();
		headings.clear();
		int y = PADDING;
		int column = 0;
		QuickCraftList.Group group = null;
		for (QuickCraftList.Entry entry : QuickCraftList.list(Minecraft.getInstance(), menu)) {
			if (entry.group() != group || column == columns) {
				if (column != 0) {
					y += CELL;
				}
				column = 0;
			}
			if (entry.group() != group) {
				group = entry.group();
				headings.add(new Heading(group, y));
				y += HEADING_HEIGHT;
			}
			cells.add(new Cell(entry, PADDING + column * CELL, y));
			column++;
		}
		y += column != 0 ? CELL : HEADING_HEIGHT;
		setWidth(columns * CELL + 2 * PADDING);
		setHeight(y + PADDING);
	}

	/** Item ids on offer, for checking. */
	public List<String> offered() {
		List<String> ids = new ArrayList<>();
		for (Cell cell : cells) {
			ids.add(cell.entry().result().getItem().toString());
		}
		return ids;
	}

	/** Forgets the wait, so a change shows in the next frame. */
	public void refreshSoon() {
		nextRefresh = 0;
	}

	/** Where an item's cell is on screen, or null if it isn't offered. */
	public int[] cellCentre(String id) {
		for (Cell cell : cells) {
			if (cell.entry().result().getItem().toString().equals(id)) {
				return new int[] {getX() + cell.x() + CELL / 2, getY() + cell.y() + CELL / 2};
			}
		}
		return null;
	}

	private boolean inside(Cell cell, double x, double y) {
		return x >= getX() + cell.x() && x < getX() + cell.x() + CELL && y >= getY() + cell.y() && y < getY() + cell.y() + CELL;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Font font = Minecraft.getInstance().font;
		graphics.fill(getX(), getY(), getRight(), getBottom(), BACKGROUND);
		graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), BORDER);
		if (cells.isEmpty()) {
			graphics.drawString(font, Component.translatable("indicatorbars.craft.nothing"), getX() + PADDING, getY() + PADDING + 1, HEADING, true);
			return;
		}
		for (Heading heading : headings) {
			graphics.drawString(font, Component.translatable("indicatorbars.craft.group." + heading.group().name().toLowerCase(Locale.ROOT)),
					getX() + PADDING, getY() + heading.y() + 1, HEADING, true);
		}
		for (Cell cell : cells) {
			int x = getX() + cell.x();
			int y = getY() + cell.y();
			if (inside(cell, mouseX, mouseY)) {
				graphics.fill(x, y, x + CELL, y + CELL, HOVER);
				graphics.renderTooltip(font, cell.entry().result(), mouseX, mouseY);
			}
			graphics.renderItem(cell.entry().result(), x + ICON_INSET, y + ICON_INSET);
			graphics.renderItemDecorations(font, cell.entry().result(), x + ICON_INSET, y + ICON_INSET);
		}
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		for (Cell cell : cells) {
			if (inside(cell, mouseX, mouseY)) {
				QuickCrafter.fill(Minecraft.getInstance(), menu, cell.entry().recipe(), Screen.hasShiftDown());
				refreshSoon();
				return;
			}
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
