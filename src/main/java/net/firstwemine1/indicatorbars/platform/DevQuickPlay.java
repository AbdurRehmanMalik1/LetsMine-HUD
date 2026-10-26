package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.IndicatorBars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Dev runs only. {@code ./gradlew runClient -PquickPlay=World} opens that world as soon as the title
 * screen shows, standing in for {@code --quickPlaySingleplayer}, which arrived in 1.20. Adding
 * {@code -PtestScene} then plays the testing/ib_test scenes by itself, screenshots each into
 * run/client/screenshots and quits: the join scene (cows), a pig portrait, worn armor and tools, and the
 * inventory screen. Does nothing unless those system properties are set.
 */
final class DevQuickPlay {
	/** Ticks after joining at which each step runs. */
	private static final int[] STEPS = {100, 120, 180, 200, 260, 280, 300, 320};

	private static boolean opened;
	private static int ticks = -1;

	private DevQuickPlay() {
	}

	static void register(IEventBus bus) {
		String world = System.getProperty("indicatorbars.quickPlay");
		if (world == null) {
			return;
		}
		bus.addListener((ScreenEvent.InitScreenEvent.Post event) -> {
			if (event.getScreen() instanceof TitleScreen && !opened) {
				opened = true;
				Minecraft minecraft = Minecraft.getInstance();
				// Not from inside the title screen's own init.
				minecraft.tell(() -> minecraft.loadLevel(world));
			}
		});
		if (Boolean.getBoolean("indicatorbars.testScene")) {
			bus.addListener((TickEvent.ClientTickEvent event) -> {
				if (event.phase == TickEvent.Phase.END) {
					testScene(Minecraft.getInstance());
				}
			});
		}
	}

	private static void testScene(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}
		ticks++;
		if (ticks == STEPS[0] || ticks == STEPS[2] || ticks == STEPS[4] || ticks == STEPS[6]) {
			Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(),
					message -> IndicatorBars.LOGGER.info("Test scene: {}", message.getString()));
		} else if (ticks == STEPS[1]) {
			minecraft.player.chat("/function ib_test:portrait/pig");
		} else if (ticks == STEPS[3]) {
			minecraft.player.chat("/function ib_test:durability");
		} else if (ticks == STEPS[5]) {
			minecraft.setScreen(new InventoryScreen(minecraft.player));
		} else if (ticks == STEPS[7]) {
			minecraft.stop();
		}
	}
}
