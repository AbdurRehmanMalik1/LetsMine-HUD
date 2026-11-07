package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** NeoForge entrypoint. The floating bars hook in through {@code LivingEntityRendererMixin}. */
@Mod(value = IndicatorBars.MOD_ID, dist = Dist.CLIENT)
public final class IndicatorBarsClient {
	/** The screen whose buttons are showing, and what keeps them in place each frame. */
	private static Screen followedScreen;
	private static Runnable followButtons;

	public IndicatorBarsClient(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(ClientHooks.KEY_CATEGORY);
			event.register(ClientHooks.TOGGLE_BARS);
			event.register(ClientHooks.TOGGLE_DURABILITY);
			event.register(ClientHooks.TOGGLE_CROPS);
			event.register(ClientHooks.CYCLE_HOTBAR);
			event.register(ClientHooks.HOTBAR_UP);
			event.register(ClientHooks.HOTBAR_DOWN);
		});
		modBus.addListener((RegisterGuiLayersEvent event) -> {
			event.registerAboveAll(IndicatorBars.id("target_panel"), ClientHooks::renderHud);
			event.registerAboveAll(IndicatorBars.id("durability_hud"), ClientHooks::renderEquipmentHud);
			event.registerAboveAll(IndicatorBars.id("hotbar_indicator"), ClientHooks::renderHotbarIndicator);
		});
		modBus.addListener(IndicatorBarsClient::registerSlotCounter);
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientHooks.tick(Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
			List<AbstractWidget> widgets = new ArrayList<>();
			for (GuiEventListener listener : event.getListenersList()) {
				if (listener instanceof AbstractWidget widget) {
					widgets.add(widget);
				}
			}
			int existing = widgets.size();
			followedScreen = event.getScreen();
			followButtons = ClientHooks.attachScreenButtons(followedScreen, widgets);
			for (AbstractWidget added : new ArrayList<>(widgets.subList(existing, widgets.size()))) {
				event.addListener(added);
			}
		});
		NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event) -> {
			if (followButtons != null && event.getScreen() == followedScreen) {
				followButtons.run();
			}
		});
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
		// Crops have no renderer of their own. This version's stages give no submit collector, so the bars are drawn here.
		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterEntities event) -> {
			MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
			ClientHooks.renderCropBars(event.getPoseStack(), buffers, event.getLevelRenderState().cameraRenderState);
			buffers.endBatch();
		});
	}

	/**
	 * NeoForge decorates per item, so the durability number is registered on every item; it skips stacks
	 * that can't wear down. Asking which items can is not possible here: the event fires before item
	 * components are bound.
	 */
	private static void registerSlotCounter(RegisterItemDecorationsEvent event) {
		IItemDecorator counter = (graphics, font, stack, x, y) -> {
			ClientHooks.renderSlot(graphics, font, stack, x, y);
			return false;
		};
		for (Item item : BuiltInRegistries.ITEM) {
			event.register(item, counter);
		}
	}
}
