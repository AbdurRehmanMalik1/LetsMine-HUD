package net.firstwemine1.indicatorbars;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.health.BarVisibility;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.firstwemine1.indicatorbars.meter.health.TargetTracker;
import net.firstwemine1.indicatorbars.render.HealthBarState;
import net.firstwemine1.indicatorbars.render.PortraitFit;
import net.firstwemine1.indicatorbars.render.PortraitRenderer;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.firstwemine1.indicatorbars.render.WorldBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint and mixins only turn its own
 * events into these calls, so this class and everything under it is the same on every loader.
 */
public final class ClientHooks {
	/** Registered by each loader's entrypoint: vanilla's registry on Fabric, an event on NeoForge. */
	public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(IndicatorBars.id(IndicatorBars.MOD_ID));
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H, KEY_CATEGORY);

	private static final TargetTracker TARGET = new TargetTracker();
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
		if (minecraft.level == null) {
			TARGET.reset();
			return;
		}
		TARGET.tick(minecraft, config.health);
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

	public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
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

	/** Disconnected: forget the target. */
	public static void reset() {
		TARGET.reset();
		PortraitFit.reset();
	}
}
