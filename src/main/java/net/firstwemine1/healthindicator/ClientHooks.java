package net.firstwemine1.healthindicator;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.healthindicator.core.BarStyle;
import net.firstwemine1.healthindicator.core.BarVisibility;
import net.firstwemine1.healthindicator.core.HIConfig;
import net.firstwemine1.healthindicator.core.TargetTracker;
import net.firstwemine1.healthindicator.render.HealthBarState;
import net.firstwemine1.healthindicator.render.TargetPanelRenderer;
import net.firstwemine1.healthindicator.render.WorldBarRenderer;
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
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.healthindicator.toggle_bars",
			InputConstants.KEY_H, KeyMapping.Category.register(HealthIndicator.id(HealthIndicator.MOD_ID)));

	private static final TargetTracker TARGET = new TargetTracker();
	private static HIConfig config;

	private ClientHooks() {
	}

	public static void init(Path configDir) {
		config = HIConfig.load(configDir.resolve(HealthIndicator.MOD_ID + ".json"));
	}

	/** End of every client tick. */
	public static void tick(Minecraft minecraft) {
		while (TOGGLE_BARS.consumeClick()) {
			config.showBars = !config.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.sendOverlayMessage(Component.translatable(
						config.showBars ? "healthindicator.bars.on" : "healthindicator.bars.off"));
			}
		}
		if (minecraft.level == null) {
			TARGET.reset();
			return;
		}
		TARGET.tick(minecraft, config);
	}

	/** A living entity's render state was extracted: decide its bar while the entity is at hand. */
	public static void extractBar(LivingEntity entity, LivingEntityRenderState state) {
		HealthBarState.Bar bar = ((HealthBarState) state).healthindicator$bar();
		bar.visible = BarVisibility.shouldShow(entity, Minecraft.getInstance(), config, TARGET.target());
		if (bar.visible) {
			bar.fraction = BarStyle.fraction(entity.getHealth(), entity.getMaxHealth());
			bar.color = BarStyle.fillColor(entity, bar.fraction, config.barColor);
		}
	}

	/** A living entity was submitted for drawing; the pose stack is at its feet. */
	public static void submitBar(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HealthBarState.Bar bar = ((HealthBarState) state).healthindicator$bar();
		// GUI previews (inventory, horse screen, our own portrait) draw with a camera that was never set up.
		if (bar.visible && camera.initialized) {
			WorldBarRenderer.submit(bar, state, poseStack, collector, camera, config);
		}
	}

	public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		LivingEntity target = TARGET.target();
		if (!config.showPanel || target == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float alpha = TARGET.alpha(config, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.extract(graphics, target, alpha, partialTick, config);
		}
	}

	/** Disconnected: forget the target. */
	public static void reset() {
		TARGET.reset();
	}
}
