package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;

/**
 * Fabric entrypoint. The floating mob bars hook in through {@code LivingEntityRendererMixin}; crops
 * have no renderer of their own, so their bars go in with the level's other submits.
 */
public class IndicatorBarsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHooks.init(FabricLoader.getInstance().getConfigDir());
		KeyMapping.Category.register(ClientHooks.KEY_CATEGORY.id());
		KeyMappingHelper.registerKeyMapping(ClientHooks.TOGGLE_BARS);
		KeyMappingHelper.registerKeyMapping(ClientHooks.TOGGLE_DURABILITY);
		KeyMappingHelper.registerKeyMapping(ClientHooks.TOGGLE_CROPS);
		KeyMappingHelper.registerKeyMapping(ClientHooks.CYCLE_HOTBAR);
		KeyMappingHelper.registerKeyMapping(ClientHooks.HOTBAR_UP);
		KeyMappingHelper.registerKeyMapping(ClientHooks.HOTBAR_DOWN);
		ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> ClientHooks.submitCropBars(
				context.poseStack(), context.submitNodeCollector(), context.levelState().cameraRenderState));
		HudElementRegistry.addLast(IndicatorBars.id("target_panel"), ClientHooks::extractHud);
		HudElementRegistry.addLast(IndicatorBars.id("durability_hud"), ClientHooks::extractEquipmentHud);
		HudElementRegistry.addLast(IndicatorBars.id("hotbar_indicator"), ClientHooks::extractHotbarIndicator);
		ExtractItemDecorationsCallback.EVENT.register(ClientHooks::extractSlot);
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof AbstractRecipeBookScreen<?> book) {
				Runnable follow = ClientHooks.attachScreenButtons(book, Screens.getWidgets(screen));
				if (follow != null) {
					ScreenEvents.beforeExtract(screen).register((current, graphics, mouseX, mouseY, partialTick) -> follow.run());
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.reset());
	}
}
