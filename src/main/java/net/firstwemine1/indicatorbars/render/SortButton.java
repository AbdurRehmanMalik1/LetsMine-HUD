package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.platform.mixin.RecipeBookScreenAccessor;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Beside the recipe book in the player's inventory: left-click sorts, right-click opens the options. */
public final class SortButton extends AbstractButton {
	private static final ResourceLocation ICON = IndicatorBars.id("textures/gui/sort.png");
	private static final WidgetSprites BUTTON = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"),
			ResourceLocation.withDefaultNamespace("widget/button_disabled"), ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
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
	private final List<AbstractWidget> widgets;
	private final IBConfig config;
	private final SortPanel panel;
	private Tooltip tooltip;

	private SortButton(InventoryScreen screen, List<AbstractWidget> widgets, IBConfig config) {
		super(0, 0, WIDTH, HEIGHT, Component.translatable("indicatorbars.sort.button"));
		this.screen = screen;
		this.widgets = widgets;
		this.config = config;
		this.panel = new SortPanel(config.sort, this::choiceMade);
	}

	/** Adds the button to a freshly built inventory screen, and returns it to be kept in place each frame. */
	public static SortButton attach(InventoryScreen screen, List<AbstractWidget> widgets, IBConfig config) {
		SortButton button = new SortButton(screen, widgets, config);
		button.updateTooltip();
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
		RecipeBookScreenAccessor book = (RecipeBookScreenAccessor) screen;
		boolean open = book.getRecipeBookComponent().isVisible();
		// On a narrow screen an open recipe book replaces the inventory.
		visible = !(open && book.getWidthTooNarrow());
		// Beside the window there may be no room next to the book.
		if (open) {
			panel.visible = false;
		}
		ImageButton bookButton = null;
		for (AbstractWidget widget : widgets) {
			if (widget instanceof ImageButton image) {
				bookButton = image;
				break;
			}
		}
		if (bookButton != null) {
			setX(bookButton.getRight() + GAP);
			setY(bookButton.getY());
		} else {
			setX((screen.width - WINDOW_WIDTH) / 2 + FALLBACK_X);
			setY(screen.height / 2 + FALLBACK_Y);
		}
		panel.setX((screen.width + WINDOW_WIDTH) / 2 + GAP);
		panel.setY((screen.height - WINDOW_HEIGHT) / 2);
		// The cursor is still here after opening a panel: a tooltip would land on top of it.
		setTooltip(QuickCraftButton.anyPanelOpen(widgets) ? null : tooltip);
	}

	/** A new order or preset: save it, and sort by it straight away. */
	private void choiceMade() {
		config.save();
		updateTooltip();
		InventorySorter.startOrQueue(Minecraft.getInstance(), config);
	}

	private void updateTooltip() {
		MutableComponent text = Component.translatable("indicatorbars.sort.button").append("\n")
				.append(Component.translatable("indicatorbars.sort.order", Component.translatable(SortPanel.key(config.sort.metric)))).append("\n")
				.append(Component.translatable("indicatorbars.sort.preset", Component.translatable(SortPanel.key(config.sort.preset)))).append("\n")
				.append(Component.translatable("indicatorbars.sort.options"));
		tooltip = Tooltip.create(text);
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
				for (AbstractWidget widget : widgets) {
					if (widget instanceof QuickCraftPanel other) {
						other.visible = false;
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
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blitSprite(BUTTON.get(active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight());
		graphics.blit(ICON, getX() + (WIDTH - ICON_SIZE) / 2, getY() + (HEIGHT - ICON_SIZE) / 2,
				0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
