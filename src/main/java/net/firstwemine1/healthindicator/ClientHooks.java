package net.firstwemine1.healthindicator;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.healthindicator.core.BarStyle;
import net.firstwemine1.healthindicator.core.BarVisibility;
import net.firstwemine1.healthindicator.core.HIConfig;
import net.firstwemine1.healthindicator.core.TargetTracker;
import net.firstwemine1.healthindicator.render.TargetPanelRenderer;
import net.firstwemine1.healthindicator.render.WorldBarRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint only turns its own events into
 * these calls, so this class and everything under it is the same on every loader of this Minecraft
 * version.
 */
public final class ClientHooks {
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.healthindicator.toggle_bars", InputConstants.KEY_H,
			"key.categories." + HealthIndicator.MOD_ID);

	private static final TargetTracker TARGET = new TargetTracker();
	private static HIConfig config;
	/**
	 * True while the level renders. GUI entity previews (inventory, horse screen, our own portrait)
	 * fire the same render events, and get no bar.
	 */
	private static boolean renderingLevel;

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
				minecraft.player.displayClientMessage(Component.translatable(
						config.showBars ? "healthindicator.bars.on" : "healthindicator.bars.off"), true);
			}
		}
		if (minecraft.level == null) {
			TARGET.reset();
			return;
		}
		TARGET.tick(minecraft, config);
	}

	public static void setRenderingLevel(boolean rendering) {
		renderingLevel = rendering;
	}

	/** A living entity was just rendered; the pose stack is at its feet. */
	public static void renderBar(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffers) {
		if (!renderingLevel || !BarVisibility.shouldShow(entity, Minecraft.getInstance(), config, TARGET.target())) {
			return;
		}
		float fraction = BarStyle.fraction(entity.getHealth(), entity.getMaxHealth());
		int color = BarStyle.fillColor(entity, fraction, config.barColor);
		WorldBarRenderer.render(entity, fraction, color, poseStack, buffers, config);
	}

	public static void renderHud(GuiGraphics graphics, float partialTick) {
		LivingEntity target = TARGET.target();
		if (!config.showPanel || target == null) {
			return;
		}
		float alpha = TARGET.alpha(config, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.render(graphics, target, alpha, config);
		}
	}

	/** Disconnected: forget the target. */
	public static void reset() {
		TARGET.reset();
	}
}
