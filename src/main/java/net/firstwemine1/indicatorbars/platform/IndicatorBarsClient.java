package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.firstwemine1.indicatorbars.render.LateTooltip;
import net.firstwemine1.indicatorbars.render.LateTooltip;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

/**
 * Fabric entrypoint. The floating mob bars hook in through {@code LivingEntityRendererMixin}, the
 * slot counter through {@code ItemRendererMixin}; crops have no renderer of their own, so their bars
 * are drawn after the level's entities.
 */
public class IndicatorBarsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientHooks.init(FabricLoader.getInstance().getConfigDir());
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_BARS);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_DURABILITY);
		KeyBindingHelper.registerKeyBinding(ClientHooks.TOGGLE_CROPS);
		KeyBindingHelper.registerKeyBinding(ClientHooks.CYCLE_HOTBAR);
		KeyBindingHelper.registerKeyBinding(ClientHooks.HOTBAR_UP);
		KeyBindingHelper.registerKeyBinding(ClientHooks.HOTBAR_DOWN);
		ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
		WorldRenderEvents.START.register(context -> ClientHooks.setRenderingLevel(true));
		WorldRenderEvents.END.register(context -> ClientHooks.setRenderingLevel(false));
		WorldRenderEvents.AFTER_ENTITIES.register(context -> ClientHooks.renderCropBars(
				context.matrixStack(), context.consumers(), context.camera(), context.frustum()));
		HudRenderCallback.EVENT.register((poseStack, partialTick) -> {
			ClientHooks.renderHotbarIndicator(poseStack);
			ClientHooks.renderHud(poseStack, partialTick);
			ClientHooks.renderEquipmentHud(poseStack, partialTick);
		});
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			Runnable follow = ClientHooks.attachScreenButtons(screen, Screens.getButtons(screen));
			if (follow != null) {
				ScreenEvents.beforeRender(screen).register((current, poseStack, mouseX, mouseY, partialTick) -> follow.run());
				ScreenEvents.afterRender(screen).register((current, poseStack, mouseX, mouseY, partialTick) -> LateTooltip.draw(current, poseStack, mouseX, mouseY));
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.reset());
	}
}
