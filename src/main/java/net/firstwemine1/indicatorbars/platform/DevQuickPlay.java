package net.firstwemine1.indicatorbars.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Dev runs only ({@code ./gradlew runClient -PquickPlay=World}): opens that world as soon as the title
 * screen shows, standing in for {@code --quickPlaySingleplayer}, which arrived in 1.20. Does nothing
 * unless the {@code indicatorbars.quickPlay} system property is set.
 */
final class DevQuickPlay {
	private static boolean opened;

	private DevQuickPlay() {
	}

	static void register(IEventBus bus) {
		String world = System.getProperty("indicatorbars.quickPlay");
		if (world == null) {
			return;
		}
		bus.addListener((ScreenEvent.Init.Post event) -> {
			if (event.getScreen() instanceof TitleScreen title && !opened) {
				opened = true;
				Minecraft minecraft = Minecraft.getInstance();
				// Not from inside the title screen's own init.
				minecraft.tell(() -> minecraft.createWorldOpenFlows().loadLevel(title, world));
			}
		});
	}
}
