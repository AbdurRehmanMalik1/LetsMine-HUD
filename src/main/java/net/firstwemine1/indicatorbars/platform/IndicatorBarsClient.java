package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.firstwemine1.indicatorbars.render.LateTooltip;

/** Client setup: turns Forge events into {@link ClientHooks} calls. Bars use events, not mixins. */
final class IndicatorBarsClient {
	/** Vanilla draws container items in front of the screen's blit offset by this much. */
	private static final float CONTAINER_BLIT_OFFSET = 100;

	/** The screen whose buttons are showing, and what keeps them in place each frame. */
	private static Screen followedScreen;
	private static Runnable followButtons;

	private IndicatorBarsClient() {
	}

	static void init(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_BARS);
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_DURABILITY);
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_CROPS);
			ClientRegistry.registerKeyBinding(ClientHooks.CYCLE_HOTBAR);
			ClientRegistry.registerKeyBinding(ClientHooks.HOTBAR_UP);
			ClientRegistry.registerKeyBinding(ClientHooks.HOTBAR_DOWN);
		}));

		IEventBus bus = MinecraftForge.EVENT_BUS;
		DevQuickPlay.register(bus);
		// 1.16.5 has no overlay registry: everything is drawn after the whole HUD, including the hotbar
		// counters (there are no item decorations either).
		bus.addListener((RenderGameOverlayEvent.Post event) -> {
			if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
				ClientHooks.renderHotbarIndicator(event.getMatrixStack());
				ClientHooks.renderHud(event.getMatrixStack(), event.getPartialTicks());
				ClientHooks.renderEquipmentHud(event.getMatrixStack(), event.getPartialTicks());
				ClientHooks.renderHotbarCounters(event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
			}
		});
		// PlayerInventory screens draw their numbers over each slot, in the screen's own coordinates.
		bus.addListener((GuiContainerEvent.DrawForeground event) -> {
			Minecraft minecraft = Minecraft.getInstance();
			for (Slot slot : event.getGuiContainer().getMenu().slots) {
				ClientHooks.renderSlot(minecraft.font, slot.getItem(), slot.x, slot.y, CONTAINER_BLIT_OFFSET);
			}
		});
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				ClientHooks.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((GuiScreenEvent.InitGuiEvent.Post event) -> {
			List<Widget> widgets = new ArrayList<>();
			for (Widget listener : event.getWidgetList()) {
				if (listener instanceof Widget) {
					widgets.add((Widget) listener);
				}
			}
			int existing = widgets.size();
			followedScreen = event.getGui();
			followButtons = ClientHooks.attachScreenButtons(followedScreen, widgets);
			for (Widget added : new ArrayList<>(widgets.subList(existing, widgets.size()))) {
				event.addWidget(added);
			}
		});
		bus.addListener((GuiScreenEvent.DrawScreenEvent.Pre event) -> {
			if (followButtons != null && event.getGui() == followedScreen) {
				followButtons.run();
			}
		});
		bus.addListener((GuiScreenEvent.DrawScreenEvent.Post event) -> LateTooltip.draw(event.getGui(), event.getMatrixStack(), event.getMouseX(), event.getMouseY()));
		bus.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) -> ClientHooks.reset());
		// No level render stages yet: a frame starts with the level, and RenderWorldLastEvent ends it.
		// GUI entity previews come after, so they get no bar.
		bus.addListener((TickEvent.RenderTickEvent event) -> {
			if (event.phase == TickEvent.Phase.START) {
				ClientHooks.setRenderingLevel(true);
			}
		});
		bus.addListener((RenderWorldLastEvent event) -> {
			ClientHooks.setRenderingLevel(false);
			// Crops have no renderer of their own, and this is the only level event there is.
			Minecraft minecraft = Minecraft.getInstance();
			IRenderTypeBuffer.Impl buffers = minecraft.renderBuffers().bufferSource();
			ClientHooks.renderCropBars(event.getMatrixStack(), buffers, minecraft.gameRenderer.getMainCamera());
			buffers.endBatch();
		});
		bus.addListener(EventPriority.NORMAL, false, RenderLivingEvent.Post.class,
				event -> ClientHooks.renderBar(event.getEntity(), event.getMatrixStack(), event.getBuffers()));
		bus.addListener((RenderNameplateEvent event) -> {
			if (TargetPanelRenderer.drawingPortrait()) {
				event.setResult(Event.Result.DENY);
			}
		});
	}
}
