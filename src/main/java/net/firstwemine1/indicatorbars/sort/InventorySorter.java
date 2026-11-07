package net.firstwemine1.indicatorbars.sort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.HotbarsConfig;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Carries out a {@link SortPlanner} plan with the same clicks a player would make, a few moves each tick
 * so a server sees no burst. A move always finishes within its tick, so nothing is left on the cursor.
 *
 * <p>On bar 2 or above, bar 1's items are in that bar's row. They go home first, so they stay out of the
 * sort, and the saved bar is updated each way so it always says what's really in the hotbar.
 */
public final class InventorySorter {
	private static final Deque<SortPlanner.Move> MOVES = new ArrayDeque<>();
	private static boolean running;
	/** The bar to go back to afterwards; 0 if bar 1 was already in the hotbar. */
	private static int returnBar;
	private static int clicks;
	/** A setting changed during a sort: sort again with it once this one is done. */
	private static boolean sortAgain;

	private InventorySorter() {
	}

	public static boolean isRunning() {
		return running;
	}

	/** Sorting clicks sent by the latest sort, for checking a sorted inventory costs nothing. */
	public static int clicks() {
		return clicks;
	}

	/** Starts a sort. False if it can't right now, or there's nothing to do. */
	public static boolean start(Minecraft minecraft, IBConfig config) {
		LocalPlayer player = minecraft.player;
		if (running || player == null || minecraft.gameMode == null || !canClick(player)) {
			if (player != null && !player.containerMenu.getCarried().isEmpty()) {
				player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("indicatorbars.sort.cursor"), true);
			}
			return false;
		}
		String world = HotbarSwitcher.worldKey(minecraft);
		int active = config.hotbars.active(world);
		clicks = 0;
		List<SortPlanner.Move> plan = SortPlanner.plan(homeRows(player.getInventory(), active), config.sort.metric, config.sort.preset);
		if (plan.isEmpty()) {
			return false;
		}
		if (active != 0) {
			HotbarSwitcher.swapRow(minecraft, active);
			config.hotbars.setActive(world, 0);
			config.save();
		}
		returnBar = active;
		MOVES.clear();
		MOVES.addAll(plan);
		running = true;
		return true;
	}

	/** Starts a sort, or if one is going, another right after it, so the newest settings always win. */
	public static void startOrQueue(Minecraft minecraft, IBConfig config) {
		if (running) {
			sortAgain = true;
		} else {
			start(minecraft, config);
		}
	}

	/** The rows as they will be once bar 1 is home: the active bar's row holds what's now in the hotbar. */
	private static List<ItemStack> homeRows(Inventory inventory, int active) {
		List<ItemStack> rows = new ArrayList<>(SortPlanner.SLOTS);
		for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.INV_SLOT_END; slot++) {
			int offset = slot - HotbarSwitcher.rowStart(active);
			rows.add(inventory.getItem(active != 0 && offset >= 0 && offset < Inventory.getSelectionSize() ? offset : slot));
		}
		return rows;
	}

	/** Each client tick. */
	public static void tick(Minecraft minecraft, IBConfig config) {
		if (!running) {
			return;
		}
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null) {
			reset();
			return;
		}
		for (int i = 0; i < config.sort.movesPerTick && !MOVES.isEmpty(); i++) {
			if (!canClick(player) || !perform(minecraft, player, MOVES.poll())) {
				MOVES.clear();
			}
		}
		if (MOVES.isEmpty()) {
			finish(minecraft, player, config);
		}
	}

	/** False, with nothing clicked, if the slots no longer hold what was planned: an item was picked up, say. */
	private static boolean perform(Minecraft minecraft, LocalPlayer player, SortPlanner.Move move) {
		Inventory inventory = player.getInventory();
		if (!ItemStack.matches(inventory.getItem(move.from()), move.expectedFrom())
				|| !ItemStack.matches(inventory.getItem(move.to()), move.expectedTo())) {
			return false;
		}
		click(minecraft, player, move.from());
		click(minecraft, player, move.to());
		if (!player.containerMenu.getCarried().isEmpty()) {
			click(minecraft, player, move.from());
		}
		return true;
	}

	private static void click(Minecraft minecraft, LocalPlayer player, int slot) {
		minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, player);
		clicks++;
	}

	private static void finish(Minecraft minecraft, LocalPlayer player, IBConfig config) {
		running = false;
		// If a chest opened meanwhile, bar 1 just stays in the hotbar, as the saved bar already says.
		if (returnBar != 0 && canClick(player)) {
			HotbarSwitcher.swapRow(minecraft, returnBar);
			HotbarsConfig hotbars = config.hotbars;
			hotbars.setActive(HotbarSwitcher.worldKey(minecraft), returnBar);
			config.save();
		}
		returnBar = 0;
		if (sortAgain) {
			sortAgain = false;
			start(minecraft, config);
		}
	}

	/** Slots can be clicked with nothing held, and only in the player's own inventory. */
	private static boolean canClick(LocalPlayer player) {
		return player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty() && !player.isSpectator();
	}

	/** Left the world: drop whatever is left. */
	public static void reset() {
		running = false;
		returnBar = 0;
		sortAgain = false;
		MOVES.clear();
	}
}
