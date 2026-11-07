package net.firstwemine1.indicatorbars.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.craft.QuickCraftList;
import net.firstwemine1.indicatorbars.craft.QuickCrafter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
	private static final class Cell {
		private final QuickCraftList.Entry entry;
		private final int x;
		private final int y;

		Cell(QuickCraftList.Entry entry, int x, int y) {
			this.entry = entry;
			this.x = x;
			this.y = y;
		}

		public QuickCraftList.Entry entry() {
			return entry;
		}

		public int x() {
			return x;
		}

		public int y() {
			return y;
		}
	}

	/** One group's title, relative to the panel. */
	private static final class Heading {
		private final QuickCraftList.Group group;
		private final int y;

		Heading(QuickCraftList.Group group, int y) {
			this.group = group;
			this.y = y;
		}

		public QuickCraftList.Group group() {
			return group;
		}

		public int y() {
			return y;
		}
	}

	private final Screen screen;
	private final RecipeBookMenu<?> menu;
	private final List<Cell> cells = new ArrayList<>();
	private final List<Heading> headings = new ArrayList<>();
	private long nextRefresh;

	QuickCraftPanel(Screen screen, RecipeBookMenu<?> menu) {
		super(0, 0, MAX_COLUMNS * CELL + 2 * PADDING, 2 * PADDING + HEADING_HEIGHT, new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.button"));
		this.screen = screen;
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
		int columns = Mth.clamp((room - 2 * PADDING) / CELL, MIN_COLUMNS, MAX_COLUMNS);
		cells.clear();
		headings.clear();
		int top = PADDING;
		int column = 0;
		QuickCraftList.Group group = null;
		for (QuickCraftList.Entry entry : QuickCraftList.list(Minecraft.getInstance(), menu)) {
			if (entry.group() != group || column == columns) {
				if (column != 0) {
					top += CELL;
				}
				column = 0;
			}
			if (entry.group() != group) {
				group = entry.group();
				headings.add(new Heading(group, top));
				top += HEADING_HEIGHT;
			}
			cells.add(new Cell(entry, PADDING + column * CELL, top));
			column++;
		}
		top += column != 0 ? CELL : HEADING_HEIGHT;
		width = columns * CELL + 2 * PADDING;
		height = top + PADDING;
	}

	/** Forgets the wait, so a change shows in the next frame. */
	public void refreshSoon() {
		nextRefresh = 0;
	}

	private boolean inside(Cell cell, double mouseX, double mouseY) {
		return mouseX >= x + cell.x() && mouseX < x + cell.x() + CELL && mouseY >= y + cell.y() && mouseY < y + cell.y() + CELL;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		GuiComponent.fill(poseStack, x, y, x + width, y + height, BACKGROUND);
		GuiComponent.fill(poseStack, x, y, x + width, y + 1, BORDER);
		GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, BORDER);
		GuiComponent.fill(poseStack, x, y, x + 1, y + height, BORDER);
		GuiComponent.fill(poseStack, x + width - 1, y, x + width, y + height, BORDER);
		if (cells.isEmpty()) {
			font.drawShadow(poseStack, new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.nothing"), x + PADDING, y + PADDING + 1, HEADING);
			return;
		}
		for (Heading heading : headings) {
			font.drawShadow(poseStack, new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.group." + heading.group().name().toLowerCase(Locale.ROOT)),
					x + PADDING, y + heading.y() + 1, HEADING);
		}
		for (Cell cell : cells) {
			int left = x + cell.x();
			int top = y + cell.y();
			if (inside(cell, mouseX, mouseY)) {
				GuiComponent.fill(poseStack, left, top, left + CELL, top + CELL, HOVER);
				LateTooltip.set(screen.getTooltipFromItem(cell.entry().result()));
			}
			minecraft.getItemRenderer().renderGuiItem(cell.entry().result(), left + ICON_INSET, top + ICON_INSET);
			minecraft.getItemRenderer().renderGuiItemDecorations(font, cell.entry().result(), left + ICON_INSET, top + ICON_INSET);
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

}
