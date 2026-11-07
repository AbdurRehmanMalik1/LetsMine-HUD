package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;

/** Beside the recipe book in the player's inventory: left-click sorts, right-click opens the options. */
public final class SortButton extends AbstractButton {
	private static final ResourceLocation ICON = IndicatorBars.id("textures/gui/sort.png");
	/** Vanilla's rule for when an open recipe book replaces the window. */
	private static final int NARROW_WIDTH = 379;
	private static final int WIDTH = 20;
	private static final int HEIGHT = 18;
	private static final int ICON_SIZE = 16;
	private static final int GAP = 2;
	/** Vanilla's inventory window, and where its book button would be. */
	private static final int WINDOW_WIDTH = 176;
	private static final int WINDOW_HEIGHT = 166;
	private static final int FALLBACK_X = 126;
	private static final int FALLBACK_Y = -22;

	private final InventoryScreen screen;
	private final List<Widget> widgets;
	private final IBConfig config;
	private final SortPanel panel;

	private SortButton(InventoryScreen screen, List<Widget> widgets, IBConfig config) {
		super(0, 0, WIDTH, HEIGHT, new net.minecraft.util.text.StringTextComponent(""));
		this.screen = screen;
		this.widgets = widgets;
		this.config = config;
		this.panel = new SortPanel(config.sort, this::choiceMade);
	}

	/** Adds the button to a freshly built inventory screen, and returns it to be kept in place each frame. */
	public static SortButton attach(InventoryScreen screen, List<Widget> widgets, IBConfig config) {
		SortButton button = new SortButton(screen, widgets, config);
		button.follow();
		widgets.add(button);
		widgets.add(button.panel);
		return button;
	}

	public SortPanel panel() {
		return panel;
	}

	/** The book button moves when the recipe book opens, so this looks it up each frame. */
	public void follow() {
		IRecipeShownListener book = (IRecipeShownListener) screen;
		boolean open = book.getRecipeBookComponent().isVisible();
		// On a narrow screen an open recipe book replaces the inventory.
		visible = !(open && screen.width < NARROW_WIDTH);
		// Beside the window there may be no room next to the book.
		if (open) {
			panel.visible = false;
		}
		ImageButton bookButton = null;
		for (Widget widget : widgets) {
			if (widget instanceof ImageButton) {
				bookButton = (ImageButton) widget;
				break;
			}
		}
		if (bookButton != null) {
			x = bookButton.x + bookButton.getWidth() + GAP;
			y = bookButton.y;
		} else {
			x = (screen.width - WINDOW_WIDTH) / 2 + FALLBACK_X;
			y = screen.height / 2 + FALLBACK_Y;
		}
		panel.x = (screen.width + WINDOW_WIDTH) / 2 + GAP;
		panel.y = (screen.height - WINDOW_HEIGHT) / 2;
	}

	/** A new order or preset: save it, and sort by it straight away. */
	private void choiceMade() {
		config.save();
		InventorySorter.startOrQueue(Minecraft.getInstance(), config);
	}

	@Override
	protected boolean isValidClickButton(int button) {
		return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!active || !visible || !isValidClickButton(button) || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundManager());
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			panel.visible = !panel.visible;
			if (panel.visible) {
				QuickCraftButton.closeRecipeBook(screen, widgets);
				for (Widget widget : widgets) {
					if (widget instanceof QuickCraftPanel) {
						widget.visible = false;
					}
				}
			}
		} else {
			onPress();
		}
		return true;
	}

	@Override
	public void onPress() {
		InventorySorter.start(Minecraft.getInstance(), config);
	}

	@Override
	public void renderButton(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
		super.renderButton(poseStack, mouseX, mouseY, partialTick);
		Minecraft.getInstance().getTextureManager().bind(ICON);
		RenderSystem.color4f(1, 1, 1, 1);
		RenderSystem.enableBlend();
		AbstractGui.blit(poseStack, x + (WIDTH - ICON_SIZE) / 2, y + (HEIGHT - ICON_SIZE) / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
		// The cursor is still here after opening a panel: a tooltip would land on top of it.
		if (isHovered() && !QuickCraftButton.anyPanelOpen(widgets)) {
			LateTooltip.set(java.util.Arrays.asList(new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.button"),
					new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.order", new net.minecraft.util.text.TranslationTextComponent(SortPanel.key(config.sort.metric))),
					new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.preset", new net.minecraft.util.text.TranslationTextComponent(SortPanel.key(config.sort.preset))),
					new net.minecraft.util.text.TranslationTextComponent("indicatorbars.sort.options")));
		}
	}

}
