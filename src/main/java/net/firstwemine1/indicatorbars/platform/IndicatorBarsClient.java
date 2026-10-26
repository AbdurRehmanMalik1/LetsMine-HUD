package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

/** Client setup: turns Forge events into {@link ClientHooks} calls. Bars use events, not mixins. */
final class IndicatorBarsClient {
	private IndicatorBarsClient() {
	}

	static void init(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(ClientHooks.TOGGLE_BARS));
		modBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAboveAll("target_panel",
				(gui, graphics, partialTick, width, height) -> ClientHooks.renderHud(graphics, partialTick)));

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				ClientHooks.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
		// Entities render between the solid blocks and the end of the level.
		bus.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
				ClientHooks.setRenderingLevel(true);
			} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
				ClientHooks.setRenderingLevel(false);
			}
		});
		bus.addListener(EventPriority.NORMAL, false, RenderLivingEvent.Post.class,
				event -> ClientHooks.renderBar(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource()));
		bus.addListener((RenderNameTagEvent event) -> {
			if (TargetPanelRenderer.drawingPortrait()) {
				event.setResult(Event.Result.DENY);
			}
		});
	}
}
