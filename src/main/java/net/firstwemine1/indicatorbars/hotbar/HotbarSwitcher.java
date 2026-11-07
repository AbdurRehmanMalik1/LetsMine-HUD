package net.firstwemine1.indicatorbars.hotbar;

import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.HotbarsConfig;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Extra hotbars, kept in the inventory rows. The server only knows the real hotbar, so bar 1's items
 * move out to the active bar's row while that bar is in the hotbar, and move back when it leaves. Every
 * move is the swap vanilla sends for a number key pressed over a slot, so any server accepts it.
 *
 * <p>Bars count from 0 here (bar 0 is the one with no row of its own); players see them from 1.
 */
public final class HotbarSwitcher {
	private static final int ROW_SIZE = Inventory.getSelectionSize();

	private HotbarSwitcher() {
	}

	/** The bar {@code delta} away, wrapping around. A bar past {@code count} (the config was lowered) steps back in. */
	public static int step(int active, int count, int delta) {
		return Math.floorMod(active + delta, count);
	}

	/** The menu slot of a bar's first item. Rows are used from the bottom up, because pickups fill them last. */
	public static int rowStart(int bar) {
		return 36 - ROW_SIZE * bar;
	}

	/** Moves the active bar by {@code delta}. Does nothing while a chest or other container is open, or during a sort. */
	public static void change(Minecraft minecraft, IBConfig config, int delta) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null || player.isSpectator() || player.containerMenu != player.inventoryMenu || InventorySorter.isRunning()) {
			return;
		}
		HotbarsConfig hotbars = config.hotbars;
		String world = worldKey(minecraft);
		int active = hotbars.active(world);
		int next = step(active, hotbars.count, delta);
		if (next == active) {
			player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("indicatorbars.hotbars.only_one"), true);
			return;
		}
		// Bar 0 goes home first, then out to the next bar's row.
		if (active != 0) {
			swapRow(minecraft, active);
		}
		if (next != 0) {
			swapRow(minecraft, next);
		}
		hotbars.setActive(world, next);
		config.save();
	}

	/** Swaps the hotbar with a bar's row, skipping slots empty on both sides. Returns how many swaps it sent. */
	public static int swapRow(Minecraft minecraft, int bar) {
		LocalPlayer player = minecraft.player;
		MultiPlayerGameMode gameMode = minecraft.gameMode;
		Inventory inventory = player.inventory;
		int start = rowStart(bar);
		int swaps = 0;
		for (int i = 0; i < ROW_SIZE; i++) {
			// Above the hotbar, inventory indices and this menu's slot numbers are the same.
			if (!inventory.getItem(start + i).isEmpty() || !inventory.getItem(i).isEmpty()) {
				gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, start + i, i, ClickType.SWAP, player);
				swaps++;
			}
		}
		return swaps;
	}

	/** Which world's rows hold what: remembered per save or server, so a rejoin doesn't lose track. */
	public static String worldKey(Minecraft minecraft) {
		IntegratedServer local = minecraft.getSingleplayerServer();
		if (local != null) {
			return "world:" + local.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
		}
		ServerData server = minecraft.getCurrentServer();
		return server == null ? "unknown" : "server:" + server.ip;
	}
}
