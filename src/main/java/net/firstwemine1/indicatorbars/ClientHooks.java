package net.firstwemine1.indicatorbars;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.core.BarStyle;
import net.firstwemine1.indicatorbars.core.BarVisibility;
import net.firstwemine1.indicatorbars.core.IBConfig;
import net.firstwemine1.indicatorbars.core.TargetTracker;
import net.firstwemine1.indicatorbars.render.PortraitFit;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.firstwemine1.indicatorbars.render.WorldBarRenderer;
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
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H,
			"key.categories." + IndicatorBars.MOD_ID);

	private static final TargetTracker TARGET = new TargetTracker();
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
			config.showBars = !config.showBars;
			config.save();
			if (minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.translatable(
						config.showBars ? "indicatorbars.bars.on" : "indicatorbars.bars.off"), true);
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
		PortraitFit.reset();
	}
}
