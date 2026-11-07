package net.firstwemine1.indicatorbars.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.firstwemine1.indicatorbars.config.IBConfig.SortConfig;
import net.minecraft.core.Registry;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;

/**
 * Works out how to sort the inventory rows: merge partial stacks, then swap each stack into place. It
 * only reads copies, and the moves it returns are what the sorter sends. The hotbar is never involved.
 */
public final class SortPlanner {
	public static final int SLOTS = 36 - 9;
	private static final int ROW = 9;

	/**
	 * Pick up {@code from}, click {@code to}, and put back whatever is left on the cursor: a swap, or a
	 * merge if the two stack. In menu slots, with the stacks each must hold when the move starts.
	 */
	public static final class Move {
		private final int from;
		private final int to;
		private final ItemStack expectedFrom;
		private final ItemStack expectedTo;

		Move(int from, int to, ItemStack expectedFrom, ItemStack expectedTo) {
			this.from = from;
			this.to = to;
			this.expectedFrom = expectedFrom;
			this.expectedTo = expectedTo;
		}

		public int from() {
			return from;
		}

		public int to() {
			return to;
		}

		public ItemStack expectedFrom() {
			return expectedFrom;
		}

		public ItemStack expectedTo() {
			return expectedTo;
		}
	}

	private SortPlanner() {
	}

	/** {@code rows}: the {@value #SLOTS} stacks from the first inventory slot on. Sorted rows give no moves. */
	public static List<Move> plan(List<ItemStack> rows, SortConfig.Metric metric, SortConfig.Preset preset) {
		ItemStack[] slots = new ItemStack[SLOTS];
		for (int i = 0; i < SLOTS; i++) {
			slots[i] = rows.get(i).copy();
		}
		List<Move> moves = new ArrayList<>();
		merge(slots, moves);
		ItemStack[] target = targets(slots, metric, preset);

		boolean[] placed = new boolean[SLOTS];
		for (int to = 0; to < SLOTS; to++) {
			placed[to] = target[to] != null && ItemStack.matches(slots[to], target[to]);
		}
		for (int to = 0; to < SLOTS; to++) {
			if (target[to] == null || placed[to]) {
				continue;
			}
			int from = find(slots, target[to], placed);
			moves.add(new Move(slot(from), slot(to), slots[from].copy(), slots[to].copy()));
			ItemStack moved = slots[from];
			slots[from] = slots[to];
			slots[to] = moved;
			placed[to] = true;
			// The swap may have sent the old stack to where it belongs.
			placed[from] = target[from] != null && ItemStack.matches(slots[from], target[from]);
		}
		return moves;
	}

	private static int slot(int index) {
		return 9 + index;
	}

	/** Tops up each partial stack from later ones of the same item. */
	private static void merge(ItemStack[] slots, List<Move> moves) {
		for (int to = 0; to < SLOTS; to++) {
			ItemStack into = slots[to];
			for (int from = to + 1; from < SLOTS && !into.isEmpty() && into.getCount() < into.getMaxStackSize(); from++) {
				ItemStack other = slots[from];
				if (!other.isEmpty() && ItemStack.isSame(into, other) && ItemStack.tagMatches(into, other)) {
					moves.add(new Move(slot(from), slot(to), other.copy(), into.copy()));
					int amount = Math.min(into.getMaxStackSize() - into.getCount(), other.getCount());
					into.grow(amount);
					other.shrink(amount);
					if (other.isEmpty()) {
						slots[from] = ItemStack.EMPTY;
					}
				}
			}
		}
	}

	/** Which stack goes where. Null: nothing does. */
	private static ItemStack[] targets(ItemStack[] slots, SortConfig.Metric metric, SortConfig.Preset preset) {
		List<ItemStack> picked = new ArrayList<>();
		List<ItemStack> rest = new ArrayList<>();
		for (ItemStack stack : slots) {
			if (!stack.isEmpty()) {
				(presetRank(stack, preset) >= 0 ? picked : rest).add(stack);
			}
		}
		Comparator<ItemStack> order = order(metric);
		picked.sort(Comparator.<ItemStack>comparingInt(stack -> presetRank(stack, preset)).thenComparing(order));
		rest.sort(order);

		ItemStack[] target = new ItemStack[SLOTS];
		// A preset's items fill from the bottom row upwards, left to right in each row.
		for (int i = 0; i < picked.size(); i++) {
			target[(SLOTS / ROW - 1 - i / ROW) * ROW + i % ROW] = picked.get(i);
		}
		int next = 0;
		for (ItemStack stack : rest) {
			while (target[next] != null) {
				next++;
			}
			target[next] = stack;
		}
		return target;
	}

	/** Where a preset wants this item among its own, or -1 if the preset leaves it alone. */
	static int presetRank(ItemStack stack, SortConfig.Preset preset) {
		ItemKind kind = ItemKind.of(stack);
		if (preset == SortConfig.Preset.NONE) {
			return -1;
		}
		if (preset == SortConfig.Preset.COMBAT) {
			return kind == ItemKind.WEAPON ? 0 : kind == ItemKind.TOOL ? 1 : -1;
		}
		if (stack.getItem() instanceof PickaxeItem) {
			return 0;
		}
		if (stack.getItem() instanceof AxeItem) {
			return 1;
		}
		if (stack.getItem() instanceof ShovelItem) {
			return 2;
		}
		return kind == ItemKind.BLOCK ? 3 : -1;
	}

	/** Never ties on stacks that differ, so a second sort finds nothing to do wherever the first put them. */
	private static Comparator<ItemStack> order(SortConfig.Metric metric) {
		Comparator<ItemStack> byName = Comparator.comparing(stack -> stack.getHoverName().getString().toLowerCase());
		Comparator<ItemStack> first = byName;
		if (metric == SortConfig.Metric.TYPE) {
			first = Comparator.<ItemStack>comparingInt(stack -> ItemKind.of(stack).ordinal())
					.thenComparingInt(ItemKind::shape).thenComparing(byName);
		} else if (metric == SortConfig.Metric.QUANTITY) {
			first = Comparator.<ItemStack>comparingInt(stack -> -stack.getCount()).thenComparing(byName);
		}
		return first.thenComparing(stack -> Registry.ITEM.getKey(stack.getItem()).toString())
				.thenComparingInt(stack -> -stack.getCount())
				.thenComparingInt(stack -> Objects.hashCode(stack.getTag()));
	}

	/** A stack not yet in place that matches. One sitting where it belongs only if there's no other. */
	private static int find(ItemStack[] slots, ItemStack wanted, boolean[] placed) {
		for (int i = 0; i < SLOTS; i++) {
			if (!placed[i] && ItemStack.matches(slots[i], wanted)) {
				return i;
			}
		}
		throw new IllegalStateException("A planned stack went missing: " + wanted);
	}
}
