package net.firstwemine1.indicatorbars.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.firstwemine1.indicatorbars.meter.HudAnchor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * {@code config/indicatorbars.json}, one section per meter. Created with defaults on first launch; edit it
 * and restart. The toggle key flips {@code health.showBars} in game and saves it.
 */
public class IBConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public HealthConfig health = new HealthConfig();
	public DurabilityConfig durability = new DurabilityConfig();

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
				ResourceLocation id = ResourceLocation.tryParse(entry);
				if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
					types.add(BuiltInRegistries.ENTITY_TYPE.get(id));
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
	}
}
