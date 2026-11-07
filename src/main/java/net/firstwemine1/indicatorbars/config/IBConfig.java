package net.firstwemine1.indicatorbars.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.firstwemine1.indicatorbars.meter.HudAnchor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

/**
 * {@code config/indicatorbars.json}, one section per meter. Created with defaults on first launch; edit it
 * and restart. The toggle keys flip {@code health.showBars}, {@code durability.showNumbers} and
 * {@code crops.showBars} in game and save them.
 */
public class IBConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public HealthConfig health = new HealthConfig();
	public DurabilityConfig durability = new DurabilityConfig();
	public CropsConfig crops = new CropsConfig();
	public HotbarsConfig hotbars = new HotbarsConfig();
	public SortConfig sort = new SortConfig();
	public CraftConfig craft = new CraftConfig();

	private transient Path path;

	public static class HealthConfig {
		public enum BarMode {
			@SerializedName("all") ALL,
			/** Only mobs below full health. */
			@SerializedName("damaged") DAMAGED,
			/** Only the mob in the target panel. */
			@SerializedName("target") TARGET
		}

		public enum HealthText {
			/** {@code 6/10} */
			@SerializedName("fraction") FRACTION,
			/** {@code 60%} */
			@SerializedName("percent") PERCENT,
			/** {@code 3/5}, counted in hearts */
			@SerializedName("hearts") HEARTS,
			@SerializedName("off") OFF
		}

		public enum BarColor {
			@SerializedName("green") GREEN,
			/** Red at low health, through yellow, to green at full. */
			@SerializedName("gradient") GRADIENT,
			/** Red for hostile mobs, yellow for neutral ones, green for passive ones. */
			@SerializedName("hostility") HOSTILITY
		}

		public boolean showBars = true;
		public BarMode barMode = BarMode.ALL;
		/** Blocks. */
		public double barDistance = 24;
		public boolean seeThrough = false;
		public double barScale = 1.0;
		public BarColor barColor = BarColor.GREEN;
		public boolean showForPlayers = true;

		public boolean showPanel = true;
		public HudAnchor panelAnchor = HudAnchor.TOP_LEFT;
		public int panelX = 4;
		public int panelY = 4;
		public double panelLingerSeconds = 2;
		/** Blocks. */
		public double targetRange = 32;
		public HealthText healthText = HealthText.FRACTION;

		/** Entity type ids that never get a bar or the panel. */
		public List<String> blacklist = new ArrayList<>(List.of("minecraft:armor_stand"));

		/** Resolved on first use: other mods register their entity types after this config loads. */
		private transient Set<EntityType<?>> blacklistedTypes;

		public boolean isBlacklisted(EntityType<?> type) {
			if (blacklistedTypes == null) {
				blacklistedTypes = resolveBlacklist();
			}
			return blacklistedTypes.contains(type);
		}

		private Set<EntityType<?>> resolveBlacklist() {
			Set<EntityType<?>> types = new HashSet<>();
			for (String entry : blacklist) {
				Identifier id = Identifier.tryParse(entry);
				if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
					types.add(BuiltInRegistries.ENTITY_TYPE.getValue(id));
				} else {
					IndicatorBars.LOGGER.warn("Unknown entity type '{}' in the health blacklist", entry);
				}
			}
			return types;
		}

		/** Gson leaves unknown enum names and missing lists as null: put the defaults back. */
		private void fillMissing() {
			HealthConfig defaults = new HealthConfig();
			if (barMode == null) {
				barMode = defaults.barMode;
			}
			if (barColor == null) {
				barColor = defaults.barColor;
			}
			if (panelAnchor == null) {
				panelAnchor = defaults.panelAnchor;
			}
			if (healthText == null) {
				healthText = defaults.healthText;
			}
			if (blacklist == null) {
				blacklist = defaults.blacklist;
			}
		}
	}

	public static class DurabilityConfig {
		public enum Show {
			@SerializedName("always") ALWAYS,
			/** Only items below full durability. */
			@SerializedName("damaged") DAMAGED,
			@SerializedName("off") OFF
		}

		public enum HudText {
			/** {@code 231} */
			@SerializedName("uses") USES,
			/** {@code 231/250} */
			@SerializedName("fraction") FRACTION,
			/** {@code 92%} */
			@SerializedName("percent") PERCENT
		}

		/** Hides the slot counter and the equipment column at once; the durability key flips it. */
		public boolean showNumbers = true;
		/** The number on each damageable item's slot, in the hotbar and inventories. */
		public Show slotCounter = Show.DAMAGED;
		/** The column of worn and held items. */
		public Show equipmentHud = Show.DAMAGED;
		public HudAnchor hudAnchor = HudAnchor.HOTBAR_RIGHT;
		public int hudX = 0;
		public int hudY = 0;
		public HudText hudText = HudText.USES;
		/** An item at or below this percentage of its durability turns red, and the warning plays once. */
		public int warnPercent = 10;
		public boolean warnSound = true;

		private void fillMissing() {
			DurabilityConfig defaults = new DurabilityConfig();
			if (slotCounter == null) {
				slotCounter = defaults.slotCounter;
			}
			if (equipmentHud == null) {
				equipmentHud = defaults.equipmentHud;
			}
			if (hudAnchor == null) {
				hudAnchor = defaults.hudAnchor;
			}
			if (hudText == null) {
				hudText = defaults.hudText;
			}
		}
	}

	public static class CropsConfig {
		public enum BarMode {
			@SerializedName("all") ALL,
			/** Only crops still growing: ripe ones lose their bar. */
			@SerializedName("growing") GROWING,
			/** Only the crop in the crop panel. */
			@SerializedName("target") TARGET
		}

		public enum StageText {
			/** {@code 4/7} */
			@SerializedName("fraction") FRACTION,
			/** {@code 57%} */
			@SerializedName("percent") PERCENT,
			@SerializedName("off") OFF
		}

		public boolean showBars = true;
		public BarMode barMode = BarMode.ALL;
		/** Blocks, also how far around you crops are looked for. */
		public int barDistance = 12;
		public double barScale = 1.0;
		/** The nearest this many crops get a bar, so a big farm stays cheap. */
		public int maxBars = 256;
		/**
		 * Same crop on two sides halves the growth speed. Off by default: most farms are planted that
		 * way, and every bar would warn. The panel still says so.
		 */
		public boolean warnCrowded = false;

		public boolean showPanel = true;
		public HudAnchor panelAnchor = HudAnchor.TOP_LEFT;
		public int panelX = 4;
		public int panelY = 4;
		/** Blocks. */
		public double targetRange = 16;
		public StageText stageText = StageText.FRACTION;

		private void fillMissing() {
			CropsConfig defaults = new CropsConfig();
			if (barMode == null) {
				barMode = defaults.barMode;
			}
			if (panelAnchor == null) {
				panelAnchor = defaults.panelAnchor;
			}
			if (stageText == null) {
				stageText = defaults.stageText;
			}
		}
	}

	public static class HotbarsConfig {
		/** The hotbar plus the three inventory rows. */
		public static final int MAX_COUNT = 4;

		/** 1 is just the vanilla hotbar. Each one past that takes an inventory row. */
		public int count = 1;
		public boolean showIndicator = true;
		public HudAnchor indicatorAnchor = HudAnchor.HOTBAR_LEFT;
		public int indicatorX = 0;
		public int indicatorY = 0;
		/** Which bar is in the hotbar, counted from 0, per world or server. Written by the keys. */
		public Map<String, Integer> active = new HashMap<>();

		public int active(String world) {
			Integer bar = active.get(world);
			return bar == null || bar < 0 || bar >= MAX_COUNT ? 0 : bar;
		}

		public void setActive(String world, int bar) {
			if (bar == 0) {
				active.remove(world);
			} else {
				active.put(world, bar);
			}
		}

		private void fillMissing() {
			HotbarsConfig defaults = new HotbarsConfig();
			count = Mth.clamp(count, 1, MAX_COUNT);
			if (indicatorAnchor == null) {
				indicatorAnchor = defaults.indicatorAnchor;
			}
			if (active == null) {
				active = defaults.active;
			}
		}
	}

	public static class SortConfig {
		public enum Metric {
			@SerializedName("name") NAME,
			/** Weapons, tools, armor, blocks, food, the rest. */
			@SerializedName("type") TYPE,
			/** Largest stacks first. */
			@SerializedName("quantity") QUANTITY
		}

		public enum Preset {
			@SerializedName("none") NONE,
			/** Weapons, then tools, into the bottom rows. */
			@SerializedName("combat") COMBAT,
			/** Pickaxe, axe, shovel, then blocks, into the bottom rows. */
			@SerializedName("builder") BUILDER
		}

		public static final int MAX_MOVES_PER_TICK = 64;

		public boolean showButton = true;
		public Metric metric = Metric.TYPE;
		public Preset preset = Preset.NONE;
		/** A slower sort is gentler on a strict server. */
		public int movesPerTick = 8;

		private void fillMissing() {
			SortConfig defaults = new SortConfig();
			if (metric == null) {
				metric = defaults.metric;
			}
			if (preset == null) {
				preset = defaults.preset;
			}
			movesPerTick = Mth.clamp(movesPerTick, 1, MAX_MOVES_PER_TICK);
		}
	}

	public static class CraftConfig {
		public boolean showButton = true;
	}

	public static IBConfig load(Path path) {
		IBConfig config = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(reader, IBConfig.class);
			} catch (IOException | JsonParseException e) {
				IndicatorBars.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}
		if (config == null) {
			config = new IBConfig();
		}
		config.path = path;
		config.fillMissing();
		config.save();
		return config;
	}

	public void save() {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			IndicatorBars.LOGGER.error("Could not write {}", path, e);
		}
	}

	private void fillMissing() {
		if (health == null) {
			health = new HealthConfig();
		}
		health.fillMissing();
		if (durability == null) {
			durability = new DurabilityConfig();
		}
		durability.fillMissing();
		if (crops == null) {
			crops = new CropsConfig();
		}
		crops.fillMissing();
		if (hotbars == null) {
			hotbars = new HotbarsConfig();
		}
		hotbars.fillMissing();
		if (sort == null) {
			sort = new SortConfig();
		}
		sort.fillMissing();
		if (craft == null) {
			craft = new CraftConfig();
		}
	}
}
