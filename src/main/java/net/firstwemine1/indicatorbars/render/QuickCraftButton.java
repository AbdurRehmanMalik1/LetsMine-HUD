package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;

/**
 * A book button inside the window, opening the quick-craft panel beside it. In the inventory it's the next
 * button along from the sort button; at a crafting table it's right of the output slot and centred on it.
 */
public final class QuickCraftButton extends AbstractButton {
	private static final ItemStack ICON = new ItemStack(Items.BOOK);
	/** Vanilla's rule for when an open recipe book replaces the window. */
	private static final int NARROW_WIDTH = 379;
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
	/** The recipe book button's own colours, normal and hovered. */
	private static final int[] NORMAL = {0xFF000000, 0xFFFFFFFF, 0xFFC6C6C6, 0xFF555555};
	private static final int[] HOVERED = {0xFF00073E, 0xFFFFFFFF, 0xFF8892C9, 0xFF343E75};

	private final ContainerScreen<?> screen;
	private final List<Widget> widgets;
	private final Slot output;
	private final QuickCraftPanel panel;
	private final boolean inInventory;

	private QuickCraftButton(ContainerScreen<?> screen, List<Widget> widgets, RecipeBookContainer<?> menu) {
		super(0, 0, WIDTH, HEIGHT, new net.minecraft.util.text.StringTextComponent(""));
		this.screen = screen;
		this.widgets = widgets;
		this.output = menu.getSlot(menu.getResultSlotIndex());
		this.panel = new QuickCraftPanel(screen, menu);
		this.inInventory = screen instanceof InventoryScreen;
	}

	/** Adds the button and its panel to a freshly built crafting screen. */
	public static QuickCraftButton attach(ContainerScreen<?> screen, List<Widget> widgets, RecipeBookContainer<?> menu) {
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
	static boolean anyPanelOpen(List<Widget> widgets) {
		for (Widget widget : widgets) {
			if ((widget instanceof SortPanel || widget instanceof QuickCraftPanel) && widget.visible) {
				return true;
			}
		}
		return false;
	}

	/** Opening a panel closes the recipe book, as opening the book closes a panel: there's no room for both. */
	static void closeRecipeBook(ContainerScreen<?> screen, List<Widget> widgets) {
		if (!((IRecipeShownListener) screen).getRecipeBookComponent().isVisible()) {
			return;
		}
		for (Widget widget : widgets) {
			if (widget instanceof ImageButton) {
				((ImageButton) widget).onPress();
				return;
			}
		}
	}

	/** Keeps up with the window, which moves when the recipe book opens. */
	public void follow() {
		IRecipeShownListener book = (IRecipeShownListener) screen;
		boolean open = book.getRecipeBookComponent().isVisible();
		// On a narrow screen an open recipe book replaces the window.
		visible = !(open && screen.width < NARROW_WIDTH);
		// Beside the window there may be no room next to the book.
		if (open) {
			panel.visible = false;
		}
		if (inInventory) {
			x = screen.getGuiLeft() + INVENTORY_X;
			y = screen.getGuiTop() + INVENTORY_Y;
		} else {
			x = screen.getGuiLeft() + output.x + ICON_SIZE + TABLE_SLOT_BORDER + GAP;
			y = screen.getGuiTop() + output.y + (ICON_SIZE - HEIGHT) / 2;
		}
		int panelX = screen.getGuiLeft() + WINDOW_WIDTH + GAP;
		panel.x = panelX;
		panel.y = screen.getGuiTop();
		panel.refresh(screen.width - panelX);
	}

	@Override
	public void onPress() {
		panel.visible = !panel.visible;
		panel.refreshSoon();
		if (panel.visible) {
			closeRecipeBook(screen, widgets);
			for (Widget widget : widgets) {
				if (widget instanceof SortPanel) {
					widget.visible = false;
				}
			}
		}
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		drawFrame(poseStack, isHovered() ? HOVERED : NORMAL);
		Minecraft.getInstance().getItemRenderer().renderGuiItem(ICON, x + (WIDTH - ICON_SIZE) / 2, y + (HEIGHT - ICON_SIZE) / 2);
		// The cursor is still here after opening a panel: a tooltip would land on top of it.
		if (isHovered() && !anyPanelOpen(widgets)) {
			LateTooltip.set(java.util.Arrays.asList(new net.minecraft.util.text.TranslationTextComponent("indicatorbars.craft.button"), new net.minecraft.util.text.TranslationTextComponent("indicatorbars.craft.help")));
		}
	}

	/** The recipe book button's shape: a dark outline with clipped corners, lit from the top left, shaded bottom right. */
	private void drawFrame(MatrixStack poseStack, int[] colors) {
		int left = x;
		int top = y;
		int right = x + width;
		int bottom = y + height;
		int outline = colors[0];
		AbstractGui.fill(poseStack, left + 2, top, right - 2, top + 1, outline);
		AbstractGui.fill(poseStack, left + 2, bottom - 1, right - 2, bottom, outline);
		AbstractGui.fill(poseStack, left, top + 2, left + 1, bottom - 2, outline);
		AbstractGui.fill(poseStack, right - 1, top + 2, right, bottom - 2, outline);
		AbstractGui.fill(poseStack, left + 1, top + 1, left + 2, top + 2, outline);
		AbstractGui.fill(poseStack, right - 2, top + 1, right - 1, top + 2, outline);
		AbstractGui.fill(poseStack, left + 1, bottom - 2, left + 2, bottom - 1, outline);
		AbstractGui.fill(poseStack, right - 2, bottom - 2, right - 1, bottom - 1, outline);
		AbstractGui.fill(poseStack, left + 2, top + 1, right - 2, top + 2, colors[1]);
		AbstractGui.fill(poseStack, left + 1, top + 2, left + 2, bottom - 2, colors[1]);
		AbstractGui.fill(poseStack, left + 2, top + 2, right - 2, bottom - 2, colors[2]);
		AbstractGui.fill(poseStack, right - 2, top + 2, right - 1, bottom - 2, colors[3]);
		AbstractGui.fill(poseStack, left + 2, bottom - 2, right - 2, bottom - 1, colors[3]);
	}

}
