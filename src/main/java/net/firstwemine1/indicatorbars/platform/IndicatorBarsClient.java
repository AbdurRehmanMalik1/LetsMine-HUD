package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Fabric entrypoint. The floating bars hook in through {@code LivingEntityRendererMixin}. */
public class IndicatorBarsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHooks.init(FabricLoader.getInstance().getConfigDir());
		KeyMapping.Category.register(ClientHooks.KEY_CATEGORY.id());
		KeyMappingHelper.registerKeyMapping(ClientHooks.TOGGLE_BARS);
		ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
		HudElementRegistry.addLast(IndicatorBars.id("target_panel"), ClientHooks::extractHud);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.reset());
	}
}
