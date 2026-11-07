package net.firstwemine1.indicatorbars;

import java.nio.file.Path;

import com.mojang.blaze3d.matrix.MatrixStack;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.firstwemine1.indicatorbars.render.HotbarIndicatorRenderer;
import net.firstwemine1.indicatorbars.render.QuickCraftButton;
import net.firstwemine1.indicatorbars.render.SortButton;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CraftingScreen;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import java.util.List;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint only turns its own events into
 * these calls, so this class and everything under it is the same on every loader of this Minecraft
 * version.
 */
public final class ClientHooks {
	public static final KeyBinding TOGGLE_BARS = new KeyBinding("key.indicatorbars.toggle_bars", GLFW.GLFW_KEY_H,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyBinding TOGGLE_DURABILITY = new KeyBinding("key.indicatorbars.toggle_durability", GLFW.GLFW_KEY_J,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyBinding TOGGLE_CROPS = new KeyBinding("key.indicatorbars.toggle_crops", GLFW.GLFW_KEY_K,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyBinding CYCLE_HOTBAR = new KeyBinding("key.indicatorbars.cycle_hotbar", GLFW.GLFW_KEY_G,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyBinding HOTBAR_UP = new KeyBinding("key.indicatorbars.hotbar_up", GLFW.GLFW_KEY_UP,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyBinding HOTBAR_DOWN = new KeyBinding("key.indicatorbars.hotbar_down", GLFW.GLFW_KEY_DOWN,
			"key.categories." + IndicatorBars.MOD_ID);

	private static final TargetTracker TARGET = new TargetTracker();
	private static final EquipmentTracker EQUIPMENT = new EquipmentTracker();
	private static final CropTracker CROPS = new CropTracker();
	private static IBConfig config;
	/**
	 * True while the level renders. GUI entity previews (inventory, horse screen, our own portrait)
	 * fire the same render events, and get no bar.
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
				minecraft.player.displayClientMessage(new TranslationTextComponent(
						config.health.showBars ? "indicatorbars.bars.on" : "indicatorbars.bars.off"), true);
			}
		}
		while (TOGGLE_DURABILITY.consumeClick()) {
			config.durability.showNumbers = !config.durability.showNumbers;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(new TranslationTextComponent(
						config.durability.showNumbers ? "indicatorbars.durability.on" : "indicatorbars.durability.off"), true);
			}
		}
		while (TOGGLE_CROPS.consumeClick()) {
			config.crops.showBars = !config.crops.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(new TranslationTextComponent(
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

	/** The level is drawn; the matrix stack has the camera at the origin. */
	public static void renderCropBars(MatrixStack poseStack, IRenderTypeBuffer buffers, ActiveRenderInfo camera) {
		if (config.crops.showBars) {
			CropBarRenderer.render(CROPS.crops(), poseStack, buffers, camera, config.crops);
		}
	}

	public static void setRenderingLevel(boolean rendering) {
		renderingLevel = rendering;
	}

	/** A living entity was just rendered; the matrix stack is at its feet. */
	public static void renderBar(LivingEntity entity, MatrixStack poseStack, IRenderTypeBuffer buffers) {
		if (!renderingLevel || !BarVisibility.shouldShow(entity, Minecraft.getInstance(), config.health, TARGET.target())) {
			return;
		}
		float fraction = MeterStyle.fraction(entity.getHealth(), entity.getMaxHealth());
		int color = HealthColors.fill(entity, fraction, config.health.barColor);
		WorldBarRenderer.render(entity, fraction, color, poseStack, buffers, config.health);
	}

	/** The mob health panel, or the crop panel while you look at a crop and not a mob. */
	public static void renderHud(MatrixStack poseStack, float partialTick) {
		Crop crop = CROPS.target();
		if (config.crops.showPanel && crop != null && !TARGET.seen()) {
			CropPanelRenderer.render(poseStack, crop, config.crops);
			return;
		}
		LivingEntity target = TARGET.target();
		if (!config.health.showPanel || target == null) {
			return;
		}
		float alpha = TARGET.alpha(config.health, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.render(poseStack, target, alpha, config.health);
		}
	}

	/** The durability column. */
	public static void renderEquipmentHud(MatrixStack poseStack, float partialTick) {
		EquipmentHudRenderer.render(poseStack, config.durability, partialTick);
	}

	/** An item was drawn in a GUI slot (inventory screens). */
	public static void renderSlot(FontRenderer font, ItemStack stack, int x, int y, float blitOffset) {
		SlotCounterRenderer.render(font, stack, x, y, blitOffset, config.durability);
	}

	/** The hotbar's slot counters (1.16.5 has no item decorations). */
	public static void renderHotbarCounters(int width, int height) {
		SlotCounterRenderer.renderHotbar(width, height, config.durability);
	}

	/** Which hotbar is in use. */
	public static void renderHotbarIndicator(MatrixStack poseStack) {
		HotbarIndicatorRenderer.render(poseStack, config.hotbars);
	}

	/**
	 * A screen was built. The player's inventory gets the sort button, and any crafting screen the
	 * quick-craft button. Returns what must run each frame to keep them in place, or null if nothing was added.
	 */
	public static Runnable attachScreenButtons(Screen screen, List<Widget> widgets) {
		if (!(screen instanceof InventoryScreen || screen instanceof CraftingScreen) || !(((ContainerScreen<?>) screen).getMenu() instanceof RecipeBookContainer)) {
			return null;
		}
		SortButton sort = screen instanceof InventoryScreen && config.sort.showButton ? SortButton.attach((InventoryScreen) screen, widgets, config) : null;
		QuickCraftButton craft = config.craft.showButton ? QuickCraftButton.attach((ContainerScreen<?>) screen, widgets, (RecipeBookContainer<?>) ((ContainerScreen<?>) screen).getMenu()) : null;
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
