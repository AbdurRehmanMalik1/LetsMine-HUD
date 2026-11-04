package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint. The floating mob bars hook in through {@code LivingEntityRendererMixin}, the
 * slot counter through {@code GuiGraphicsMixin}; crops have no renderer of their own, so their bars
 * are drawn after the level's entities.
 */
public class IndicatorBarsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHooks.init(FabricLoader.getInstance().getConfigDir());
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_BARS);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_DURABILITY);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_CROPS);
		ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
		WorldRenderEvents.START.register(context -> ClientHooks.setRenderingLevel(true));
		WorldRenderEvents.END.register(context -> ClientHooks.setRenderingLevel(false));
		WorldRenderEvents.AFTER_ENTITIES.register(context -> ClientHooks.renderCropBars(
				context.matrixStack(), context.consumers(), context.camera(), context.frustum()));
		HudRenderCallback.EVENT.register((graphics, partialTick) -> {
			ClientHooks.renderHud(graphics, partialTick);
			ClientHooks.renderEquipmentHud(graphics, partialTick);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.reset());
	}
}
