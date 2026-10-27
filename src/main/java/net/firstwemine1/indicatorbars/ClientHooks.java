package net.firstwemine1.indicatorbars;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.durability.EquipmentTracker;
import net.firstwemine1.indicatorbars.meter.health.BarVisibility;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.firstwemine1.indicatorbars.meter.health.TargetTracker;
import net.firstwemine1.indicatorbars.render.EquipmentHudRenderer;
import net.firstwemine1.indicatorbars.render.HealthBarState;
import net.firstwemine1.indicatorbars.render.PortraitFit;
import net.firstwemine1.indicatorbars.render.PortraitRenderer;
import net.firstwemine1.indicatorbars.render.SlotCounterRenderer;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.firstwemine1.indicatorbars.render.WorldBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the loader-specific code calls. Each loader's entrypoint and mixins only turn its own
 * events into these calls, so this class and everything under it is the same on every loader.
 */
public final class ClientHooks {
	public static final KeyMapping TOGGLE_BARS = new KeyMapping("key.indicatorbars.toggle_bars", InputConstants.KEY_H,
			"key.categories." + IndicatorBars.MOD_ID);
	public static final KeyMapping TOGGLE_DURABILITY = new KeyMapping("key.indicatorbars.toggle_durability", InputConstants.KEY_J,
			"key.categories." + IndicatorBars.MOD_ID);

	private static final TargetTracker TARGET = new TargetTracker();
	private static final EquipmentTracker EQUIPMENT = new EquipmentTracker();
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
		if (minecraft.level == null) {
			reset();
			return;
		}
		TARGET.tick(minecraft, config.health);
		EQUIPMENT.tick(minecraft, config.durability);
	}

	public static void setRenderingLevel(boolean rendering) {
		renderingLevel = rendering;
	}

	/** A living entity's render state was extracted: decide its bar while the entity is at hand. */
	public static void extractBar(LivingEntity entity, LivingEntityRenderState state) {
		if (TargetPanelRenderer.drawingPortrait()) {
			state.nameTag = null;
		}
		HealthBarState.Bar bar = ((HealthBarState) state).indicatorbars$bar();
		bar.target = entity == TARGET.target();
		bar.entityId = entity.getId();
		bar.visible = BarVisibility.shouldShow(entity, Minecraft.getInstance(), config.health, TARGET.target());
		if (bar.visible) {
			bar.fraction = MeterStyle.fraction(entity.getHealth(), entity.getMaxHealth());
			bar.color = HealthColors.fill(entity, bar.fraction, config.health.barColor);
		}
	}

	/** A living entity was just rendered; the pose stack is at its feet. */
	public static void renderBar(PortraitRenderer renderer, LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers) {
		if (!renderingLevel) {
			return;
		}
		HealthBarState.Bar bar = ((HealthBarState) state).indicatorbars$bar();
		if (bar.target) {
			PortraitFit.capture(renderer, bar.entityId);
		}
		if (bar.visible) {
			WorldBarRenderer.render(bar, state, poseStack, buffers, config.health);
		}
	}

	/** The mob health panel. */
	public static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		LivingEntity target = TARGET.target();
		if (!config.health.showPanel || target == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		float alpha = TARGET.alpha(config.health, partialTick);
		if (alpha > 0) {
			TargetPanelRenderer.render(graphics, target, alpha, partialTick, config.health);
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

	/** Disconnected or left the world: forget the target and the equipment. */
	public static void reset() {
		TARGET.reset();
		EQUIPMENT.reset();
		PortraitFit.reset();
	}
}
