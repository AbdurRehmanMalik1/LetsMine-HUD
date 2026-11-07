package net.firstwemine1.indicatorbars;

import java.nio.file.Path;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
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
import net.firstwemine1.indicatorbars.render.HealthBarState;
import net.firstwemine1.indicatorbars.render.HotbarIndicatorRenderer;
import net.firstwemine1.indicatorbars.render.PortraitFit;
import net.firstwemine1.indicatorbars.render.PortraitRenderer;
import net.firstwemine1.indicatorbars.render.QuickCraftButton;
import net.firstwemine1.indicatorbars.render.SlotCounterRenderer;
import net.firstwemine1.indicatorbars.render.SortButton;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.firstwemine1.indicatorbars.render.WorldBarRenderer;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint and mixins only turn its own
 * events into these calls, so this class and everything under it is the same on every loader.
 */
public final class ClientHooks {
	/** Registered by each loader's entrypoint: vanilla's registry on Fabric, an event on NeoForge. */
	public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(IndicatorBars.id(IndicatorBars.MOD_ID));
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H, KEY_CATEGORY);
	public static final KeyMapping TOGGLE_DURABILITY = new KeyMapping("key.indicatorbars.toggle_durability", InputConstants.KEY_J, KEY_CATEGORY);
	public static final KeyMapping TOGGLE_CROPS = new KeyMapping("key.indicatorbars.toggle_crops", InputConstants.KEY_K, KEY_CATEGORY);
	public static final KeyMapping CYCLE_HOTBAR = new KeyMapping("key.indicatorbars.cycle_hotbar", InputConstants.KEY_G, KEY_CATEGORY);
	public static final KeyMapping HOTBAR_UP = new KeyMapping("key.indicatorbars.hotbar_up", InputConstants.KEY_UP, KEY_CATEGORY);
	public static final KeyMapping HOTBAR_DOWN = new KeyMapping("key.indicatorbars.hotbar_down", InputConstants.KEY_DOWN, KEY_CATEGORY);

	private static final TargetTracker TARGET = new TargetTracker();
	private static final EquipmentTracker EQUIPMENT = new EquipmentTracker();
	private static final CropTracker CROPS = new CropTracker();
	private static IBConfig config;

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
				minecraft.player.sendOverlayMessage(Component.translatable(
						config.health.showBars ? "indicatorbars.bars.on" : "indicatorbars.bars.off"));
			}
		}
		while (TOGGLE_DURABILITY.consumeClick()) {
			config.durability.showNumbers = !config.durability.showNumbers;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.sendOverlayMessage(Component.translatable(
						config.durability.showNumbers ? "indicatorbars.durability.on" : "indicatorbars.durability.off"));
			}
		}
		while (TOGGLE_CROPS.consumeClick()) {
			config.crops.showBars = !config.crops.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.sendOverlayMessage(Component.translatable(
						config.crops.showBars ? "indicatorbars.crops.on" : "indicatorbars.crops.off"));
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

	/** The level's features are being submitted; the pose stack has the camera at the origin. */
	public static void submitCropBars(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (config.crops.showBars && camera.initialized) {
			CropBarRenderer.submit(CROPS.crops(), poseStack, collector, camera, config.crops);
		}
	}

	/** A living entity's render state was extracted: decide its bar while the entity is at hand. */
	public static void extractBar(LivingEntity entity, LivingEntityRenderState state) {
		HealthBarState.Bar bar = ((HealthBarState) state).indicatorbars$bar();
		bar.target = entity == TARGET.target();
		bar.entityId = entity.getId();
		bar.visible = BarVisibility.shouldShow(entity, Minecraft.getInstance(), config.health, TARGET.target());
		if (bar.visible) {
			bar.fraction = MeterStyle.fraction(entity.getHealth(), entity.getMaxHealth());
			bar.color = HealthColors.fill(entity, bar.fraction, config.health.barColor);
		}
	}

	/** A living entity was submitted for drawing; the pose stack is at its feet. */
	public static void submitBar(PortraitRenderer renderer, LivingEntityRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera) {
		// GUI previews (inventory, horse screen, our own portrait) draw with a camera that was never set up.
		if (!camera.initialized) {
			return;
		}
		HealthBarState.Bar bar = ((HealthBarState) state).indicatorbars$bar();
		if (bar.target) {
			PortraitFit.capture(renderer, bar.entityId);
		}
		if (bar.visible) {
			WorldBarRenderer.submit(bar, state, poseStack, collector, camera, config.health);
		}
	}

	/** The mob health panel, or the crop panel while you look at a crop and not a mob. */
	public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Crop crop = CROPS.target();
		if (config.crops.showPanel && crop != null && !TARGET.seen()) {
			CropPanelRenderer.extract(graphics, crop, config.crops);
			return;
		}
		LivingEntity target = TARGET.target();
		if (!config.health.showPanel || target == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float alpha = TARGET.alpha(config.health, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.extract(graphics, target, alpha, partialTick, config.health);
		}
	}

	/** The durability column. */
	public static void extractEquipmentHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		EquipmentHudRenderer.extract(graphics, config.durability, deltaTracker.getGameTimeDeltaPartialTick(false));
	}

	/** Which hotbar is in use. */
	public static void extractHotbarIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		HotbarIndicatorRenderer.extract(graphics, config.hotbars);
	}

	/** An item was drawn in a GUI slot (hotbar, inventory screens). */
	public static void extractSlot(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
		SlotCounterRenderer.extract(graphics, font, stack, x, y, config.durability);
	}

	/**
	 * A screen was built. The player's inventory gets the sort button, and any crafting screen the
	 * quick-craft arrow. Returns what must run each frame to keep them in place, or null if nothing was added.
	 */
	public static Runnable attachScreenButtons(AbstractRecipeBookScreen<?> screen, List<AbstractWidget> widgets) {
		SortButton sort = screen instanceof InventoryScreen inventory && config.sort.showButton ? SortButton.attach(inventory, widgets, config) : null;
		QuickCraftButton craft = screen.getMenu() instanceof AbstractCraftingMenu menu && config.craft.showButton
				? QuickCraftButton.attach(screen, widgets, menu) : null;
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
