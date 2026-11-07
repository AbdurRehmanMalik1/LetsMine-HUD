package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.firstwemine1.indicatorbars.render.LateTooltip;

/** Client setup: turns Forge events into {@link ClientHooks} calls. Bars use events, not mixins. */
final class IndicatorBarsClient {
	/** The screen whose buttons are showing, and what keeps them in place each frame. */
	private static Screen followedScreen;
	private static Runnable followButtons;

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
			event.register(ClientHooks.TOGGLE_CROPS);
			event.register(ClientHooks.CYCLE_HOTBAR);
			event.register(ClientHooks.HOTBAR_UP);
			event.register(ClientHooks.HOTBAR_DOWN);
		});
		modBus.addListener((RegisterGuiOverlaysEvent event) -> {
			event.registerAboveAll("target_panel", (gui, poseStack, partialTick, width, height) -> ClientHooks.renderHud(poseStack, partialTick));
			event.registerAboveAll("durability_hud", (gui, poseStack, partialTick, width, height) -> ClientHooks.renderEquipmentHud(poseStack, partialTick));
			event.registerAboveAll("hotbar_indicator", (gui, poseStack, partialTick, width, height) -> ClientHooks.renderHotbarIndicator(poseStack));
		});
		modBus.addListener(IndicatorBarsClient::registerSlotCounter);

		IEventBus bus = MinecraftForge.EVENT_BUS;
		DevQuickPlay.register(bus);
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				ClientHooks.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((ScreenEvent.Init.Post event) -> {
			List<AbstractWidget> widgets = new ArrayList<>();
			for (net.minecraft.client.gui.components.events.GuiEventListener listener : event.getListenersList()) {
				if (listener instanceof AbstractWidget) {
					widgets.add((AbstractWidget) listener);
				}
			}
			int existing = widgets.size();
			followedScreen = event.getScreen();
			followButtons = ClientHooks.attachScreenButtons(followedScreen, widgets);
			for (AbstractWidget added : new ArrayList<>(widgets.subList(existing, widgets.size()))) {
				event.addListener(added);
			}
		});
		bus.addListener((ScreenEvent.Render.Pre event) -> {
			if (followButtons != null && event.getScreen() == followedScreen) {
				followButtons.run();
			}
		});
		bus.addListener((ScreenEvent.Render.Post event) -> LateTooltip.draw(event.getScreen(), event.getPoseStack(), event.getMouseX(), event.getMouseY()));
		bus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
		// Entities render between the solid blocks and the weather (1.19.2 has no AFTER_LEVEL stage).
		bus.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
				ClientHooks.setRenderingLevel(true);
			} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
				ClientHooks.setRenderingLevel(false);
			}
		});
		// Crops have no renderer of their own, and there is no stage right after the entities here.
		bus.addListener((RenderLevelStageEvent event) -> {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
				MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
				ClientHooks.renderCropBars(event.getPoseStack(), buffers, event.getCamera(), event.getFrustum());
				buffers.endBatch();
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
