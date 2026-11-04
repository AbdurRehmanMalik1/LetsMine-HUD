package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.DrawItemStackOverlayCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/**
 * Fabric entrypoint. The floating mob bars hook in through {@code LivingEntityRendererMixin}; crops
 * have no renderer of their own, so their bars go in with the level's other submits.
 */
public class IndicatorBarsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHooks.init(FabricLoader.getInstance().getConfigDir());
		KeyMapping.Category.register(ClientHooks.KEY_CATEGORY.id());
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_BARS);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_DURABILITY);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_CROPS);
		ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> ClientHooks.submitCropBars(
				context.matrices(), context.commandQueue(), context.worldState().cameraRenderState));
		HudElementRegistry.addLast(IndicatorBars.id("target_panel"), ClientHooks::renderHud);
		HudElementRegistry.addLast(IndicatorBars.id("durability_hud"), ClientHooks::renderEquipmentHud);
		DrawItemStackOverlayCallback.EVENT.register(ClientHooks::renderSlot);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.reset());
	}
}
