package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entrypoint. The floating bars hook in through {@code LivingEntityRendererMixin}. */
@Mod(value = IndicatorBars.MOD_ID, dist = Dist.CLIENT)
public final class IndicatorBarsClient {
	public IndicatorBarsClient(IEventBus modBus) {
		ClientHooks.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(ClientHooks.KEY_CATEGORY);
			event.register(ClientHooks.TOGGLE_BARS);
		});
		modBus.addListener((RegisterGuiLayersEvent event) -> {
			event.registerAboveAll(IndicatorBars.id("target_panel"), ClientHooks::extractHud);
			event.registerAboveAll(IndicatorBars.id("durability_hud"), ClientHooks::extractEquipmentHud);
		});
		modBus.addListener(IndicatorBarsClient::registerSlotCounter);
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientHooks.tick(Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientHooks.reset());
	}

	/** NeoForge decorates per item: register the durability number on every item that can wear down. */
	private static void registerSlotCounter(RegisterItemDecorationsEvent event) {
		IItemDecorator counter = (graphics, font, stack, x, y) -> {
			ClientHooks.extractSlot(graphics, font, stack, x, y);
			return false;
		};
		for (Item item : BuiltInRegistries.ITEM) {
			if (new ItemStack(item).isDamageableItem()) {
				event.register(item, counter);
			}
		}
	}
}
