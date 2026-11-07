package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.platform.mixin.ContainerScreenAccessor;
import net.firstwemine1.indicatorbars.platform.mixin.RecipeBookScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookMenu;
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
	/** The recipe book button's own colours, normal and hovered. */
	private static final int[] NORMAL = {0xFF000000, 0xFFFFFFFF, 0xFFC6C6C6, 0xFF555555};
	private static final int[] HOVERED = {0xFF00073E, 0xFFFFFFFF, 0xFF8892C9, 0xFF343E75};

	private final AbstractContainerScreen<?> screen;
	private final List<AbstractWidget> widgets;
	private final Slot output;
	private final QuickCraftPanel panel;
	private final boolean inInventory;

	private QuickCraftButton(AbstractContainerScreen<?> screen, List<AbstractWidget> widgets, RecipeBookMenu<?> menu) {
		super(0, 0, WIDTH, HEIGHT, new net.minecraft.network.chat.TextComponent(""));
		this.screen = screen;
		this.widgets = widgets;
		this.output = menu.getSlot(menu.getResultSlotIndex());
		this.panel = new QuickCraftPanel(screen, menu);
		this.inInventory = screen instanceof InventoryScreen;
	}

	/** Adds the button and its panel to a freshly built crafting screen. */
	public static QuickCraftButton attach(AbstractContainerScreen<?> screen, List<AbstractWidget> widgets, RecipeBookMenu<?> menu) {
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
	static void closeRecipeBook(AbstractContainerScreen<?> screen, List<AbstractWidget> widgets) {
		if (!((RecipeBookScreenAccessor) screen).getRecipeBookComponent().isVisible()) {
			return;
		}
		for (AbstractWidget widget : widgets) {
			if (widget instanceof ImageButton book) {
				book.onPress();
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
			x = window.getLeftPos() + INVENTORY_X;
			y = window.getTopPos() + INVENTORY_Y;
		} else {
			x = window.getLeftPos() + output.x + ICON_SIZE + TABLE_SLOT_BORDER + GAP;
			y = window.getTopPos() + output.y + (ICON_SIZE - HEIGHT) / 2;
		}
		int panelX = window.getLeftPos() + WINDOW_WIDTH + GAP;
		panel.x = panelX;
		panel.y = window.getTopPos();
		panel.refresh(screen.width - panelX);
	}

	@Override
	public void onPress() {
		panel.visible = !panel.visible;
		panel.refreshSoon();
		if (panel.visible) {
			closeRecipeBook(screen, widgets);
			for (AbstractWidget widget : widgets) {
				if (widget instanceof SortPanel other) {
					other.visible = false;
				}
			}
		}
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		drawFrame(poseStack, isHoveredOrFocused() ? HOVERED : NORMAL);
		Minecraft.getInstance().getItemRenderer().renderGuiItem(ICON, x + (WIDTH - ICON_SIZE) / 2, y + (HEIGHT - ICON_SIZE) / 2);
		// The cursor is still here after opening a panel: a tooltip would land on top of it.
		if (isHovered && !anyPanelOpen(widgets)) {
			LateTooltip.set(List.of(new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.button"), new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.help")));
		}
	}

	/** The recipe book button's shape: a dark outline with clipped corners, lit from the top left, shaded bottom right. */
	private void drawFrame(PoseStack poseStack, int[] colors) {
		int left = x;
		int top = y;
		int right = x + width;
		int bottom = y + height;
		int outline = colors[0];
		GuiComponent.fill(poseStack, left + 2, top, right - 2, top + 1, outline);
		GuiComponent.fill(poseStack, left + 2, bottom - 1, right - 2, bottom, outline);
		GuiComponent.fill(poseStack, left, top + 2, left + 1, bottom - 2, outline);
		GuiComponent.fill(poseStack, right - 1, top + 2, right, bottom - 2, outline);
		GuiComponent.fill(poseStack, left + 1, top + 1, left + 2, top + 2, outline);
		GuiComponent.fill(poseStack, right - 2, top + 1, right - 1, top + 2, outline);
		GuiComponent.fill(poseStack, left + 1, bottom - 2, left + 2, bottom - 1, outline);
		GuiComponent.fill(poseStack, right - 2, bottom - 2, right - 1, bottom - 1, outline);
		GuiComponent.fill(poseStack, left + 2, top + 1, right - 2, top + 2, colors[1]);
		GuiComponent.fill(poseStack, left + 1, top + 2, left + 2, bottom - 2, colors[1]);
		GuiComponent.fill(poseStack, left + 2, top + 2, right - 2, bottom - 2, colors[2]);
		GuiComponent.fill(poseStack, right - 2, top + 2, right - 1, bottom - 2, colors[3]);
		GuiComponent.fill(poseStack, left + 2, bottom - 2, right - 2, bottom - 1, colors[3]);
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
		output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, new net.minecraft.network.chat.TranslatableComponent("indicatorbars.craft.button"));
	}
}
