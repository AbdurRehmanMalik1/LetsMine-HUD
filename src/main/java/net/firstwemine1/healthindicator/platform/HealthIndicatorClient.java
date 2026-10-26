package net.firstwemine1.healthindicator.platform;

import net.firstwemine1.healthindicator.ClientHooks;
import net.firstwemine1.healthindicator.HealthIndicator;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entrypoint. The floating bars hook in through {@code LivingEntityRendererMixin}. */
@Mod(value = HealthIndicator.MOD_ID, dist = Dist.CLIENT)
public final class HealthIndicatorClient {
	public HealthIndicatorClient(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(ClientHooks.KEY_CATEGORY);
			event.register(ClientHooks.TOGGLE_BARS);
		});
		modBus.addListener((RegisterGuiLayersEvent event) ->
				event.registerAboveAll(HealthIndicator.id("target_panel"), ClientHooks::extractHud));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientHooks.tick(Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
	}
}
