package net.firstwemine1.indicatorbars.render;

import java.util.List;

import net.firstwemine1.indicatorbars.platform.mixin.ContainerScreenAccessor;
import net.firstwemine1.indicatorbars.platform.mixin.RecipeBookScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A book button inside the window, opening the quick-craft panel beside it. In the inventory it's the next
 * button along from the sort button; at a crafting table it's right of the output slot and centred on it.
 */
public final class QuickCraftButton extends AbstractButton {
	private static final ItemStack ICON = new ItemStack(Items.BOOK);
	private static final int WIDTH = 20;
	private static final int HEIGHT = 18;
	private static final int ICON_SIZE = 16;
	/** Within the inventory window: after the recipe book and sort buttons, on their row. */
	private static final int INVENTORY_X = 148;
	private static final int INVENTORY_Y = 61;
	/** A crafting table's output slot is drawn this much bigger than its item, on every side. */
	private static final int TABLE_SLOT_BORDER = 5;
	private static final int WINDOW_WIDTH = 176;
	private static final int GAP = 2;
	/** The recipe book button's own colours, normal and hovered, read from its sprite. Its book is part of the sprite. */
	private static final int[] NORMAL = {0xFF000000, 0xFFFFFFFF, 0xFFC6C6C6, 0xFF555555};
	private static final int[] HOVERED = {0xFF00073E, 0xFFFFFFFF, 0xFF8892C9, 0xFF343E75};

	private final AbstractRecipeBookScreen<?> screen;
	private final List<AbstractWidget> widgets;
	private final Slot output;
	private final QuickCraftPanel panel;
	private final boolean inInventory;
	private final Tooltip tooltip;

	private QuickCraftButton(AbstractRecipeBookScreen<?> screen, List<AbstractWidget> widgets, AbstractCraftingMenu menu) {
		super(0, 0, WIDTH, HEIGHT, Component.translatable("indicatorbars.craft.button"));
		this.screen = screen;
		this.widgets = widgets;
		this.output = menu.getResultSlot();
		this.panel = new QuickCraftPanel(menu);
		this.inInventory = screen instanceof InventoryScreen;
		this.tooltip = Tooltip.create(Component.translatable("indicatorbars.craft.button").append("\n")
				.append(Component.translatable("indicatorbars.craft.help")));
	}

	/** Adds the button and its panel to a freshly built crafting screen. */
	public static QuickCraftButton attach(AbstractRecipeBookScreen<?> screen, List<AbstractWidget> widgets, AbstractCraftingMenu menu) {
		QuickCraftButton button = new QuickCraftButton(screen, widgets, menu);
		button.follow();
		widgets.add(button);
		widgets.add(button.panel);
		return button;
	}

	public QuickCraftPanel panel() {
		return panel;
	}

	/** Only one panel opens at a time, but either one rules out a tooltip. */
	static boolean anyPanelOpen(List<AbstractWidget> widgets) {
		for (AbstractWidget widget : widgets) {
			if ((widget instanceof SortPanel || widget instanceof QuickCraftPanel) && widget.visible) {
				return true;
			}
		}
		return false;
	}

	/** Opening a panel closes the recipe book, as opening the book closes a panel: there's no room for both. */
	static void closeRecipeBook(AbstractRecipeBookScreen<?> screen, List<AbstractWidget> widgets, InputWithModifiers input) {
		if (!((RecipeBookScreenAccessor) screen).getRecipeBookComponent().isVisible()) {
			return;
		}
		for (AbstractWidget widget : widgets) {
			if (widget instanceof ImageButton book) {
				book.onPress(input);
				return;
			}
		}
	}

	/** Keeps up with the window, which moves when the recipe book opens. */
	public void follow() {
		ContainerScreenAccessor window = (ContainerScreenAccessor) screen;
		RecipeBookScreenAccessor book = (RecipeBookScreenAccessor) screen;
		boolean open = book.getRecipeBookComponent().isVisible();
		// On a narrow screen an open recipe book replaces the window.
		visible = !(open && book.getWidthTooNarrow());
		// Beside the window there may be no room next to the book.
		if (open) {
			panel.visible = false;
		}
		if (inInventory) {
			setX(window.getLeftPos() + INVENTORY_X);
			setY(window.getTopPos() + INVENTORY_Y);
		} else {
			setX(window.getLeftPos() + output.x + ICON_SIZE + TABLE_SLOT_BORDER + GAP);
			setY(window.getTopPos() + output.y + (ICON_SIZE - HEIGHT) / 2);
		}
		int panelX = window.getLeftPos() + WINDOW_WIDTH + GAP;
		panel.setX(panelX);
		panel.setY(window.getTopPos());
		panel.refresh(screen.width - panelX);
		// The cursor is still here after opening a panel: a tooltip would land on top of it.
		setTooltip(anyPanelOpen(widgets) ? null : tooltip);
	}

	@Override
	public void onPress(InputWithModifiers input) {
		panel.visible = !panel.visible;
		panel.refreshSoon();
		if (panel.visible) {
			closeRecipeBook(screen, widgets, input);
			for (AbstractWidget widget : widgets) {
				if (widget instanceof SortPanel other) {
					other.visible = false;
				}
			}
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		drawFrame(graphics, isHoveredOrFocused() ? HOVERED : NORMAL);
		graphics.item(ICON, getX() + (WIDTH - ICON_SIZE) / 2, getY() + (HEIGHT - ICON_SIZE) / 2);
	}

	/** The recipe book button's shape: a dark outline with clipped corners, lit from the top left, shaded bottom right. */
	private void drawFrame(GuiGraphicsExtractor graphics, int[] colors) {
		int left = getX();
		int top = getY();
		int right = getRight();
		int bottom = getBottom();
		int outline = colors[0];
		graphics.fill(left + 2, top, right - 2, top + 1, outline);
		graphics.fill(left + 2, bottom - 1, right - 2, bottom, outline);
		graphics.fill(left, top + 2, left + 1, bottom - 2, outline);
		graphics.fill(right - 1, top + 2, right, bottom - 2, outline);
		graphics.fill(left + 1, top + 1, left + 2, top + 2, outline);
		graphics.fill(right - 2, top + 1, right - 1, top + 2, outline);
		graphics.fill(left + 1, bottom - 2, left + 2, bottom - 1, outline);
		graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, outline);
		graphics.fill(left + 2, top + 1, right - 2, top + 2, colors[1]);
		graphics.fill(left + 1, top + 2, left + 2, bottom - 2, colors[1]);
		graphics.fill(left + 2, top + 2, right - 2, bottom - 2, colors[2]);
		graphics.fill(right - 2, top + 2, right - 1, bottom - 2, colors[3]);
		graphics.fill(left + 2, bottom - 2, right - 2, bottom - 1, colors[3]);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
