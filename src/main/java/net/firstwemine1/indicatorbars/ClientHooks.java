package net.firstwemine1.indicatorbars;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.firstwemine1.indicatorbars.meter.crops.CropTracker;
import net.firstwemine1.indicatorbars.meter.durability.EquipmentTracker;
import net.firstwemine1.indicatorbars.meter.health.BarVisibility;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.firstwemine1.indicatorbars.meter.health.TargetTracker;
import net.firstwemine1.indicatorbars.render.CropBarRenderer;
import net.firstwemine1.indicatorbars.render.CropPanelRenderer;
import net.firstwemine1.indicatorbars.render.EquipmentHudRenderer;
import net.firstwemine1.indicatorbars.render.PortraitFit;
import net.firstwemine1.indicatorbars.render.SlotCounterRenderer;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.firstwemine1.indicatorbars.render.WorldBarRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.firstwemine1.indicatorbars.render.HotbarIndicatorRenderer;
import net.firstwemine1.indicatorbars.render.QuickCraftButton;
import net.firstwemine1.indicatorbars.render.SortButton;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint and mixins only turn its own
 * events into these calls, so this class and everything under it is the same on every loader.
 */
public final class ClientHooks {
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping TOGGLE_DURABILITY = new KeyMapping("key.indicatorbars.toggle_durability", InputConstants.KEY_J,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping TOGGLE_CROPS = new KeyMapping("key.indicatorbars.toggle_crops", InputConstants.KEY_K,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping CYCLE_HOTBAR = new KeyMapping("key.indicatorbars.cycle_hotbar", InputConstants.KEY_G,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping HOTBAR_UP = new KeyMapping("key.indicatorbars.hotbar_up", InputConstants.KEY_UP,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping HOTBAR_DOWN = new KeyMapping("key.indicatorbars.hotbar_down", InputConstants.KEY_DOWN,
			"key.categories." + IndicatorBars.MOD_ID);

	private static final TargetTracker TARGET = new TargetTracker();
	private static final EquipmentTracker EQUIPMENT = new EquipmentTracker();
	private static final CropTracker CROPS = new CropTracker();
	private static IBConfig config;
	/**
	 * True while the level renders. GUI entity previews (inventory, horse screen, our own portrait)
	 * run the same renderer, and get no bar.
	 */
	private static boolean renderingLevel;

	private ClientHooks() {
	}

	public static void init(Path configDir) {
		config = IBConfig.load(configDir.resolve(IndicatorBars.MOD_ID + ".json"));
	}

	/** End of every client tick. */
	public static void tick(Minecraft minecraft) {
		while (TOGGLE_BARS.consumeClick()) {
			config.health.showBars = !config.health.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.translatable(
						config.health.showBars ? "indicatorbars.bars.on" : "indicatorbars.bars.off"), true);
			}
		}
		while (TOGGLE_DURABILITY.consumeClick()) {
			config.durability.showNumbers = !config.durability.showNumbers;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.translatable(
						config.durability.showNumbers ? "indicatorbars.durability.on" : "indicatorbars.durability.off"), true);
			}
		}
		while (TOGGLE_CROPS.consumeClick()) {
			config.crops.showBars = !config.crops.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.translatable(
						config.crops.showBars ? "indicatorbars.crops.on" : "indicatorbars.crops.off"), true);
			}
		}
		while (CYCLE_HOTBAR.consumeClick()) {
			HotbarSwitcher.change(minecraft, config, 1);
		}
		while (HOTBAR_UP.consumeClick()) {
			HotbarSwitcher.change(minecraft, config, 1);
		}
		while (HOTBAR_DOWN.consumeClick()) {
			HotbarSwitcher.change(minecraft, config, -1);
		}
		if (minecraft.level == null) {
			reset();
			return;
		}
		InventorySorter.tick(minecraft, config);
		TARGET.tick(minecraft, config.health);
		EQUIPMENT.tick(minecraft, config.durability);
		CROPS.tick(minecraft, config.crops);
	}

	/** The level's entities were drawn; the pose stack has the camera at the origin. */
	public static void renderCropBars(PoseStack poseStack, MultiBufferSource buffers, Camera camera, Frustum frustum) {
		if (config.crops.showBars) {
			CropBarRenderer.render(CROPS.crops(), poseStack, buffers, camera, frustum, config.crops);
		}
	}

	public static void setRenderingLevel(boolean rendering) {
		renderingLevel = rendering;
	}

	/** A living entity was just rendered; the pose stack is at its feet. */
	public static void renderBar(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffers) {
		if (!renderingLevel || !BarVisibility.shouldShow(entity, Minecraft.getInstance(), config.health, TARGET.target())) {
			return;
		}
		float fraction = MeterStyle.fraction(entity.getHealth(), entity.getMaxHealth());
		int color = HealthColors.fill(entity, fraction, config.health.barColor);
		WorldBarRenderer.render(entity, fraction, color, poseStack, buffers, config.health);
	}

	/** The mob health panel, or the crop panel while you look at a crop and not a mob. */
	public static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Crop crop = CROPS.target();
		if (config.crops.showPanel && crop != null && !TARGET.seen()) {
			CropPanelRenderer.render(graphics, crop, config.crops);
			return;
		}
		LivingEntity target = TARGET.target();
		if (!config.health.showPanel || target == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float alpha = TARGET.alpha(config.health, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.render(graphics, target, alpha, config.health);
		}
	}

	/** The durability column. */
	public static void renderEquipmentHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		EquipmentHudRenderer.render(graphics, config.durability, deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	/** An item was drawn in a GUI slot (hotbar, inventory screens). */
	public static void renderSlot(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
		SlotCounterRenderer.render(graphics, font, stack, x, y, config.durability);
	}

	/** Which hotbar is in use. */
	public static void renderHotbarIndicator(GuiGraphics graphics) {
		HotbarIndicatorRenderer.render(graphics, config.hotbars);
	}

	/**
	 * A screen was built. The player's inventory gets the sort button, and any crafting screen the
	 * quick-craft button. Returns what must run each frame to keep them in place, or null if nothing was added.
	 */
	public static Runnable attachScreenButtons(Screen screen, List<AbstractWidget> widgets) {
		if (!(screen instanceof InventoryScreen || screen instanceof CraftingScreen) || !(((AbstractContainerScreen<?>) screen).getMenu() instanceof RecipeBookMenu<?, ?> menu)) {
			return null;
		}
		SortButton sort = screen instanceof InventoryScreen inventory && config.sort.showButton ? SortButton.attach(inventory, widgets, config) : null;
		QuickCraftButton craft = config.craft.showButton ? QuickCraftButton.attach((AbstractContainerScreen<?>) screen, widgets, menu) : null;
		if (sort == null && craft == null) {
			return null;
		}
		return () -> {
			if (sort != null) {
				sort.follow();
			}
			if (craft != null) {
				craft.follow();
			}
		};
	}

	/** Disconnected or left the world: forget the target, the equipment and the crops. */
	public static void reset() {
		InventorySorter.reset();
		TARGET.reset();
		EQUIPMENT.reset();
		CROPS.reset();
		PortraitFit.reset();
	}
}
