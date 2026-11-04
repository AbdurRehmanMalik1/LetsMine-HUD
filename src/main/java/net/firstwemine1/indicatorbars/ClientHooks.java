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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint only turns its own events into
 * these calls, so this class and everything under it is the same on every loader of this Minecraft
 * version.
 */
public final class ClientHooks {
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping TOGGLE_DURABILITY = new KeyMapping("key.indicatorbars.toggle_durability", InputConstants.KEY_J,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping TOGGLE_CROPS = new KeyMapping("key.indicatorbars.toggle_crops", InputConstants.KEY_K,
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
				minecraft.player.displayClientMessage(new TranslatableComponent(
						config.health.showBars ? "indicatorbars.bars.on" : "indicatorbars.bars.off"), true);
			}
		}
		while (TOGGLE_DURABILITY.consumeClick()) {
			config.durability.showNumbers = !config.durability.showNumbers;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(new TranslatableComponent(
						config.durability.showNumbers ? "indicatorbars.durability.on" : "indicatorbars.durability.off"), true);
			}
		}
		while (TOGGLE_CROPS.consumeClick()) {
			config.crops.showBars = !config.crops.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(new TranslatableComponent(
						config.crops.showBars ? "indicatorbars.crops.on" : "indicatorbars.crops.off"), true);
			}
		}
		if (minecraft.level == null) {
			reset();
			return;
		}
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
	public static void renderHud(PoseStack poseStack, float partialTick) {
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
	public static void renderEquipmentHud(PoseStack poseStack, float partialTick) {
		EquipmentHudRenderer.render(poseStack, config.durability, partialTick);
	}

	/** An item was drawn in a GUI slot (hotbar, inventory screens). */
	public static void renderSlot(Font font, ItemStack stack, int x, int y, float blitOffset) {
		SlotCounterRenderer.render(font, stack, x, y, blitOffset, config.durability);
	}

	/** The hotbar's slot counters (1.18.2 has no item decorations). */
	public static void renderHotbarCounters(int width, int height) {
		SlotCounterRenderer.renderHotbar(width, height, config.durability);
	}

	/** Disconnected or left the world: forget the target, the equipment and the crops. */
	public static void reset() {
		TARGET.reset();
		EQUIPMENT.reset();
		CROPS.reset();
		PortraitFit.reset();
	}
}
