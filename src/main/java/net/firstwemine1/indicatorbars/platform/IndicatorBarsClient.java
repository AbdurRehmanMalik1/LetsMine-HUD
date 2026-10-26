package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
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
import net.minecraftforge.registries.ForgeRegistries;

/** Client setup: turns Forge events into {@link ClientHooks} calls. Bars use events, not mixins. */
final class IndicatorBarsClient {
	private IndicatorBarsClient() {
	}

	/** Forge decorates per item, so the durability number is registered on every item; it skips the rest. */
	private static void registerSlotCounter(RegisterItemDecorationsEvent event) {
		IItemDecorator counter = (font, stack, x, y, blitOffset) -> {
			ClientHooks.renderSlot(font, stack, x, y, blitOffset);
			return false;
		};
		for (Item item : ForgeRegistries.ITEMS) {
			event.register(item, counter);
		}
	}

	static void init(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.register(ClientHooks.TOGGLE_BARS);
			event.register(ClientHooks.TOGGLE_DURABILITY);
		});
		modBus.addListener((RegisterGuiOverlaysEvent event) -> {
			event.registerAboveAll("target_panel", (gui, poseStack, partialTick, width, height) -> ClientHooks.renderHud(poseStack, partialTick));
			event.registerAboveAll("durability_hud", (gui, poseStack, partialTick, width, height) -> ClientHooks.renderEquipmentHud(poseStack, partialTick));
		});
		modBus.addListener(IndicatorBarsClient::registerSlotCounter);

		IEventBus bus = MinecraftForge.EVENT_BUS;
		DevQuickPlay.register(bus);
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				ClientHooks.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
		// Entities render between the solid blocks and the weather (1.19.2 has no AFTER_LEVEL stage).
		bus.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
				ClientHooks.setRenderingLevel(true);
			} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
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
