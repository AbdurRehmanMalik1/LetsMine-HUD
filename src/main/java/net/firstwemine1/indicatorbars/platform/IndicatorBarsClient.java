package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.render.TargetPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

/** Client setup: turns Forge events into {@link ClientHooks} calls. Bars use events, not mixins. */
final class IndicatorBarsClient {
	/** Vanilla draws container items in front of the screen's blit offset by this much. */
	private static final float CONTAINER_BLIT_OFFSET = 100;

	private IndicatorBarsClient() {
	}

	static void init(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_BARS);
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_DURABILITY);
			ClientRegistry.registerKeyBinding(ClientHooks.TOGGLE_CROPS);
			OverlayRegistry.registerOverlayTop("Indicator Bars target panel",
					(gui, poseStack, partialTick, width, height) -> ClientHooks.renderHud(poseStack, partialTick));
			OverlayRegistry.registerOverlayTop("Indicator Bars durability",
					(gui, poseStack, partialTick, width, height) -> ClientHooks.renderEquipmentHud(poseStack, partialTick));
			// 1.18.2 has no item decorations: the hotbar's numbers are an overlay drawn over the hotbar...
			OverlayRegistry.registerOverlayAbove(ForgeIngameGui.HOTBAR_ELEMENT, "Indicator Bars hotbar counters",
					(gui, poseStack, partialTick, width, height) -> ClientHooks.renderHotbarCounters(width, height));
		}));

		IEventBus bus = MinecraftForge.EVENT_BUS;
		DevQuickPlay.register(bus);
		// ...and inventory screens draw theirs over each slot, in the screen's own coordinates.
		bus.addListener((ContainerScreenEvent.DrawForeground event) -> {
			Minecraft minecraft = Minecraft.getInstance();
			for (Slot slot : event.getContainerScreen().getMenu().slots) {
				ClientHooks.renderSlot(minecraft.font, slot.getItem(), slot.x, slot.y, CONTAINER_BLIT_OFFSET);
			}
		});
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				ClientHooks.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) -> ClientHooks.reset());
		// Entities render between the solid blocks and the weather.
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
		bus.addListener((RenderNameplateEvent event) -> {
			if (TargetPanelRenderer.drawingPortrait()) {
				event.setResult(Event.Result.DENY);
			}
		});
	}
}
