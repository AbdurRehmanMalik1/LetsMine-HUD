package net.firstwemine1.indicatorbars.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import net.firstwemine1.indicatorbars.ClientHooks;
import net.firstwemine1.indicatorbars.IndicatorBars;
import net.firstwemine1.indicatorbars.config.IBConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.CraftConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.HotbarsConfig;
import net.firstwemine1.indicatorbars.config.IBConfig.SortConfig;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.render.QuickCraftButton;
import net.firstwemine1.indicatorbars.render.QuickCraftPanel;
import net.firstwemine1.indicatorbars.render.SortButton;
import net.firstwemine1.indicatorbars.render.SortPanel;
import net.firstwemine1.indicatorbars.sort.InventorySorter;
import net.firstwemine1.indicatorbars.sort.ItemKind;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Problem;
import net.firstwemine1.indicatorbars.meter.crops.CropTracker;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Test scenes in a flat world, screenshotted to build/run/clientGameTest/screenshots. An everyday run
 * asserts what's new, the hotbars and sorting; {@code -Pfull} adds the 1.0.0 scenes, where the mob and
 * durability ones are for looking at and the crop meter is asserted. Any failure fails the run.
 */
public class IndicatorBarsClientGameTest implements FabricClientGameTest {
	/** One mob at a time straight ahead: the portrait must fit each in its box. */
	private static final String[][] PORTRAITS = {
			{"pig", "~ ~ ~4", ""},
			{"pig-baby", "~ ~ ~4", "Age:-24000"},
			{"cow", "~ ~ ~4", ""},
			{"chicken", "~ ~ ~3", ""},
			{"horse", "~ ~ ~5", ""},
			{"sheep", "~ ~ ~4", ""},
			{"spider", "~ ~ ~4", ""},
			{"wolf", "~ ~ ~4", ""},
			{"ghast", "~ ~ ~12", ""},
			{"slime", "~ ~ ~5", "Size:3"},
	};

	/** How many checks ran, for the log. */
	private static int checks;
	private static int sortChecks;
	private static int craftChecks;

	@Override
	public void runTest(ClientGameTestContext context) {
		if ("panels".equals(System.getProperty("indicatorbars.testOnly"))) {
			panelsOnly(context);
			return;
		}
		if ("craft".equals(System.getProperty("indicatorbars.testOnly"))) {
			craftOnly(context);
			return;
		}
		List<String> failures = new ArrayList<>();
		List<String> hotbarFailures = new ArrayList<>();
		List<String> sortFailures = new ArrayList<>();
		List<String> craftFailures = new ArrayList<>();
		boolean full = Boolean.getBoolean("indicatorbars.fullTest");
		if (full) {
			cropConfig(failures);
		}
		int cropChecks;
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			// Not peaceful, which would remove the ghast and the slime.
			server.runCommand("difficulty easy");
			// Face south, looking slightly down at the targeted mob's body.
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");
			context.waitTicks(5);

			if (full) {
				oneZeroScenes(context, server, failures);
				// Those mobs leave drops behind, which would end up in the inventories being checked.
				server.runCommand("kill @e[type=!player]");
				context.waitTicks(25);
				server.runCommand("kill @e[type=item]");
			}
			cropChecks = checks;
			sortScene(context, server, sortFailures);
			craftScene(context, server, craftFailures);
			hotbarConfig(hotbarFailures);
			hotbars(context, server, hotbarFailures);
			save = world.getWorldSave();
		}
		hotbarsAfterRejoin(context, save, hotbarFailures);
		hotbarsOnDedicatedServer(context, hotbarFailures, sortFailures, craftFailures);
		if (full) {
			IndicatorBars.LOGGER.info("[crop-test] {} checks, {} failed", cropChecks, failures.size());
		}
		IndicatorBars.LOGGER.info("[hotbar-test] {} checks, {} failed", checks - cropChecks - sortChecks - craftChecks, hotbarFailures.size());
		IndicatorBars.LOGGER.info("[sort-test] {} checks, {} failed", sortChecks, sortFailures.size());
		failures.addAll(hotbarFailures);
		IndicatorBars.LOGGER.info("[craft-test] {} checks, {} failed", craftChecks, craftFailures.size());
		failures.addAll(sortFailures);
		failures.addAll(craftFailures);
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed checks:\n  " + String.join("\n  ", failures));
		}
	}

	/** Everything from 1.0.0. */
	private static void oneZeroScenes(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		{
			cows(context, server);
			for (String[] portrait : PORTRAITS) {
				server.runCommand("kill @e[type=!player]");
				// Let the last mob's death animation finish, or it stays in the panel.
				context.waitTicks(25);
				String type = portrait[0].replace("-baby", "");
				summon(server, type, portrait[1], portrait[2]);
				context.waitTicks(5);
				context.runOnClient(IndicatorBarsClientGameTest::lookAtMob);
				context.waitTicks(10);
				context.takeScreenshot("portrait-" + portrait[0]);
			}
			durability(context, server);
			crops(context, server, failures);
			cropPixels(context, server, failures);
			cropLogic(context, server, failures);
		}
	}

	/** One crop case: where it goes (from the base, on the ground), what to build, what the meter must say. */
	private record CropCase(String name, int dx, int dz, String[] build, int age, int maxAge, Set<Problem> problems) {
	}

	private static CropCase crop(String name, int dx, int dz, int age, int maxAge, Set<Problem> problems, String... build) {
		return new CropCase(name, dx, dz, build, age, maxAge, problems);
	}

	/**
	 * Asserts what {@link CropGrowth#read} makes of each case against vanilla's growth rules, then which
	 * crops the tracker gives a bar in each mode. Built north of the player, out of the screenshots'
	 * way. "@" in a command is the case's own block, "@x,y,z" one offset from it.
	 */
	private static void cropLogic(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		Set<Problem> none = EnumSet.noneOf(Problem.class);
		CropCase[] cases = {
				crop("wheat-moist", -6, -4, 3, 7, none, "@0,-1,0 farmland[moisture=7]", "@ wheat[age=3]"),
				crop("wheat-dry", -4, -4, 3, 7, EnumSet.of(Problem.DRY), "@0,-1,0 farmland[moisture=0]", "@ wheat[age=3]"),
				crop("wheat-ripe-dry", -2, -4, 7, 7, none, "@0,-1,0 farmland[moisture=0]", "@ wheat[age=7]"),
				// A row is crowded on one axis only: full speed.
				crop("wheat-row", 3, -4, 1, 7, none,
						"@-1,-1,0 farmland[moisture=7]", "@0,-1,0 farmland[moisture=7]", "@1,-1,0 farmland[moisture=7]",
						"@-1,0,0 wheat[age=1]", "@1,0,0 wheat[age=1]", "@ wheat[age=1]"),
				crop("wheat-both-axes", 3, -8, 1, 7, EnumSet.of(Problem.CROWDED),
						"@-1,-1,-1 farmland[moisture=7]", "@1,-1,1 farmland[moisture=7]",
						"@-1,-1,1 farmland[moisture=7]", "@1,-1,-1 farmland[moisture=7]",
						"@-1,-1,0 farmland[moisture=7]", "@1,-1,0 farmland[moisture=7]", "@0,-1,-1 farmland[moisture=7]",
						"@0,-1,1 farmland[moisture=7]", "@0,-1,0 farmland[moisture=7]",
						"@-1,0,0 wheat[age=1]", "@0,0,1 wheat[age=1]", "@ wheat[age=1]"),
				crop("wheat-diagonal", -5, -8, 1, 7, EnumSet.of(Problem.CROWDED),
						"@0,-1,0 farmland[moisture=7]", "@1,-1,-1 farmland[moisture=7]", "@1,0,-1 wheat[age=1]", "@ wheat[age=1]"),
				// Other crops all around don't count.
				crop("carrots-among-potatoes", -6, -12, 2, 7, none,
						"@-1,-1,-1 @1,-1,1 farmland[moisture=7]",
						"@-1,0,0 potatoes", "@1,0,0 potatoes", "@0,0,-1 potatoes", "@0,0,1 potatoes", "@ carrots[age=2]"),
				// Grass on every side: room for the pumpkin.
				crop("stem-room", 6, -12, 7, 7, none, "@0,-1,0 farmland[moisture=7]", "@ pumpkin_stem[age=7]"),
				crop("stem-no-room", 2, -16, 7, 7, EnumSet.of(Problem.NO_FRUIT_ROOM),
						"@-1,-1,0 stone", "@1,-1,0 stone", "@0,-1,-1 stone", "@0,-1,1 stone",
						"@0,-1,0 farmland[moisture=7]", "@ melon_stem[age=7]"),
				// Still growing: its fruit's room doesn't matter yet.
				crop("stem-young-boxed", 6, -16, 3, 7, none,
						"@-1,0,0 white_carpet", "@1,0,0 white_carpet", "@0,0,-1 white_carpet", "@0,0,1 white_carpet",
						"@0,-1,0 farmland[moisture=7]", "@ pumpkin_stem[age=3]"),
				crop("pitcher-open", -2, -16, 2, 4, none, "@0,-1,0 farmland[moisture=7]", "@ pitcher_crop[age=2,half=lower]"),
				crop("pitcher-blocked", -5, -16, 2, 4, EnumSet.of(Problem.BLOCKED_ABOVE),
						"@0,-1,0 farmland[moisture=7]", "@0,1,0 oak_planks", "@ pitcher_crop[age=2,half=lower]"),
				crop("pitcher-grown", -8, -16, 4, 4, none, "@0,-1,0 farmland[moisture=7]",
						"@ pitcher_crop[age=4,half=lower]", "@0,1,0 pitcher_crop[age=4,half=upper]"),
				// Any moisture at all counts as moist.
				crop("wheat-barely-moist", -8, -8, 3, 7, none, "@0,-1,0 farmland[moisture=1]", "@ wheat[age=3]"),
				crop("beetroots-ripe", -2, -20, 3, 3, none, "@0,-1,0 farmland[moisture=7]", "@ beetroots[age=3]"),
				crop("potatoes", -4, -20, 7, 7, none, "@0,-1,0 farmland[moisture=7]", "@ potatoes[age=7]"),
				// Its last growth turns it into a torchflower, so it never reads as ripe.
				crop("torchflower", -6, -20, 1, 2, none, "@0,-1,0 farmland[moisture=7]", "@ torchflower_crop[age=1]"),
				crop("berries-young", 0, -20, 1, 3, none, "@ sweet_berry_bush[age=1]"),
				crop("berries-ripe", 2, -20, 3, 3, none, "@ sweet_berry_bush[age=3]"),
				crop("nether-wart", 4, -20, 3, 3, none, "@0,-1,0 soul_sand", "@ nether_wart[age=3]"),
				crop("cocoa", 6, -20, 2, 2, none, "@0,0,1 jungle_log", "@ cocoa[age=2,facing=south]"),
				// Shut in stone, lit only by a light block beside it, one level down at the crop.
				crop("wheat-light-8", -14, -26, 3, 7, EnumSet.of(Problem.TOO_DARK), dark("farmland[moisture=7]", "wheat[age=3]", 9)),
				crop("wheat-light-9", -9, -26, 3, 7, none, dark("farmland[moisture=7]", "wheat[age=3]", 10)),
				crop("wheat-ripe-dark", -4, -26, 7, 7, none, dark("farmland[moisture=7]", "wheat[age=7]", 0)),
				// The pitcher grows at 8.
				crop("pitcher-light-8", 1, -26, 1, 4, none, dark("farmland[moisture=7]", "pitcher_crop[age=1,half=lower]", 9)),
				crop("pitcher-light-7", 6, -26, 1, 4, EnumSet.of(Problem.TOO_DARK),
						dark("farmland[moisture=7]", "pitcher_crop[age=1,half=lower]", 8)),
				// A grown stem keeps its problems: its fruit still needs light, and stone all round.
				crop("stem-dark", 11, -26, 7, 7, EnumSet.of(Problem.TOO_DARK, Problem.NO_FRUIT_ROOM),
						dark("farmland[moisture=7]", "pumpkin_stem[age=7]", 0)),
				// The bush reads the light above it, the stone roof here.
				crop("berries-dark", -14, -31, 1, 3, EnumSet.of(Problem.TOO_DARK), dark("dirt", "sweet_berry_bush[age=1]", 15)),
		};

		BlockPos base = context.computeOnClient(minecraft -> minecraft.player.blockPosition());
		server.runCommand(String.format("fill %d %d %d %d %d %d air", base.getX() - 16, base.getY(), base.getZ() - 33,
				base.getX() + 14, base.getY() + 3, base.getZ() - 2));
		server.runCommand(String.format("fill %d %d %d %d %d %d grass_block", base.getX() - 16, base.getY() - 1, base.getZ() - 33,
				base.getX() + 14, base.getY() - 1, base.getZ() - 2));
		for (CropCase c : cases) {
			BlockPos at = base.offset(c.dx, 0, c.dz);
			for (String command : c.build) {
				server.runCommand(place(at, command));
			}
		}
		// The top of the scan and just past it, off the ground.
		server.runCommand(place(base, "@-8,5,-4 farmland[moisture=7]"));
		server.runCommand(place(base, "@-8,6,-4 wheat[age=2]"));
		server.runCommand(place(base, "@-10,6,-4 farmland[moisture=7]"));
		server.runCommand(place(base, "@-10,7,-4 wheat[age=2]"));
		// Long enough for the light to reach the client.
		context.waitTicks(40);

		context.runOnClient(minecraft -> {
			Level level = minecraft.level;
			for (CropCase c : cases) {
				BlockPos at = base.offset(c.dx, 0, c.dz);
				Crop crop = CropGrowth.read(level, at, level.getBlockState(at));
				expect(failures, c.name + ": not read as a crop (" + level.getBlockState(at) + ")", crop != null);
				if (crop == null) {
					continue;
				}
				String got = crop.age() + "/" + crop.maxAge() + " " + crop.problems() + " light " + crop.light();
				String want = c.age + "/" + c.maxAge + " " + c.problems;
				expect(failures, c.name + ": want " + want + ", got " + got,
						crop.age() == c.age && crop.maxAge() == c.maxAge && crop.problems().equals(c.problems));
				IndicatorBars.LOGGER.info("[crop-logic] {}: {}", c.name, got);
			}
			// The pitcher's top half is no crop of its own; its bottom reaches into it.
			BlockPos pitcher = base.offset(-8, 0, -16);
			expect(failures, "pitcher-grown: its top half read as a crop",
					CropGrowth.read(level, pitcher.above(), level.getBlockState(pitcher.above())) == null);
			Crop grown = CropGrowth.read(level, pitcher, level.getBlockState(pitcher));
			expect(failures, "pitcher-grown: top should reach into the upper block", grown != null && grown.top() > 1);
			// Not crops at all.
			for (BlockPos pos : List.of(base.offset(-6, -1, -4), base.offset(0, -1, -20), base.offset(-12, 0, -4))) {
				expect(failures, level.getBlockState(pos) + " read as a crop", CropGrowth.read(level, pos, level.getBlockState(pos)) == null);
			}
		});

		cropValues(context, base, failures);
		trackerModes(context, server, base, failures);
		crowdingAgainstVanilla(context, server, base, failures);
		growthAgainstVanilla(context, server, base, cases, failures);
	}

	/** What the bar and panel are drawn from: warning, colour and fill. */
	private static void cropValues(ClientGameTestContext context, BlockPos base, List<String> failures) {
		context.runOnClient(minecraft -> {
			Level level = minecraft.level;
			CropsConfig config = new CropsConfig();
			CropsConfig warnCrowded = new CropsConfig();
			warnCrowded.warnCrowded = true;
			Crop dry = CropGrowth.read(level, base.offset(-4, 0, -4), level.getBlockState(base.offset(-4, 0, -4)));
			Crop crowded = CropGrowth.read(level, base.offset(3, 0, -8), level.getBlockState(base.offset(3, 0, -8)));
			Crop moist = CropGrowth.read(level, base.offset(-6, 0, -4), level.getBlockState(base.offset(-6, 0, -4)));
			Crop ripe = CropGrowth.read(level, base.offset(-2, 0, -4), level.getBlockState(base.offset(-2, 0, -4)));
			Crop stem = CropGrowth.read(level, base.offset(6, 0, -12), level.getBlockState(base.offset(6, 0, -12)));
			expect(failures, "dry warns", dry.warns(config));
			expect(failures, "crowded doesn't warn by default", !crowded.warns(config));
			expect(failures, "crowded warns with warnCrowded", crowded.warns(warnCrowded));
			expect(failures, "healthy doesn't warn", !moist.warns(warnCrowded));
			expect(failures, "growing is yellow", moist.color() == MeterStyle.YELLOW);
			expect(failures, "ripe is green", ripe.color() == MeterStyle.GREEN);
			expect(failures, "grown stem is green", stem.color() == MeterStyle.GREEN);
			expect(failures, "3/7 fills 3/7 of the bar", Math.abs(moist.fraction() - 3f / 7) < 1e-4f);
			expect(failures, "ripe fills the bar", ripe.fraction() == 1);
		});
	}

	/**
	 * The meter's CROWDED against {@code CropBlock.getGrowthSpeed} itself: identical moist soil, wheat in
	 * the middle, and only the neighbours differ. Vanilla must halve the speed exactly when the meter says so.
	 */
	private static void crowdingAgainstVanilla(ClientGameTestContext context, TestServerContext server, BlockPos base, List<String> failures) {
		// Name, neighbour block, neighbours as dx,dz. "alone" comes first: the others are compared to it.
		String[][] layouts = {
				{"alone", "wheat"},
				{"one-side", "wheat", "-1,0"},
				{"row", "wheat", "-1,0", "1,0"},
				{"column", "wheat", "0,-1", "0,1"},
				{"corner", "wheat", "-1,0", "0,-1"},
				{"diagonal", "wheat", "1,1"},
				{"row-and-diagonal", "wheat", "-1,0", "1,0", "1,-1"},
				{"other-crop-all-round", "potatoes", "-1,0", "1,0", "0,-1", "0,1", "1,1", "-1,-1"},
		};
		server.runCommand(String.format("fill %d %d %d %d %d %d air", base.getX() - 18, base.getY(), base.getZ() - 42,
				base.getX() + 18, base.getY() + 1, base.getZ() - 38));
		BlockPos[] middles = new BlockPos[layouts.length];
		for (int i = 0; i < layouts.length; i++) {
			BlockPos at = middles[i] = base.offset(-16 + i * 4, 0, -40);
			server.runCommand(place(at, "@-1,-1,-1 @1,-1,1 farmland[moisture=7]"));
			server.runCommand(place(at, "@ wheat[age=1]"));
			for (int n = 2; n < layouts[i].length; n++) {
				String[] xz = layouts[i][n].split(",");
				server.runCommand(place(at, "@" + xz[0] + ",0," + xz[1] + " " + layouts[i][1] + "[age=1]"));
			}
		}
		context.waitTicks(10);
		context.runOnClient(minecraft -> {
			Level level = minecraft.level;
			float alone = vanillaSpeed(level, middles[0]);
			int crowdedLayouts = 0;
			for (int i = 0; i < layouts.length; i++) {
				float speed = vanillaSpeed(level, middles[i]);
				boolean halved = Math.abs(speed - alone / 2) < 1e-3f;
				expect(failures, layouts[i][0] + ": vanilla speed " + speed + " is neither full (" + alone + ") nor half",
						halved || Math.abs(speed - alone) < 1e-3f);
				Crop crop = CropGrowth.read(level, middles[i], level.getBlockState(middles[i]));
				boolean crowded = crop.problems().contains(Problem.CROWDED);
				expect(failures, layouts[i][0] + ": meter says crowded=" + crowded + ", vanilla speed " + speed + " of " + alone, crowded == halved);
				crowdedLayouts += halved ? 1 : 0;
			}
			expect(failures, "crowding layouts: 3 should be crowded, got " + crowdedLayouts, crowdedLayouts == 3);
			// Moisture: the block under the crop is what the DRY warning is about.
			float moist = vanillaSpeed(level, base.offset(-6, 0, -4));
			float barelyMoist = vanillaSpeed(level, base.offset(-8, 0, -8));
			float dry = vanillaSpeed(level, base.offset(-4, 0, -4));
			expect(failures, "vanilla: moisture 1 grows as fast as 7, " + barelyMoist + " vs " + moist, barelyMoist == moist);
			expect(failures, "vanilla: dry grows slower, " + dry + " vs " + moist, dry < moist);
		});
	}

	private static float vanillaSpeed(Level level, BlockPos pos) {
		try {
			Method speed = CropBlock.class.getDeclaredMethod("getGrowthSpeed", Block.class, BlockGetter.class, BlockPos.class);
			speed.setAccessible(true);
			return (float) speed.invoke(null, level.getBlockState(pos).getBlock(), level, pos);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Could not call vanilla's getGrowthSpeed", e);
		}
	}

	/**
	 * The meter's promises against vanilla's own growth: every case gets thousands of real random ticks
	 * on the server. A crop the meter calls stopped must not have moved, every other one must have grown.
	 */
	private static void growthAgainstVanilla(ClientGameTestContext context, TestServerContext server, BlockPos base, CropCase[] cases, List<String> failures) {
		Map<String, Crop> before = new HashMap<>();
		context.runOnClient(minecraft -> {
			for (CropCase c : cases) {
				BlockPos at = base.offset(c.dx, 0, c.dz);
				before.put(c.name, CropGrowth.read(minecraft.level, at, minecraft.level.getBlockState(at)));
			}
		});
		server.runOnServer(minecraftServer -> {
			ServerLevel level = minecraftServer.overworld();
			RandomSource random = RandomSource.create(42);
			for (CropCase c : cases) {
				BlockPos at = base.offset(c.dx, 0, c.dz);
				for (int tick = 0; tick < 5000; tick++) {
					level.getBlockState(at).randomTick(level, at, random);
				}
			}
		});
		context.waitTicks(20);
		context.runOnClient(minecraft -> {
			Level level = minecraft.level;
			for (CropCase c : cases) {
				BlockPos at = base.offset(c.dx, 0, c.dz);
				Crop was = before.get(c.name);
				if (was == null) {
					continue;
				}
				boolean stopped = was.problems().contains(Problem.TOO_DARK) || was.problems().contains(Problem.BLOCKED_ABOVE)
						|| was.problems().contains(Problem.NO_FRUIT_ROOM);
				if (was.stem() && was.ripe()) {
					boolean fruited = !(level.getBlockState(at).getBlock() instanceof StemBlock);
					expect(failures, c.name + ": meter says " + was.problems() + ", vanilla fruited=" + fruited, fruited != stopped);
					continue;
				}
				Crop now = CropGrowth.read(level, at, level.getBlockState(at));
				// No longer a crop: the torchflower's last growth.
				int age = now == null ? Integer.MAX_VALUE : now.age();
				if (stopped || was.ripe()) {
					expect(failures, c.name + ": meter says stopped " + was.problems() + ", vanilla grew it to " + age, age == was.age());
				} else {
					expect(failures, c.name + ": meter says growing " + was.problems() + ", vanilla left it at " + age, age > was.age());
				}
			}
			// Once grown it is boxed in by its carpets, and the meter must notice.
			BlockPos boxed = base.offset(6, 0, -16);
			Crop stem = CropGrowth.read(level, boxed, level.getBlockState(boxed));
			expect(failures, "stem-young-boxed: grown, now no room for fruit",
					stem != null && stem.ripe() && stem.problems().contains(Problem.NO_FRUIT_ROOM));
			BlockPos pitcher = base.offset(-2, 0, -16);
			Crop open = CropGrowth.read(level, pitcher, level.getBlockState(pitcher));
			expect(failures, "pitcher-open: fully grown, with its top half", open != null && open.ripe() && open.top() > 1);
		});
	}

	/** Which crops each bar mode gives a bar. */
	private static void trackerModes(ClientGameTestContext context, TestServerContext server, BlockPos base, List<String> failures) {
		BlockPos ripeWheat = base.offset(-2, 0, -4);
		BlockPos dryWheat = base.offset(-4, 0, -4);
		BlockPos ripeStem = base.offset(6, 0, -12);
		context.runOnClient(minecraft -> {
			CropsConfig config = new CropsConfig();
			config.barDistance = 40;
			config.maxBars = 10_000;
			CropTracker tracker = new CropTracker();

			config.barMode = CropsConfig.BarMode.ALL;
			tracker.tick(minecraft, config);
			Set<BlockPos> all = positions(tracker.crops());
			expect(failures, "all: ripe wheat", all.contains(ripeWheat));
			expect(failures, "all: growing wheat", all.contains(dryWheat));

			config.barMode = CropsConfig.BarMode.GROWING;
			tracker.reset();
			tracker.tick(minecraft, config);
			Set<BlockPos> growing = positions(tracker.crops());
			expect(failures, "growing: no ripe wheat", !growing.contains(ripeWheat));
			expect(failures, "growing: growing wheat", growing.contains(dryWheat));
			expect(failures, "growing: grown stem, its fruit still to come", growing.contains(ripeStem));

			config.maxBars = 3;
			tracker.reset();
			tracker.tick(minecraft, config);
			expect(failures, "maxBars 3: got " + tracker.crops().size(), tracker.crops().size() == 3);
			Set<BlockPos> nearest = positions(tracker.crops());
			double furthestKept = 0;
			double nearestDropped = Double.MAX_VALUE;
			for (BlockPos pos : growing) {
				double distance = pos.distToCenterSqr(minecraft.player.position());
				if (nearest.contains(pos)) {
					furthestKept = Math.max(furthestKept, distance);
				} else {
					nearestDropped = Math.min(nearestDropped, distance);
				}
			}
			expect(failures, "maxBars 3: not the nearest", furthestKept <= nearestDropped);

			config.showBars = false;
			tracker.reset();
			tracker.tick(minecraft, config);
			expect(failures, "bars off: none", tracker.crops().isEmpty());

			// How far the scan reaches, at the default distance.
			tracker.reset();
			tracker.tick(minecraft, new CropsConfig());
			Set<BlockPos> near = positions(tracker.crops());
			expect(failures, "12 blocks: wheat at 4 has a bar", near.contains(dryWheat));
			expect(failures, "12 blocks: 6 above has a bar", near.contains(base.offset(-8, 6, -4)));
			expect(failures, "12 blocks: 7 above has none", !near.contains(base.offset(-10, 7, -4)));
			expect(failures, "12 blocks: stem at 13.4 has none", !near.contains(ripeStem));
			BlockPos player = minecraft.player.blockPosition();
			for (BlockPos pos : near) {
				int dx = pos.getX() - player.getX();
				int dz = pos.getZ() - player.getZ();
				expect(failures, pos + " is outside the 12 block circle", dx * dx + dz * dz <= 144);
			}
		});

		// The scan follows the world with no reset, within its few ticks.
		CropTracker live = new CropTracker();
		context.runOnClient(minecraft -> {
			live.tick(minecraft, new CropsConfig());
			expect(failures, "scan: age before, got " + ageAt(live, dryWheat), ageAt(live, dryWheat) == 3);
		});
		server.runCommand(place(dryWheat, "@ wheat[age=5]"));
		context.waitTicks(5);
		context.runOnClient(minecraft -> {
			for (int tick = 0; tick < 6; tick++) {
				live.tick(minecraft, new CropsConfig());
			}
			expect(failures, "scan: picks up the new age, got " + ageAt(live, dryWheat), ageAt(live, dryWheat) == 5);
		});
		server.runCommand(place(dryWheat, "@ wheat[age=3]"));
		context.waitTicks(5);

		// Target mode: only the crop under the crosshair.
		context.runOnClient(minecraft -> lookAtBlock(minecraft, -4, -4, 0.4));
		context.waitTicks(2);
		context.runOnClient(minecraft -> {
			CropsConfig config = new CropsConfig();
			config.barMode = CropsConfig.BarMode.TARGET;
			CropTracker tracker = new CropTracker();
			tracker.tick(minecraft, config);
			Crop target = tracker.target();
			expect(failures, "target: dry wheat under the crosshair, got " + (target == null ? null : target.pos()),
					target != null && target.pos().equals(dryWheat));
			expect(failures, "target: only it gets a bar", tracker.crops().size() == 1);
			config.targetRange = 2;
			tracker.tick(minecraft, config);
			expect(failures, "target: out of range", tracker.target() == null);
			expect(failures, "target: out of range, no bar", tracker.crops().isEmpty());
		});

		// The pitcher's top half stands for its bottom one.
		context.runOnClient(minecraft -> lookAtBlock(minecraft, -8, -16, 1.4));
		context.waitTicks(2);
		context.runOnClient(minecraft -> {
			CropsConfig config = new CropsConfig();
			config.targetRange = 32;
			CropTracker tracker = new CropTracker();
			tracker.tick(minecraft, config);
			Crop target = tracker.target();
			expect(failures, "target: top of the pitcher, got " + (target == null ? null : target.pos()),
					target != null && target.pos().equals(base.offset(-8, 0, -16)));
		});
	}

	private static int ageAt(CropTracker tracker, BlockPos pos) {
		for (Crop crop : tracker.crops()) {
			if (crop.pos().equals(pos)) {
				return crop.age();
			}
		}
		return -1;
	}

	private static Set<BlockPos> positions(List<Crop> crops) {
		Set<BlockPos> positions = new HashSet<>();
		for (Crop crop : crops) {
			positions.add(crop.pos());
		}
		return positions;
	}

	private static void expect(List<String> failures, String what, boolean ok) {
		checks++;
		if (!ok) {
			failures.add(what);
		}
	}

	/** A crop shut in stone with a light block of {@code light} beside it (0: none). */
	private static String[] dark(String soil, String crop, int light) {
		List<String> build = new ArrayList<>(List.of("@-1,-1,-1 @2,1,1 stone hollow", "@0,-1,0 " + soil));
		if (light > 0) {
			build.add("@1,0,0 light[level=" + light + "]");
		}
		build.add("@ " + crop);
		return build.toArray(String[]::new);
	}

	/** "@dx,dy,dz block" to a setblock, "@a @b block" to a fill, "@ block" at the case itself. */
	private static String place(BlockPos at, String command) {
		String[] parts = command.split(" ", 2);
		if (parts[1].startsWith("@")) {
			String[] fill = parts[1].split(" ", 2);
			BlockPos from = offset(at, parts[0]);
			BlockPos to = offset(at, fill[0]);
			return String.format("fill %d %d %d %d %d %d %s", from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), fill[1]);
		}
		BlockPos pos = offset(at, parts[0]);
		return String.format("setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), parts[1]);
	}

	private static BlockPos offset(BlockPos at, String token) {
		if (token.equals("@")) {
			return at;
		}
		String[] xyz = token.substring(1).split(",");
		return at.offset(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
	}

	/**
	 * A small farm, one crop per case: a row of wheat at every age on moist farmland, carrots on dry
	 * farmland (dotted), a grown pumpkin stem boxed in (dotted), a pitcher crop under a plank
	 * (dotted), a crowded beetroot patch (no dots by default), a berry bush, nether wart and cocoa.
	 * Random ticks are off, so nothing grows or dries out mid-test.
	 */
	private static void crops(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		// Twice: the big slime from the portraits splits when killed.
		server.runCommand("kill @e[type=!player]");
		context.waitTicks(25);
		server.runCommand("kill @e[type=!player]");
		server.runCommand("gamerule random_tick_speed 0");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 35");
		String[] blocks = {
				"fill ~-5 ~ ~2 ~5 ~3 ~11 air",
				"fill ~-4 ~-1 ~3 ~3 ~-1 ~3 farmland[moisture=7]",
				"setblock ~4 ~-1 ~3 water",
				"fill ~-2 ~-1 ~5 ~0 ~-1 ~5 farmland[moisture=0]",
				"setblock ~-2 ~ ~5 carrots[age=2]",
				"setblock ~-1 ~ ~5 carrots[age=4]",
				"setblock ~0 ~ ~5 carrots[age=6]",
				"setblock ~2 ~-1 ~5 farmland[moisture=7]",
				"setblock ~2 ~ ~5 pumpkin_stem[age=7]",
				// Carpet is low enough to aim over, and not air, so the fruit has no room.
				"setblock ~1 ~ ~5 white_carpet",
				"setblock ~3 ~ ~5 white_carpet",
				"setblock ~2 ~ ~4 white_carpet",
				"setblock ~2 ~ ~6 white_carpet",
				"setblock ~-4 ~ ~5 sweet_berry_bush[age=1]",
				"setblock ~4 ~-1 ~6 farmland[moisture=7]",
				"setblock ~4 ~ ~6 pitcher_crop[age=2,half=lower]",
				"setblock ~4 ~1 ~6 oak_planks",
				"setblock ~-3 ~-1 ~7 soul_sand",
				"setblock ~-3 ~ ~7 nether_wart[age=2]",
				"setblock ~3 ~ ~8 jungle_log",
				"setblock ~3 ~ ~7 cocoa[age=1,facing=south]",
				"fill ~-1 ~-1 ~8 ~1 ~-1 ~10 farmland[moisture=7]",
				"fill ~-1 ~ ~8 ~1 ~ ~10 beetroots[age=1]",
		};
		for (String block : blocks) {
			server.runCommand("execute at @p run " + block);
		}
		for (int age = 0; age <= 7; age++) {
			server.runCommand("execute at @p run setblock ~" + (age - 4) + " ~ ~3 wheat[age=" + age + "]");
		}
		// Long enough for the durability scene's advancement toasts to leave.
		context.waitTicks(120);
		context.takeScreenshot("crops");

		// Name, block offset from the player, how high to aim (over the crops in front of it), and the
		// panel's colours: Green, Yellow, Red, greY note.
		String[][] panels = {
				{"dry-carrots", "-1", "5", "0.4", "YR"},
				{"no-fruit-room", "2", "5", "0.4", "GR"},
				{"ripe-wheat", "3", "3", "0.95", "G"},
				{"crowded-beetroots", "1", "9", "0.15", "YN"},
				{"blocked-pitcher", "4", "6", "0.4", "YR"},
		};
		for (String[] panel : panels) {
			int dx = Integer.parseInt(panel[1]);
			int dz = Integer.parseInt(panel[2]);
			double height = Double.parseDouble(panel[3]);
			context.runOnClient(minecraft -> lookAtBlock(minecraft, dx, dz, height));
			context.waitTicks(3);
			expectPanel(failures, panel[0], context.takeScreenshot("crop-panel-" + panel[0]), panel[4]);
		}

		// K hides the crop bars.
		context.getInput().pressKey(ClientHooks.TOGGLE_CROPS);
		context.waitTicks(5);
		context.takeScreenshot("crops-hidden");
		context.getInput().pressKey(ClientHooks.TOGGLE_CROPS);
	}

	/**
	 * The drawing itself, read back from screenshots with one crop in the world: the bar's colour and
	 * fill, the dotted line, K, and which panel wins. The HUD draws exact colours; bars in the world are
	 * blended a little, so they are matched within a tolerance. Nothing else in these scenes comes close.
	 */
	private static void cropPixels(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		server.runCommand("clear @p");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 35");
		context.waitTicks(2);
		BlockPos base = context.computeOnClient(minecraft -> minecraft.player.blockPosition());
		server.runCommand(String.format("fill %d %d %d %d %d %d air", base.getX() - 13, base.getY(), base.getZ() - 13,
				base.getX() + 13, base.getY() + 3, base.getZ() + 13));
		server.runCommand(String.format("fill %d %d %d %d %d %d grass_block", base.getX() - 13, base.getY() - 1, base.getZ() - 13,
				base.getX() + 13, base.getY() - 1, base.getZ() + 13));
		BlockPos at = base.offset(2, 0, 4);

		Pixels age0 = barScene(context, server, at, 7, 0, "crop-bar-age0");
		expect(failures, "bar age 0: empty, " + age0, age0.yellow == 0 && age0.green == 0 && age0.red == 0);
		Pixels age3 = barScene(context, server, at, 7, 3, "crop-bar-age3");
		expect(failures, "bar age 3: yellow only, " + age3, age3.yellow > 0 && age3.green == 0 && age3.red == 0);
		Pixels age6 = barScene(context, server, at, 7, 6, "crop-bar-age6");
		expect(failures, "bar age 6: twice the fill of age 3, " + age6 + " vs " + age3,
				age6.yellow > age3.yellow * 3 / 2 && age6.yellow < age3.yellow * 5 / 2);
		// Sweet berries at 2/3 and ripe stand about as tall, so their bars sit in the same place.
		server.runCommand(place(at, "@0,-1,0 grass_block"));
		server.runCommand(place(at, "@ sweet_berry_bush[age=2]"));
		context.waitTicks(15);
		Pixels part = worldPixels(context.takeScreenshot("crop-bar-berries-growing"));
		server.runCommand(place(at, "@ sweet_berry_bush[age=3]"));
		context.waitTicks(15);
		Pixels full = worldPixels(context.takeScreenshot("crop-bar-berries-ripe"));
		expect(failures, "bar: fills from the left, " + part.yellowLeft + ".." + part.yellowRight + " of " + full.greenLeft + ".." + full.greenRight,
				Math.abs(part.yellowLeft - full.greenLeft) <= 3 && part.yellowRight < full.greenRight - (full.greenRight - full.greenLeft) / 5);
		Pixels dry = barScene(context, server, at, 0, 3, "crop-bar-dry");
		expect(failures, "bar dry: dotted red, " + dry, dry.yellow > 0 && dry.red > 0);
		Pixels ripe = barScene(context, server, at, 0, 7, "crop-bar-ripe");
		expect(failures, "bar ripe: green and no warning, even dry, " + ripe, ripe.green > 0 && ripe.yellow == 0 && ripe.red == 0);

		// K hides the bars, but not the panel, and is saved.
		Pixels moist = barScene(context, server, at, 7, 3, "crop-bar-before-k");
		context.getInput().pressKey(ClientHooks.TOGGLE_CROPS);
		context.waitTicks(5);
		Pixels off = worldPixels(context.takeScreenshot("crop-bar-k-off"));
		expect(failures, "K: no bars, " + off, off.yellow == 0 && off.green == 0 && off.red == 0);
		expect(failures, "K: saved as off", !savedShowBars());
		context.runOnClient(minecraft -> lookAtBlock(minecraft, 2, 4, 0.4));
		context.waitTicks(3);
		expectPanel(failures, "bars off", context.takeScreenshot("crop-panel-bars-off"), "YG");
		context.getInput().pressKey(ClientHooks.TOGGLE_CROPS);
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 35");
		context.waitTicks(10);
		Pixels on = worldPixels(context.takeScreenshot("crop-bar-k-on"));
		expect(failures, "K: back as they were, " + on + " vs " + moist, on.yellow == moist.yellow);
		expect(failures, "K: saved as on", savedShowBars());

		// The panel, on each state of one crop.
		expectPanel(failures, "optimal", panelScene(context, server, at, 7, 3, "crop-panel-optimal"), "YG");
		expectPanel(failures, "ripe", panelScene(context, server, at, 7, 7, "crop-panel-ripe"), "G");
		expectPanel(failures, "dry", panelScene(context, server, at, 0, 3, "crop-panel-dry"), "YR");
		server.runCommand(place(at, "@1,-1,-1 farmland[moisture=7]"));
		server.runCommand(place(at, "@1,0,-1 wheat[age=3]"));
		expectPanel(failures, "crowded", panelScene(context, server, at, 7, 3, "crop-panel-crowded"), "YN");
		server.runCommand(place(at, "@1,0,-1 air"));

		// A mob under the crosshair beats the crop behind it; a mob only lingering in the panel doesn't.
		BlockPos other = base.offset(-2, 0, 4);
		server.runCommand(place(other, "@0,-1,0 farmland[moisture=0]"));
		server.runCommand(place(other, "@ wheat[age=3]"));
		// Halfway to the crop, so the crosshair passes through the mob and lands on the crop behind it.
		summon(server, "cow", "~1 ~ ~2", "");
		context.waitTicks(10);
		context.runOnClient(minecraft -> lookAtBlock(minecraft, 2, 4, 0.4));
		context.waitTicks(3);
		Pixels mobPanel = panelPixels(context.takeScreenshot("crop-panel-mob-wins"));
		expect(failures, "mob panel over the crop's, " + mobPanel, mobPanel.yellow == 0 && mobPanel.green > 0);
		context.runOnClient(minecraft -> lookAtBlock(minecraft, -2, 4, 0.4));
		context.waitTicks(3);
		// Not expectPanel: the cow's own green bar in the world can show beside the panel here.
		Pixels lingering = panelPixels(context.takeScreenshot("crop-panel-mob-lingers"));
		expect(failures, "crop panel over a lingering mob, " + lingering, lingering.yellow > 0 && lingering.red > 0);
		server.runCommand("kill @e[type=!player]");
		context.waitTicks(25);
	}

	/** One wheat in view with the crosshair off it. */
	private static Pixels barScene(ClientGameTestContext context, TestServerContext server, BlockPos at, int moisture, int age, String name) {
		placeWheat(context, server, at, moisture, age);
		return worldPixels(context.takeScreenshot(name));
	}

	/** One wheat with the crosshair on it. */
	private static Path panelScene(ClientGameTestContext context, TestServerContext server, BlockPos at, int moisture, int age, String name) {
		placeWheat(context, server, at, moisture, age);
		context.runOnClient(minecraft -> lookAtBlock(minecraft, 2, 4, 0.4));
		context.waitTicks(3);
		Path shot = context.takeScreenshot(name);
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 35");
		context.waitTicks(3);
		return shot;
	}

	private static void placeWheat(ClientGameTestContext context, TestServerContext server, BlockPos at, int moisture, int age) {
		server.runCommand(place(at, "@0,-1,0 farmland[moisture=" + moisture + "]"));
		server.runCommand(place(at, "@ wheat[age=" + age + "]"));
		// The tracker rescans every 5 ticks.
		context.waitTicks(15);
	}

	private record Pixels(int green, int yellow, int red, int note, int yellowLeft, int yellowRight, int greenLeft, int greenRight) {
	}

	/** How bright this version draws a bar in the world, against its colour. */
	private static final float WORLD_SHADE = 1;

	/** Above the chat and the hotbar, where the bars are. */
	private static Pixels worldPixels(Path screenshot) {
		return pixels(screenshot, 854, 300, 12, WORLD_SHADE);
	}

	/** Where the crop panel is, by default. Before 26.1 text is drawn a shade off its colour. */
	private static Pixels panelPixels(Path screenshot) {
		return pixels(screenshot, 450, 110, 6, 1);
	}

	private static Pixels pixels(Path screenshot, int width, int height, int tolerance, float shade) {
		BufferedImage image;
		try {
			image = ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new AssertionError("Could not read " + screenshot, e);
		}
		int[] counts = new int[4];
		int[] colors = {shaded(MeterStyle.GREEN, shade), shaded(MeterStyle.YELLOW, shade), shaded(MeterStyle.RED, shade), 0xFFA0A0A0};
		int[] left = {Integer.MAX_VALUE, Integer.MAX_VALUE};
		int[] right = {-1, -1};
		for (int y = 0; y < Math.min(height, image.getHeight()); y++) {
			for (int x = 0; x < Math.min(width, image.getWidth()); x++) {
				for (int c = 0; c < colors.length; c++) {
					if (near(image.getRGB(x, y), colors[c], tolerance)) {
						counts[c]++;
						if (c <= 1) {
							left[c] = Math.min(left[c], x);
							right[c] = Math.max(right[c], x);
						}
					}
				}
			}
		}
		return new Pixels(counts[0], counts[1], counts[2], counts[3], left[1], right[1], left[0], right[0]);
	}

	private static int shaded(int color, float shade) {
		return Math.round((color >> 16 & 255) * shade) << 16 | Math.round((color >> 8 & 255) * shade) << 8 | Math.round((color & 255) * shade);
	}

	private static boolean near(int a, int b, int tolerance) {
		return Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255)) <= tolerance;
	}

	/** {@code want}: which of Green, Yellow, Red and greY note the panel has, and so which it must not. */
	private static void expectPanel(List<String> failures, String name, Path screenshot, String want) {
		Pixels got = panelPixels(screenshot);
		expect(failures, "panel " + name + ": want " + want + ", got " + got,
				want.contains("G") == got.green > 0 && want.contains("Y") == got.yellow > 0
						&& // A note line is well over 1000 pixels; the odd icon pixel lands near its grey.
						want.contains("R") == got.red > 0 && want.contains("N") == got.note > 50);
	}

	private static boolean savedShowBars() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(IndicatorBars.MOD_ID + ".json");
		try {
			JsonObject saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			return saved.getAsJsonObject("crops").get("showBars").getAsBoolean();
		} catch (IOException e) {
			throw new AssertionError("Could not read " + path, e);
		}
	}

	/** The crops block of the config file: read, defaulted and written back. */
	private static void cropConfig(List<String> failures) {
		try {
			Path dir = Files.createTempDirectory("indicatorbars-test");
			Path path = dir.resolve("config.json");

			CropsConfig fresh = IBConfig.load(path).crops;
			CropsConfig defaults = new CropsConfig();
			expect(failures, "config: no file, default crops", fresh.showBars && fresh.barMode == CropsConfig.BarMode.ALL
					&& fresh.barDistance == 12 && fresh.maxBars == 256 && !fresh.warnCrowded && fresh.targetRange == defaults.targetRange
					&& fresh.stageText == CropsConfig.StageText.FRACTION);
			expect(failures, "config: written on first load", Files.exists(path));

			Files.writeString(path, "{\"crops\":{\"showBars\":false,\"barMode\":\"growing\",\"barDistance\":5,\"stageText\":\"percent\",\"warnCrowded\":true}}");
			CropsConfig set = IBConfig.load(path).crops;
			expect(failures, "config: values read", !set.showBars && set.barMode == CropsConfig.BarMode.GROWING
					&& set.barDistance == 5 && set.stageText == CropsConfig.StageText.PERCENT && set.warnCrowded);
			expect(failures, "config: unset values default", set.maxBars == 256 && set.panelAnchor == defaults.panelAnchor);
			CropsConfig again = IBConfig.load(path).crops;
			expect(failures, "config: survives being written back", again.barMode == CropsConfig.BarMode.GROWING && !again.showBars);

			Files.writeString(path, "{\"crops\":{\"barMode\":\"nonsense\",\"stageText\":\"nonsense\",\"panelAnchor\":\"nonsense\"}}");
			CropsConfig bad = IBConfig.load(path).crops;
			expect(failures, "config: bad values default", bad.barMode == defaults.barMode && bad.stageText == defaults.stageText
					&& bad.panelAnchor == defaults.panelAnchor);

			Files.writeString(path, "{\"health\":{}}");
			expect(failures, "config: an older file gains crops", IBConfig.load(path).crops != null);

			Files.writeString(path, "{not json");
			expect(failures, "config: broken file, defaults", IBConfig.load(path).crops.barMode == defaults.barMode);
		} catch (IOException e) {
			throw new AssertionError("Could not test the config", e);
		}
	}

	/** Every slot's count of one item, on the client and the server, which must agree. */
	private static int countOf(ClientGameTestContext context, TestServerContext server, List<String> failures, String id) {
		int client = context.computeOnClient(minecraft -> count(minecraft.player.getInventory(), id));
		int onServer = server.computeOnServer(minecraftServer -> count(minecraftServer.getPlayerList().getPlayers().get(0).getInventory(), id));
		expect(failures, id + ": client and server agree, got " + client + " and " + onServer, client == onServer);
		return onServer;
	}

	private static int count(Inventory inventory, String id) {
		int total = 0;
		for (int i = 0; i < 36; i++) {
			if (BuiltInRegistries.ITEM.getKey(inventory.getItem(i).getItem()).toString().equals(id)) {
				total += inventory.getItem(i).getCount();
			}
		}
		return total;
	}

	private static void expectCount(ClientGameTestContext context, TestServerContext server, List<String> failures, String what, String id, int want) {
		int got = countOf(context, server, failures, id);
		expect(failures, what + ": want " + want + " " + id + ", got " + got, got == want);
	}

	/** What the open panel offers, once it has caught up. */
	private static List<String> offered(ClientGameTestContext context) {
		context.runOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).refreshSoon());
		context.waitTicks(2);
		return context.computeOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).offered());
	}

	private static void clickQuickCraft(ClientGameTestContext context) {
		int[] at = context.computeOnClient(minecraft -> {
			QuickCraftButton arrow = widget(minecraft, QuickCraftButton.class);
			return new int[] {arrow.getX() + arrow.getWidth() / 2, arrow.getY() + arrow.getHeight() / 2};
		});
		clickAt(context, at[0], at[1], LEFT_BUTTON);
		context.waitTicks(2);
	}

	/**
	 * A click on an offered item with Ctrl or Shift. The test input's mouse press always says no modifier
	 * is held, so this goes straight to the screen, with what a real click carries.
	 */
	private static void clickOfferWith(ClientGameTestContext context, List<String> failures, String id, int modifiers) {
		offered(context);
		int[] at = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).cellCentre(id));
		expect(failures, "offered, so it can be clicked: " + id, at != null);
		if (at != null) {
			context.runOnClient(minecraft -> minecraft.screen.mouseClicked(new MouseButtonEvent(at[0] + 0.5, at[1] + 0.5, new MouseButtonInfo(LEFT_BUTTON, modifiers)), false));
			context.waitTicks(1);
		}
	}

	/** A real click on an offered item. */
	private static void clickOffer(ClientGameTestContext context, List<String> failures, String id) {
		offered(context);
		int[] at = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).cellCentre(id));
		expect(failures, "offered, so it can be clicked: " + id, at != null);
		if (at != null) {
			clickAt(context, at[0], at[1], LEFT_BUTTON);
		}
	}

	private static final String[][] CRAFT_MATERIALS = {{"inventory.0", "oak_planks 30"}, {"inventory.1", "coal 3"}, {"inventory.2", "stick 4"}, {"inventory.3", "iron_ingot 4"}};

	/** How many of each panel exist and are open: never more than one of each, never both open. */
	private static void expectOnePanel(ClientGameTestContext context, List<String> failures, String what, boolean sortOpen, boolean listOpen) {
		context.waitTicks(2);
		int[] state = context.computeOnClient(minecraft -> {
			int[] counts = new int[4];
			for (AbstractWidget widget : Screens.getWidgets(minecraft.screen)) {
				if (widget instanceof SortPanel) {
					counts[0]++;
					counts[1] += widget.visible ? 1 : 0;
				} else if (widget instanceof QuickCraftPanel) {
					counts[2]++;
					counts[3] += widget.visible ? 1 : 0;
				}
			}
			return counts;
		});
		expect(failures, what + ": one of each panel, got " + Arrays.toString(state), state[0] == 1 && state[2] == 1);
		expect(failures, what + ": sort open " + sortOpen + ", list open " + listOpen + ", got " + Arrays.toString(state),
				state[1] == (sortOpen ? 1 : 0) && state[3] == (listOpen ? 1 : 0));
	}

	private static boolean recipeBookOpen(Minecraft minecraft) {
		return ((net.firstwemine1.indicatorbars.platform.mixin.RecipeBookScreenAccessor) minecraft.screen).getRecipeBookComponent().isVisible();
	}

	private static void clickRecipeBook(ClientGameTestContext context) {
		int[] at = context.computeOnClient(minecraft -> {
			ImageButton book = widget(minecraft, ImageButton.class);
			return new int[] {book.getX() + 10, book.getY() + 9};
		});
		clickAt(context, at[0], at[1], LEFT_BUTTON);
		context.waitTicks(3);
	}

	/** A widget's rectangle in screenshot pixels. */
	private static int[] buttonRect(AbstractWidget widget) {
		int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
		return new int[] {widget.getX() * scale, widget.getY() * scale, widget.getRight() * scale, widget.getBottom() * scale};
	}

	private static int colourCount(Path screenshot, int[] rect, int colour) {
		BufferedImage image;
		try {
			image = ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new AssertionError("Could not read " + screenshot, e);
		}
		int count = 0;
		for (int y = rect[1]; y < rect[3]; y++) {
			for (int x = rect[0]; x < rect[2]; x++) {
				if ((image.getRGB(x, y) & 0xFFFFFF) == colour) {
					count++;
				}
			}
		}
		return count;
	}

	/** The tooltip's purple frame, the only such colour on this screen. */
	private static int tooltipPixels(Path screenshot) {
		BufferedImage image;
		try {
			image = ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new AssertionError("Could not read " + screenshot, e);
		}
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y);
				int red = rgb >> 16 & 255;
				int green = rgb >> 8 & 255;
				int blue = rgb & 255;
				// Red too: the blue of a hovered button's dark outline has none.
				if (blue > red + 30 && green < 12 && blue > 60 && red > 20) {
					count++;
				}
			}
		}
		return count;
	}

	/** Just the two panels in the inventory, opened with the real key, with the cursor left where a player's would be. */
	private static void panelsOnly(ClientGameTestContext context) {
		List<String> failures = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("gamemode survival @a");
			server.runCommand("recipe give @a *");
			fillSort(context, server, new String[][] {{"inventory.0", "oak_planks 30"}, {"inventory.1", "coal 3"}, {"inventory.2", "stick 4"}});
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitFor(minecraft -> minecraft.screen instanceof InventoryScreen);
			context.waitTicks(3);
			clearToasts(context);
			expectOnePanel(context, failures, "opened", false, false);

			clickQuickCraft(context);
			expectOnePanel(context, failures, "book", false, true);
			context.waitTicks(30);
			int onArrow = tooltipPixels(context.takeScreenshot("panels-list-cursor-on-book"));
			expect(failures, "list open, cursor on the book: no tooltip, got " + onArrow, onArrow == 0);
			clickSortButton(context, RIGHT_BUTTON);
			expectOnePanel(context, failures, "then right-click sort", true, false);
			context.waitTicks(30);
			int onSort = tooltipPixels(context.takeScreenshot("panels-sort-cursor-on-button"));
			expect(failures, "sort open, cursor on its button: no tooltip, got " + onSort, onSort == 0);
			clickQuickCraft(context);
			expectOnePanel(context, failures, "then book", false, true);
			clickSortButton(context, LEFT_BUTTON);
			context.waitTicks(4);
			expectOnePanel(context, failures, "then left-click sort", false, true);
			context.waitTicks(30);
			int onOther = tooltipPixels(context.takeScreenshot("panels-list-cursor-on-sort"));
			expect(failures, "list open, cursor on the sort button: no tooltip, got " + onOther, onOther == 0);
			clickQuickCraft(context);
			expectOnePanel(context, failures, "book closes the list", false, false);
			context.waitTicks(30);
			int closed = tooltipPixels(context.takeScreenshot("panels-closed-cursor-on-book"));
			expect(failures, "all closed, cursor on the book: the tooltip is back, got " + closed, closed > 0);
			clickSortButton(context, RIGHT_BUTTON);
			clickPanelRow(context, SortPanel.row(SortConfig.Metric.NAME));
			context.waitTicks(4);
			expectOnePanel(context, failures, "a sort choice leaves its panel open", true, false);
			clickQuickCraft(context);
			expectOnePanel(context, failures, "book after a choice", false, true);
			context.runOnClient(minecraft -> minecraft.screen.resize(minecraft.screen.width, minecraft.screen.height));
			context.waitTicks(3);
			expectOnePanel(context, failures, "a window resize starts closed", false, false);
			clickQuickCraft(context);
			clickSortButton(context, RIGHT_BUTTON);
			expectOnePanel(context, failures, "after a resize, only the last one opened", true, false);

			// Opening a panel closes the recipe book, as the book closes a panel.
			clickSortButton(context, RIGHT_BUTTON);
			expectOnePanel(context, failures, "closed again", false, false);
			int home = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftButton.class).getX());
			clickRecipeBook(context);
			expect(failures, "recipe book opens", context.computeOnClient(IndicatorBarsClientGameTest::recipeBookOpen));
			clickQuickCraft(context);
			expect(failures, "book button closes the recipe book", context.computeOnClient(minecraft -> !recipeBookOpen(minecraft)));
			expectOnePanel(context, failures, "book button, with the recipe book open", false, true);
			int backHome = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftButton.class).getX());
			expect(failures, "window back where it was, got " + backHome + " want " + home, backHome == home);
			clickRecipeBook(context);
			expect(failures, "recipe book opens over the list", context.computeOnClient(IndicatorBarsClientGameTest::recipeBookOpen));
			expectOnePanel(context, failures, "recipe book closes the list", false, false);
			clickSortButton(context, RIGHT_BUTTON);
			expect(failures, "sort menu closes the recipe book", context.computeOnClient(minecraft -> !recipeBookOpen(minecraft)));
			expectOnePanel(context, failures, "sort menu, with the recipe book open", true, false);
			clickSortButton(context, RIGHT_BUTTON);

			// The same colours as the recipe book button, grey, and blue under the cursor.
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(3);
			Path normal = context.takeScreenshot("button-normal");
			int[] greys = context.computeOnClient(minecraft -> buttonRect(widget(minecraft, QuickCraftButton.class)));
			int[] bookRect = context.computeOnClient(minecraft -> buttonRect(widget(minecraft, ImageButton.class)));
			expect(failures, "normal: the recipe book's grey", colourCount(normal, greys, 0xC6C6C6) > 0 && colourCount(normal, greys, 0x555555) > 0);
			expect(failures, "normal: no blue", colourCount(normal, greys, 0x8892C9) == 0);
			expect(failures, "normal: the recipe book button has this grey too", colourCount(normal, bookRect, 0xC6C6C6) > 0);
			int[] centre = context.computeOnClient(minecraft -> {
				QuickCraftButton button = widget(minecraft, QuickCraftButton.class);
				return new int[] {button.getX() + 10, button.getY() + 9};
			});
			double scale = context.computeOnClient(minecraft -> (double) minecraft.getWindow().getGuiScale());
			context.getInput().setCursorPos((centre[0] + 0.5) * scale, (centre[1] + 0.5) * scale);
			context.waitTicks(3);
			Path hovered = context.takeScreenshot("button-hovered");
			expect(failures, "hovered: the recipe book's blue", colourCount(hovered, greys, 0x8892C9) > 0 && colourCount(hovered, greys, 0x343E75) > 0);

			// After a click it stays blue until something else is clicked, exactly as the recipe book button does.
			clickQuickCraft(context);
			clickQuickCraft(context);
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(3);
			Path afterOurs = context.takeScreenshot("button-after-click");
			expect(failures, "after a click on ours: ours blue, the recipe book's grey",
					colourCount(afterOurs, greys, 0x8892C9) > 0 && colourCount(afterOurs, bookRect, 0x8892C9) == 0);
			clickRecipeBook(context);
			clickRecipeBook(context);
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(3);
			Path afterVanilla = context.takeScreenshot("recipe-book-after-click");
			int[] greysNow = context.computeOnClient(minecraft -> buttonRect(widget(minecraft, QuickCraftButton.class)));
			int[] bookNow = context.computeOnClient(minecraft -> buttonRect(widget(minecraft, ImageButton.class)));
			expect(failures, "after a click on the recipe book: it blue, ours grey",
					colourCount(afterVanilla, bookNow, 0x8892C9) > 0 && colourCount(afterVanilla, greysNow, 0x8892C9) == 0);
			context.setScreen(() -> null);
		}
		IndicatorBars.LOGGER.info("[panel-test] {} checks, {} failed", checks, failures.size());
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed checks:\n  " + String.join("\n  ", failures));
		}
	}

	/** Just quick craft, in a world of its own and on a real server. */
	private static void craftOnly(ClientGameTestContext context) {
		List<String> failures = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			world.getServer().runCommand("time set noon");
			craftScene(context, world.getServer(), failures);
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				var connection = server.connect()) {
			connection.getClientLevel().waitForChunksRender();
			server.runCommand("gamemode survival @a");
			craftOnDedicatedServer(context, server, failures);
		}
		IndicatorBars.LOGGER.info("[craft-test] {} checks, {} failed", checks, failures.size());
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed checks:\n  " + String.join("\n  ", failures));
		}
	}

	/** One item's count across the crafting grid and in the output slot of whatever crafting menu is open. */
	private static int[] gridAndOutput(net.minecraft.world.inventory.AbstractContainerMenu open, String id) {
		var menu = (net.minecraft.world.inventory.AbstractCraftingMenu) open;
		int grid = 0;
		for (var slot : menu.getInputGridSlots()) {
			if (BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString().equals(id)) {
				grid += slot.getItem().getCount();
			}
		}
		ItemStack output = menu.getResultSlot().getItem();
		return new int[] {grid, BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals(id) ? output.getCount() : 0};
	}

	/** What the grid holds of an ingredient and the output slot of a result, the same on both sides. */
	private static void expectGrid(ClientGameTestContext context, TestServerContext server, List<String> failures, String what,
			String ingredient, int inGrid, String result, int inOutput) {
		context.waitTicks(4);
		int clientGrid = context.computeOnClient(minecraft -> gridAndOutput(minecraft.player.containerMenu, ingredient)[0]);
		int clientOut = context.computeOnClient(minecraft -> gridAndOutput(minecraft.player.containerMenu, result)[1]);
		int serverGrid = server.computeOnServer(minecraftServer -> gridAndOutput(minecraftServer.getPlayerList().getPlayers().get(0).containerMenu, ingredient)[0]);
		int serverOut = server.computeOnServer(minecraftServer -> gridAndOutput(minecraftServer.getPlayerList().getPlayers().get(0).containerMenu, result)[1]);
		String got = "client " + clientGrid + "/" + clientOut + ", server " + serverGrid + "/" + serverOut;
		expect(failures, what + ": want " + inGrid + " " + ingredient + " in the grid and " + inOutput + " " + result + " waiting, got " + got,
				clientGrid == inGrid && serverGrid == inGrid && clientOut == inOutput && serverOut == inOutput);
	}

	/** Real clicks: take the waiting result, then put it down in an inventory slot. */
	private static void takeResultTo(ClientGameTestContext context, int slotNumber) {
		int[][] at = context.computeOnClient(minecraft -> {
			var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) minecraft.screen;
			var menu = (net.minecraft.world.inventory.AbstractCraftingMenu) screen.getMenu();
			int left = (screen.width - 176) / 2;
			int top = (screen.height - 166) / 2;
			var output = menu.getResultSlot();
			var target = menu.getSlot(slotNumber);
			return new int[][] {{left + output.x + 8, top + output.y + 8}, {left + target.x + 8, top + target.y + 8}};
		});
		clickAt(context, at[0][0], at[0][1], LEFT_BUTTON);
		clickAt(context, at[1][0], at[1][1], LEFT_BUTTON);
		context.waitTicks(3);
	}

	/** In the inventory: the next button along from sort, the same size and on its row, inside the window. */
	private static void expectInventoryButton(ClientGameTestContext context, List<String> failures) {
		int[] at = context.computeOnClient(minecraft -> {
			QuickCraftButton button = widget(minecraft, QuickCraftButton.class);
			SortButton sort = widget(minecraft, SortButton.class);
			return new int[] {button.getX(), button.getY(), button.getWidth(), button.getHeight(), sort.getRight(), sort.getY(), sort.getWidth(), sort.getHeight(),
					(minecraft.screen.width + 176) / 2};
		});
		String got = Arrays.toString(at);
		expect(failures, "inventory button: same size as the sort button, got " + got, at[2] == at[6] && at[3] == at[7]);
		expect(failures, "inventory button: right of the sort button, got " + got, at[0] == at[4] + 2);
		expect(failures, "inventory button: on its row, got " + got, at[1] == at[5]);
		expect(failures, "inventory button: inside the window, got " + got, at[0] + at[2] <= at[8]);
	}

	/** At a table: right of the output slot, centred on it, level with the recipe book, inside the window. */
	private static void expectTableButton(ClientGameTestContext context, List<String> failures) {
		int[] at = context.computeOnClient(minecraft -> {
			QuickCraftButton button = widget(minecraft, QuickCraftButton.class);
			ImageButton book = widget(minecraft, ImageButton.class);
			CraftingScreen screen = (CraftingScreen) minecraft.screen;
			var output = screen.getMenu().getResultSlot();
			int left = (screen.width - 176) / 2;
			int top = (screen.height - 166) / 2;
			return new int[] {button.getX(), button.getY(), button.getRight(), button.getHeight(), left + output.x + 16 + 5, top + output.y - 5, 26, book.getY(), left + 176};
		});
		String got = Arrays.toString(at);
		expect(failures, "table button: right of the output slot, got " + got, at[0] >= at[4] && at[0] <= at[4] + 2);
		expect(failures, "table button: centred on the output slot, got " + got, 2 * at[1] + at[3] == 2 * at[5] + at[6]);
		expect(failures, "table button: level with the recipe book, got " + got, at[1] == at[7]);
		expect(failures, "table button: inside the window, got " + got, at[2] <= at[8]);
	}

	private static void craftScene(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		int mark = checks;
		server.runCommand("gamemode survival @a");
		server.runCommand("recipe give @a *");
		fillSort(context, server, CRAFT_MATERIALS);

		openInventory(context);
		clearToasts(context);
		expectInventoryButton(context, failures);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.takeScreenshot("craft-button");

		// What the 2x2 grid offers, and the list clear of the arrow.
		clickQuickCraft(context);
		boolean opened = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).visible);
		expect(failures, "button: opens the list", opened);
		List<String> small = offered(context);
		for (String id : List.of("minecraft:stick", "minecraft:torch", "minecraft:crafting_table")) {
			expect(failures, "inventory offers " + id + ", got " + small, small.contains(id));
		}
		for (String id : List.of("minecraft:iron_sword", "minecraft:iron_pickaxe", "minecraft:wooden_sword", "minecraft:chest")) {
			expect(failures, "inventory leaves out " + id + ", got " + small, !small.contains(id));
		}
		boolean clear = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftPanel.class).getX() >= widget(minecraft, QuickCraftButton.class).getRight());
		expect(failures, "list: outside the window, clear of the button", clear);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.takeScreenshot("craft-list");

		// A click fills the grid and makes nothing: the result waits in the output slot.
		clickOffer(context, failures, "minecraft:stick");
		expectGrid(context, server, failures, "click", "minecraft:oak_planks", 2, "minecraft:stick", 4);
		expectCount(context, server, failures, "click: nothing made yet", "minecraft:stick", 4);
		expectCount(context, server, failures, "click: ingredients moved to the grid", "minecraft:oak_planks", 28);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.takeScreenshot("craft-filled");

		// Again adds one more craft's worth; taking the result is what makes it, one craft at a time.
		clickOffer(context, failures, "minecraft:stick");
		expectGrid(context, server, failures, "second click", "minecraft:oak_planks", 4, "minecraft:stick", 4);
		expectCount(context, server, failures, "second click: nothing made yet", "minecraft:stick", 4);
		takeResultTo(context, 20);
		expectCount(context, server, failures, "took the result by hand", "minecraft:stick", 8);
		expectGrid(context, server, failures, "took the result by hand", "minecraft:oak_planks", 2, "minecraft:stick", 4);

		// Choosing something else sends the grid back first.
		clickOffer(context, failures, "minecraft:torch");
		expectGrid(context, server, failures, "another item", "minecraft:coal", 1, "minecraft:torch", 4);
		expectCount(context, server, failures, "another item: the planks came back", "minecraft:oak_planks", 28);

		// Shift fills as far as the materials go, and still makes nothing. Ctrl is just a click now.
		context.runOnClient(minecraft -> minecraft.screen.onClose());
		context.waitTicks(4);
		expectCount(context, server, failures, "closing: ingredients back", "minecraft:coal", 3);
		openInventory(context);
		clickQuickCraft(context);
		clickOfferWith(context, failures, "minecraft:torch", InputConstants.MOD_SHIFT);
		expectGrid(context, server, failures, "Shift", "minecraft:coal", 3, "minecraft:torch", 4);
		expectCount(context, server, failures, "Shift: nothing made yet", "minecraft:torch", 0);
		clickOfferWith(context, failures, "minecraft:stick", InputConstants.MOD_CONTROL);
		expectGrid(context, server, failures, "Ctrl is a plain click", "minecraft:oak_planks", 2, "minecraft:stick", 4);
		context.runOnClient(minecraft -> minecraft.screen.onClose());
		context.waitTicks(4);

		// An item on the cursor: the grid is left alone.
		fillSort(context, server, CRAFT_MATERIALS);
		openInventory(context);
		clickQuickCraft(context);
		offered(context);
		context.runOnClient(minecraft -> minecraft.gameMode.handleContainerInput(0, 11, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player));
		context.waitTicks(3);
		clickOffer(context, failures, "minecraft:crafting_table");
		expectGrid(context, server, failures, "item on the cursor", "minecraft:oak_planks", 0, "minecraft:crafting_table", 0);
		context.runOnClient(minecraft -> minecraft.gameMode.handleContainerInput(0, 11, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player));
		context.waitTicks(3);

		// One panel at a time.
		clickSortButton(context, RIGHT_BUTTON);
		boolean[] panels = context.computeOnClient(minecraft -> new boolean[] {widget(minecraft, QuickCraftPanel.class).visible, widget(minecraft, SortPanel.class).visible});
		expect(failures, "sort panel replaces the list", !panels[0] && panels[1]);
		clickQuickCraft(context);
		panels = context.computeOnClient(minecraft -> new boolean[] {widget(minecraft, QuickCraftPanel.class).visible, widget(minecraft, SortPanel.class).visible});
		expect(failures, "list replaces the sort panel", panels[0] && !panels[1]);
		context.setScreen(() -> null);
		context.waitTicks(2);

		// At a real table: 3x3 recipes, the same way.
		fillSort(context, server, new String[][] {{"inventory.0", "iron_ingot 6"}, {"inventory.1", "stick 8"}, {"inventory.2", "oak_planks 12"}});
		BlockPos table = context.computeOnClient(minecraft -> minecraft.player.blockPosition()).offset(0, 0, 2);
		server.runCommand(place(table, "@ crafting_table"));
		server.runCommand(place(table, "@0,1,0 air"));
		context.waitTicks(5);
		context.runOnClient(minecraft -> minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false)));
		context.waitFor(minecraft -> minecraft.screen instanceof CraftingScreen);
		context.waitTicks(3);
		clearToasts(context);
		expectTableButton(context, failures);
		clickQuickCraft(context);
		List<String> big = offered(context);
		for (String id : List.of("minecraft:iron_sword", "minecraft:iron_pickaxe", "minecraft:chest", "minecraft:stick")) {
			expect(failures, "table offers " + id + ", got " + big, big.contains(id));
		}
		expect(failures, "table leaves out what there's no material for, got " + big, !big.contains("minecraft:diamond_sword"));
		clickOffer(context, failures, "minecraft:iron_sword");
		expectGrid(context, server, failures, "table", "minecraft:iron_ingot", 2, "minecraft:iron_sword", 1);
		expectCount(context, server, failures, "table: nothing made yet", "minecraft:iron_sword", 0);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		context.takeScreenshot("craft-table-filled");
		takeResultTo(context, 20);
		expectCount(context, server, failures, "table: took the sword by hand", "minecraft:iron_sword", 1);
		expectCount(context, server, failures, "table: it used the iron", "minecraft:iron_ingot", 4);
		context.runOnClient(minecraft -> minecraft.screen.onClose());
		context.waitTicks(4);
		server.runCommand(place(table, "@ air"));

		// Turned off in the config.
		context.runOnClient(minecraft -> liveConfig().craft.showButton = false);
		openInventory(context);
		boolean none = context.computeOnClient(minecraft -> widget(minecraft, QuickCraftButton.class) == null);
		expect(failures, "button: off in the config", none);
		context.setScreen(() -> null);
		context.runOnClient(minecraft -> {
			liveConfig().craft.showButton = true;
			liveConfig().save();
		});
		context.waitTicks(2);
		craftChecks += checks - mark;
	}

	/** Over a real connection, as a plain survival player. */
	private static void craftOnDedicatedServer(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		int mark = checks;
		server.runCommand("recipe give @a *");
		fillSort(context, server, CRAFT_MATERIALS);
		context.waitTicks(10);
		openInventory(context);
		clickQuickCraft(context);
		clickOffer(context, failures, "minecraft:stick");
		expectGrid(context, server, failures, "server: click", "minecraft:oak_planks", 2, "minecraft:stick", 4);
		expectCount(context, server, failures, "server: nothing made yet", "minecraft:stick", 4);
		takeResultTo(context, 20);
		expectCount(context, server, failures, "server: took the result by hand", "minecraft:stick", 8);
		context.runOnClient(minecraft -> minecraft.screen.onClose());
		context.waitTicks(4);
		craftChecks += checks - mark;
	}

	/** A messy inventory, by command slot. Stone comes in two stacks that must merge. */
	private static final String[][] SORT_ITEMS = {
			{"inventory.0", "stone 5"}, {"inventory.3", "diamond_sword"}, {"inventory.5", "dirt 10"}, {"inventory.9", "stone 7"},
			{"inventory.12", "cobblestone_stairs 4"}, {"inventory.14", "oak_planks 64"}, {"inventory.17", "apple 3"},
			{"inventory.20", "iron_pickaxe"}, {"inventory.22", "iron_axe"}, {"inventory.24", "iron_shovel"},
			{"inventory.25", "bow"}, {"inventory.26", "oak_slab 2"},
	};
	/** Never moved by a sort. */
	private static final String[][] SORT_FIXED = {
			{"hotbar.0", "torch 3"}, {"hotbar.1", "apple 1"}, {"armor.head", "iron_helmet"}, {"weapon.offhand", "shield"},
	};
	private static final String[] BY_NAME = {"apple:3", "bow:1", "cobblestone_stairs:4", "diamond_sword:1", "dirt:10", "iron_axe:1",
			"iron_pickaxe:1", "iron_shovel:1", "oak_planks:64", "oak_slab:2", "stone:12"};
	private static final String[] BY_QUANTITY = {"oak_planks:64", "stone:12", "dirt:10", "cobblestone_stairs:4", "apple:3", "oak_slab:2",
			"bow:1", "diamond_sword:1", "iron_axe:1", "iron_pickaxe:1", "iron_shovel:1"};
	private static final String[] BY_TYPE = {"bow:1", "diamond_sword:1", "iron_axe:1", "iron_pickaxe:1", "iron_shovel:1",
			"dirt:10", "oak_planks:64", "stone:12", "cobblestone_stairs:4", "oak_slab:2", "apple:3"};
	private static final String[] COMBAT_PICKED = {"bow:1", "diamond_sword:1", "iron_axe:1", "iron_pickaxe:1", "iron_shovel:1"};
	private static final String[] COMBAT_REST = {"apple:3", "cobblestone_stairs:4", "dirt:10", "oak_planks:64", "oak_slab:2", "stone:12"};
	private static final String[] BUILDER_PICKED = {"iron_pickaxe:1", "iron_axe:1", "iron_shovel:1", "cobblestone_stairs:4", "dirt:10",
			"oak_planks:64", "oak_slab:2", "stone:12"};
	private static final String[] BUILDER_REST = {"apple:3", "bow:1", "diamond_sword:1"};
	private static final int LEFT_BUTTON = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RIGHT_BUTTON = InputConstants.MOUSE_BUTTON_RIGHT;

	/** The 27 row slots, as inventory indices 9 to 35: {@code rest} from the top, {@code picked} from the bottom row up. */
	private static String[] rowsWith(String[] rest, String[] picked) {
		String[] rows = new String[27];
		Arrays.fill(rows, "air:0");
		System.arraycopy(rest, 0, rows, 0, rest.length);
		for (int i = 0; i < picked.length; i++) {
			rows[(2 - i / 9) * 9 + i % 9] = picked[i];
		}
		return rows;
	}

	private static String[] rowsOf(String[] items) {
		return Arrays.copyOfRange(items, 9, 36);
	}

	private static void fillSort(ClientGameTestContext context, TestServerContext server, String[][]... groups) {
		server.runCommand("clear @a");
		for (String[][] group : groups) {
			for (String[] item : group) {
				String[] parts = item[1].split(" ");
				server.runCommand("item replace entity @a " + item[0] + " with minecraft:" + parts[0] + (parts.length > 1 ? " " + parts[1] : ""));
			}
		}
		context.waitTicks(8);
	}

	private static String idCount(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ":" + stack.getCount();
	}

	/** Hotbar slots 0 and 1, armor and offhand as {@link #SORT_FIXED} set them, on both sides. */
	private static void expectFixed(ClientGameTestContext context, TestServerContext server, List<String> failures, String what) {
		String want = "torch:3 apple:1 air:0 iron_helmet:1 shield:1";
		String client = context.computeOnClient(minecraft -> fixedOf(minecraft.player.getInventory(), minecraft.player.getItemBySlot(EquipmentSlot.HEAD), minecraft.player.getOffhandItem()));
		String onServer = server.computeOnServer(minecraftServer -> {
			var player = minecraftServer.getPlayerList().getPlayers().get(0);
			return fixedOf(player.getInventory(), player.getItemBySlot(EquipmentSlot.HEAD), player.getOffhandItem());
		});
		expect(failures, what + ": hotbar, armor, offhand kept on the client, got " + client, want.equals(client));
		expect(failures, what + ": hotbar, armor, offhand kept on the server, got " + onServer, want.equals(onServer));
	}

	private static String fixedOf(Inventory inventory, ItemStack head, ItemStack offhand) {
		return idCount(inventory.getItem(0)) + " " + idCount(inventory.getItem(1)) + " " + idCount(inventory.getItem(2)) + " " + idCount(head) + " " + idCount(offhand);
	}

	private static void expectRows(ClientGameTestContext context, TestServerContext server, List<String> failures, String what, String[] want) {
		String[] client = rowsOf(context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory())));
		String[] onServer = rowsOf(server.computeOnServer(IndicatorBarsClientGameTest::serverInventory));
		expect(failures, what + ": client rows, got " + String.join(" ", client), Arrays.equals(want, client));
		expect(failures, what + ": server rows, got " + String.join(" ", onServer), Arrays.equals(want, onServer));
	}

	/** Starts a sort and waits for it to finish. */
	private static void sortNow(ClientGameTestContext context) {
		context.runOnClient(minecraft -> InventorySorter.start(minecraft, liveConfig()));
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
	}

	private static void setSort(ClientGameTestContext context, SortConfig.Metric metric, SortConfig.Preset preset) {
		context.runOnClient(minecraft -> {
			SortConfig sort = liveConfig().sort;
			sort.metric = metric;
			sort.preset = preset;
		});
	}

	/** One order or preset from the messy inventory: the result, what must stay, and a second sort costing nothing. */
	private static void sortCase(ClientGameTestContext context, TestServerContext server, List<String> failures, String what,
			SortConfig.Metric metric, SortConfig.Preset preset, String[] want) {
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		setSort(context, metric, preset);
		sortNow(context);
		expectRows(context, server, failures, what, want);
		expectFixed(context, server, failures, what);
		int first = context.computeOnClient(minecraft -> InventorySorter.clicks());
		expect(failures, what + ": clicks sent, got " + first, first > 0);
		sortNow(context);
		int second = context.computeOnClient(minecraft -> InventorySorter.clicks());
		expect(failures, what + ": sorting again sends nothing, got " + second, second == 0);
		expectRows(context, server, failures, what + " again", want);
	}

	private static <T extends AbstractWidget> T widget(Minecraft minecraft, Class<T> type) {
		for (AbstractWidget widget : Screens.getWidgets(minecraft.screen)) {
			if (type.isInstance(widget)) {
				return type.cast(widget);
			}
		}
		return null;
	}

	/** A real click at a GUI position. */
	private static void clickAt(ClientGameTestContext context, int x, int y, int button) {
		double scale = context.computeOnClient(minecraft -> (double) minecraft.getWindow().getGuiScale());
		context.getInput().setCursorPos((x + 0.5) * scale, (y + 0.5) * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(button);
		context.waitTicks(2);
	}

	private static void clickSortButton(ClientGameTestContext context, int button) {
		int[] at = context.computeOnClient(minecraft -> {
			SortButton sort = widget(minecraft, SortButton.class);
			return new int[] {sort.getX() + sort.getWidth() / 2, sort.getY() + sort.getHeight() / 2};
		});
		clickAt(context, at[0], at[1], button);
	}

	private static void clickPanelRow(ClientGameTestContext context, int row) {
		int[] at = context.computeOnClient(minecraft -> {
			SortPanel panel = widget(minecraft, SortPanel.class);
			return new int[] {panel.getX() + panel.getWidth() / 2, panel.getY() + SortPanel.PADDING + row * SortPanel.ROW_HEIGHT + SortPanel.ROW_HEIGHT / 2};
		});
		clickAt(context, at[0], at[1], LEFT_BUTTON);
	}

	/** Advancement popups would cover what the screenshot is for. */
	private static void clearToasts(ClientGameTestContext context) {
		context.runOnClient(minecraft -> minecraft.getToastManager().clear());
		context.waitTicks(2);
	}

	private static void openInventory(ClientGameTestContext context) {
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(3);
	}

	private static void expectKind(List<String> failures, Item item, ItemKind want) {
		ItemKind got = ItemKind.of(new ItemStack(item));
		expect(failures, "type of " + item + ": want " + want + ", got " + got, got == want);
	}

	private static void sortConfig(List<String> failures) {
		try {
			Path path = Files.createTempDirectory("indicatorbars-test").resolve("config.json");
			SortConfig fresh = IBConfig.load(path).sort;
			expect(failures, "sort config: defaults", fresh.showButton && fresh.metric == SortConfig.Metric.TYPE
					&& fresh.preset == SortConfig.Preset.NONE && fresh.movesPerTick == 8);
			Files.writeString(path, "{\"sort\":{\"metric\":\"quantity\",\"preset\":\"builder\",\"showButton\":false,\"movesPerTick\":3}}");
			SortConfig set = IBConfig.load(path).sort;
			expect(failures, "sort config: read back", set.metric == SortConfig.Metric.QUANTITY && set.preset == SortConfig.Preset.BUILDER
					&& !set.showButton && set.movesPerTick == 3);
			SortConfig again = IBConfig.load(path).sort;
			expect(failures, "sort config: survives being written", again.metric == SortConfig.Metric.QUANTITY && again.preset == SortConfig.Preset.BUILDER);
			Files.writeString(path, "{\"sort\":{\"metric\":\"nope\",\"preset\":\"nope\",\"movesPerTick\":0}}");
			SortConfig bad = IBConfig.load(path).sort;
			expect(failures, "sort config: bad values get defaults", bad.metric == fresh.metric && bad.preset == fresh.preset && bad.movesPerTick == 1);
			Files.writeString(path, "{\"sort\":{\"movesPerTick\":9999}}");
			expect(failures, "sort config: too many moves is capped", IBConfig.load(path).sort.movesPerTick == SortConfig.MAX_MOVES_PER_TICK);
			Files.writeString(path, "{\"crops\":{}}");
			expect(failures, "sort config: an older file gains sort", IBConfig.load(path).sort != null);
		} catch (IOException e) {
			throw new AssertionError("Could not test the config", e);
		}
	}

	private static void sortScene(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		int mark = checks;
		sortConfig(failures);
		for (Item item : List.of(Items.DIAMOND_SWORD, Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.MACE, Items.SHIELD, Items.ARROW)) {
			expectKind(failures, item, ItemKind.WEAPON);
		}
		for (Item item : List.of(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.SHEARS, Items.FISHING_ROD)) {
			expectKind(failures, item, ItemKind.TOOL);
		}
		expectKind(failures, Items.IRON_HELMET, ItemKind.ARMOR);
		expectKind(failures, Items.ELYTRA, ItemKind.ARMOR);
		expectKind(failures, Items.STONE, ItemKind.BLOCK);
		expectKind(failures, Items.OAK_STAIRS, ItemKind.BLOCK);
		expectKind(failures, Items.CARVED_PUMPKIN, ItemKind.BLOCK);
		expectKind(failures, Items.APPLE, ItemKind.FOOD);
		expectKind(failures, Items.STICK, ItemKind.OTHER);

		server.runCommand("gamemode survival @a");
		context.runOnClient(minecraft -> {
			IBConfig config = liveConfig();
			config.hotbars.active.clear();
			config.hotbars.count = 1;
			config.sort.movesPerTick = 8;
			minecraft.player.getInventory().setSelectedSlot(0);
		});

		// An empty inventory has nothing to do.
		server.runCommand("clear @a");
		context.waitTicks(5);
		setSort(context, SortConfig.Metric.TYPE, SortConfig.Preset.NONE);
		sortNow(context);
		int empty = context.computeOnClient(minecraft -> InventorySorter.clicks());
		expect(failures, "empty inventory: no clicks, got " + empty, empty == 0);

		// Every order and preset.
		sortCase(context, server, failures, "by name", SortConfig.Metric.NAME, SortConfig.Preset.NONE, rowsWith(BY_NAME, new String[0]));
		sortCase(context, server, failures, "by quantity", SortConfig.Metric.QUANTITY, SortConfig.Preset.NONE, rowsWith(BY_QUANTITY, new String[0]));
		sortCase(context, server, failures, "by type", SortConfig.Metric.TYPE, SortConfig.Preset.NONE, rowsWith(BY_TYPE, new String[0]));
		sortCase(context, server, failures, "combat", SortConfig.Metric.NAME, SortConfig.Preset.COMBAT, rowsWith(COMBAT_REST, COMBAT_PICKED));
		sortCase(context, server, failures, "builder", SortConfig.Metric.NAME, SortConfig.Preset.BUILDER, rowsWith(BUILDER_REST, BUILDER_PICKED));

		// A preset with more than a row's worth carries on in the row above.
		fillSort(context, server, new String[][] {{"inventory.0", "stone"}, {"inventory.1", "dirt"}, {"inventory.2", "oak_planks"}, {"inventory.3", "glass"},
				{"inventory.4", "sand"}, {"inventory.5", "gravel"}, {"inventory.6", "bricks"}, {"inventory.7", "obsidian"}, {"inventory.8", "white_wool"},
				{"inventory.9", "tnt"}, {"inventory.10", "apple"}});
		setSort(context, SortConfig.Metric.NAME, SortConfig.Preset.BUILDER);
		sortNow(context);
		expectRows(context, server, failures, "builder, two rows", rowsWith(new String[] {"apple:1"},
				new String[] {"bricks:1", "dirt:1", "glass:1", "gravel:1", "oak_planks:1", "obsidian:1", "sand:1", "stone:1", "tnt:1", "white_wool:1"}));

		// Same item with a different name is a different stack; unstackable items don't merge.
		fillSort(context, server, new String[][] {{"inventory.0", "stone 5"}, {"inventory.1", "iron_sword"}, {"inventory.2", "iron_sword"}});
		server.runCommand("item replace entity @a inventory.3 with minecraft:stone[minecraft:custom_name=\"Named\"] 5");
		context.waitTicks(4);
		setSort(context, SortConfig.Metric.TYPE, SortConfig.Preset.NONE);
		sortNow(context);
		expectRows(context, server, failures, "different stacks stay apart", rowsWith(new String[] {"iron_sword:1", "iron_sword:1", "stone:5", "stone:5"}, new String[0]));

		// A stack too big to fit in one leaves the rest behind.
		fillSort(context, server, new String[][] {{"inventory.4", "dirt 40"}, {"inventory.7", "dirt 40"}, {"inventory.9", "dirt 40"}});
		sortNow(context);
		expectRows(context, server, failures, "stacks merge up to full", rowsWith(new String[] {"dirt:64", "dirt:56"}, new String[0]));

		// An item on the cursor: no sort, nothing moved.
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		setSort(context, SortConfig.Metric.NAME, SortConfig.Preset.NONE);
		String[] before = rowsOf(context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory())));
		openInventory(context);
		context.runOnClient(minecraft -> minecraft.gameMode.handleContainerInput(0, 9, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player));
		context.waitTicks(3);
		boolean refused = context.computeOnClient(minecraft -> !InventorySorter.start(minecraft, liveConfig()));
		expect(failures, "item on the cursor: refused", refused);
		context.runOnClient(minecraft -> minecraft.gameMode.handleContainerInput(0, 9, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player));
		context.waitTicks(3);
		expectRows(context, server, failures, "item on the cursor: nothing moved", before);

		// The crafting grid stays out of it.
		context.runOnClient(minecraft -> {
			minecraft.gameMode.handleContainerInput(0, 37, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player);
			minecraft.gameMode.handleContainerInput(0, 1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, minecraft.player);
		});
		context.waitTicks(3);
		sortNow(context);
		String grid = context.computeOnClient(minecraft -> idCount(minecraft.player.inventoryMenu.getSlot(1).getItem()));
		String gridOnServer = server.computeOnServer(minecraftServer -> idCount(minecraftServer.getPlayerList().getPlayers().get(0).inventoryMenu.getSlot(1).getItem()));
		expect(failures, "crafting grid kept, got " + grid + " and " + gridOnServer, grid.equals("apple:1") && gridOnServer.equals("apple:1"));
		expectRows(context, server, failures, "sorted with something in the crafting grid", rowsWith(BY_NAME, new String[0]));
		context.setScreen(() -> null);

		sortButton(context, server, failures);
		sortOnBar(context, server, failures);
		sortInterrupted(context, server, failures);

		context.runOnClient(minecraft -> {
			IBConfig config = liveConfig();
			config.sort.metric = SortConfig.Metric.TYPE;
			config.sort.preset = SortConfig.Preset.NONE;
			config.sort.movesPerTick = 8;
			config.sort.showButton = true;
			config.hotbars.count = 1;
			config.hotbars.active.clear();
			config.save();
		});
		sortChecks += checks - mark;
	}

	/** The button and its panel, by real mouse clicks. */
	private static void sortButton(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		setSort(context, SortConfig.Metric.NAME, SortConfig.Preset.NONE);
		openInventory(context);
		int[] closed = context.computeOnClient(minecraft -> {
			SortButton sort = widget(minecraft, SortButton.class);
			ImageButton book = widget(minecraft, ImageButton.class);
			return new int[] {sort.getX(), sort.getY(), book.getRight(), book.getY(), sort.getWidth(), sort.getHeight(), book.getHeight()};
		});
		expect(failures, "button: right of the book, level with it, not overlapping, got " + Arrays.toString(closed),
				closed[0] >= closed[2] && closed[0] <= closed[2] + 4 && closed[1] == closed[3] && closed[5] == closed[6]);
		clearToasts(context);
		context.takeScreenshot("sort-button");

		clickSortButton(context, LEFT_BUTTON);
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		expectRows(context, server, failures, "left-click sorts", rowsWith(BY_NAME, new String[0]));

		// Right-click opens the panel; its rows change the settings and save them.
		boolean hidden = context.computeOnClient(minecraft -> !widget(minecraft, SortPanel.class).visible);
		expect(failures, "panel: closed to begin with", hidden);
		clickSortButton(context, RIGHT_BUTTON);
		boolean shown = context.computeOnClient(minecraft -> widget(minecraft, SortPanel.class).visible);
		expect(failures, "panel: right-click opens it", shown);
		boolean clear = context.computeOnClient(minecraft -> {
			SortPanel panel = widget(minecraft, SortPanel.class);
			return panel.getX() >= (minecraft.screen.width + 176) / 2 && panel.getRight() <= minecraft.screen.width;
		});
		expect(failures, "panel: beside the window, on screen", clear);
		clearToasts(context);
		context.takeScreenshot("sort-panel");

		// A heading isn't a choice: nothing changes and nothing sorts.
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		String[] messy = rowsOf(context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory())));
		clickPanelRow(context, 0);
		clickPanelRow(context, 4);
		context.waitTicks(4);
		expectRows(context, server, failures, "panel: clicking a heading does nothing", messy);

		// A choice sorts by itself, with no click on the button.
		clickPanelRow(context, SortPanel.row(SortConfig.Metric.QUANTITY));
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		expectRows(context, server, failures, "panel: choosing an order sorts", rowsWith(BY_QUANTITY, new String[0]));
		clickPanelRow(context, SortPanel.row(SortConfig.Preset.BUILDER));
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		expectRows(context, server, failures, "panel: choosing a preset sorts", rowsWith(new String[] {"apple:3", "bow:1", "diamond_sword:1"},
				new String[] {"iron_pickaxe:1", "iron_axe:1", "iron_shovel:1", "oak_planks:64", "stone:12", "dirt:10", "cobblestone_stairs:4", "oak_slab:2"}));
		SortConfig chosen = context.computeOnClient(minecraft -> liveConfig().sort);
		expect(failures, "panel: choices applied, got " + chosen.metric + " " + chosen.preset, chosen.metric == SortConfig.Metric.QUANTITY && chosen.preset == SortConfig.Preset.BUILDER);
		JsonObject saved = savedSort();
		expect(failures, "panel: choices saved, got " + saved, saved.get("metric").getAsString().equals("quantity") && saved.get("preset").getAsString().equals("builder"));
		clearToasts(context);
		context.takeScreenshot("sort-panel-chosen");

		// Choosing again while a slow sort is running: the newest choice wins.
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		context.runOnClient(minecraft -> liveConfig().sort.movesPerTick = 1);
		clickPanelRow(context, SortPanel.row(SortConfig.Metric.NAME));
		boolean going = context.computeOnClient(minecraft -> InventorySorter.isRunning());
		expect(failures, "panel: first choice still sorting", going);
		clickPanelRow(context, SortPanel.row(SortConfig.Preset.COMBAT));
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		expectRows(context, server, failures, "panel: a choice during a sort wins", rowsWith(COMBAT_REST, COMBAT_PICKED));
		context.runOnClient(minecraft -> liveConfig().sort.movesPerTick = 8);
		clickSortButton(context, RIGHT_BUTTON);
		boolean closedAgain = context.computeOnClient(minecraft -> !widget(minecraft, SortPanel.class).visible);
		expect(failures, "panel: right-click closes it", closedAgain);

		// Opening the recipe book moves the button with it, and closes the panel.
		clickSortButton(context, RIGHT_BUTTON);
		int[] bookAt = context.computeOnClient(minecraft -> {
			ImageButton book = widget(minecraft, ImageButton.class);
			return new int[] {book.getX() + 10, book.getY() + 9};
		});
		clickAt(context, bookAt[0], bookAt[1], LEFT_BUTTON);
		context.waitTicks(3);
		int[] open = context.computeOnClient(minecraft -> {
			SortButton sort = widget(minecraft, SortButton.class);
			ImageButton book = widget(minecraft, ImageButton.class);
			return new int[] {sort.getX(), sort.getY(), book.getRight(), book.getY(), widget(minecraft, SortPanel.class).visible ? 1 : 0};
		});
		expect(failures, "button: moved with the open book, got " + Arrays.toString(open) + " from " + Arrays.toString(closed),
				open[0] > closed[0] && open[0] >= open[2] && open[0] <= open[2] + 4 && open[1] == open[3]);
		expect(failures, "panel: closed by the book", open[4] == 0);
		context.takeScreenshot("sort-button-book-open");
		clickAt(context, bookAt[0] + (open[2] - closed[2]), bookAt[1], LEFT_BUTTON);
		context.waitTicks(3);
		int back = context.computeOnClient(minecraft -> widget(minecraft, SortButton.class).getX());
		expect(failures, "button: back when the book closes, got " + back + " want " + closed[0], back == closed[0]);
		context.setScreen(() -> null);
		context.waitTicks(2);

		// Turned off in the config.
		context.runOnClient(minecraft -> liveConfig().sort.showButton = false);
		openInventory(context);
		boolean none = context.computeOnClient(minecraft -> widget(minecraft, SortButton.class) == null);
		expect(failures, "button: off in the config", none);
		context.setScreen(() -> null);
		context.runOnClient(minecraft -> liveConfig().sort.showButton = true);
		context.waitTicks(2);
	}

	/** On bar 2 the sort leaves bar 1 alone, and bar 2 comes back with its new items. */
	private static void sortOnBar(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		setSort(context, SortConfig.Metric.NAME, SortConfig.Preset.COMBAT);
		context.runOnClient(minecraft -> {
			liveConfig().hotbars.count = 2;
			minecraft.player.getInventory().setSelectedSlot(0);
		});
		context.getInput().pressKey(ClientHooks.CYCLE_HOTBAR);
		context.waitTicks(4);
		sortNow(context);
		int active = context.computeOnClient(minecraft -> liveConfig().hotbars.active(HotbarSwitcher.worldKey(minecraft)));
		expect(failures, "on bar 2: still bar 2 afterwards, got " + active, active == 1);
		String[] onBar = context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory()));
		String[] hotbarWant = {"bow:1", "diamond_sword:1", "iron_axe:1", "iron_pickaxe:1", "iron_shovel:1", "air:0", "air:0", "air:0", "air:0"};
		expect(failures, "on bar 2: the hotbar is the sorted bar, got " + String.join(" ", onBar), Arrays.equals(hotbarWant, Arrays.copyOf(onBar, 9)));
		context.getInput().pressKey(ClientHooks.HOTBAR_DOWN);
		context.waitTicks(4);
		expectRows(context, server, failures, "on bar 2: back on bar 1", rowsWith(COMBAT_REST, COMBAT_PICKED));
		expectFixed(context, server, failures, "on bar 2: back on bar 1");
		context.runOnClient(minecraft -> liveConfig().hotbars.count = 1);
	}

	/** Slowed right down, so a sort is still going when the screen closes or a chest opens. */
	private static void sortInterrupted(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		// Closing the screen doesn't stop it.
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		setSort(context, SortConfig.Metric.NAME, SortConfig.Preset.NONE);
		context.runOnClient(minecraft -> liveConfig().sort.movesPerTick = 1);
		openInventory(context);
		context.runOnClient(minecraft -> {
			InventorySorter.start(minecraft, liveConfig());
			minecraft.player.closeContainer();
		});
		boolean stillGoing = context.computeOnClient(minecraft -> InventorySorter.isRunning());
		expect(failures, "screen closed: sort was still going", stillGoing);
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		expectRows(context, server, failures, "screen closed mid-sort: finished", rowsWith(BY_NAME, new String[0]));

		// A chest opening stops it between moves, with nothing lost, and bar 1 in the hotbar as saved.
		fillSort(context, server, SORT_ITEMS, SORT_FIXED);
		context.runOnClient(minecraft -> liveConfig().hotbars.count = 2);
		context.getInput().pressKey(ClientHooks.CYCLE_HOTBAR);
		context.waitTicks(4);
		BlockPos chest = context.computeOnClient(minecraft -> minecraft.player.blockPosition()).offset(0, 0, 2);
		server.runCommand(place(chest, "@ chest"));
		server.runCommand(place(chest, "@0,1,0 air"));
		context.waitTicks(5);
		context.runOnClient(minecraft -> {
			InventorySorter.start(minecraft, liveConfig());
			minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false));
		});
		context.waitFor(minecraft -> minecraft.player.containerMenu != minecraft.player.inventoryMenu);
		context.waitFor(minecraft -> !InventorySorter.isRunning());
		context.waitTicks(4);
		String[] rows = rowsOf(context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory())));
		expect(failures, "chest opened mid-sort: it stopped early, got " + String.join(" ", rows), !Arrays.equals(rows, rowsWith(BY_NAME, new String[0])));
		int active = context.computeOnClient(minecraft -> liveConfig().hotbars.active(HotbarSwitcher.worldKey(minecraft)));
		expect(failures, "chest opened mid-sort: saved as bar 1, got " + active, active == 0);
		expectFixed(context, server, failures, "chest opened mid-sort");
		int total = server.computeOnServer(minecraftServer -> {
			Inventory inventory = minecraftServer.getPlayerList().getPlayers().get(0).getInventory();
			int sum = 0;
			for (int i = 9; i < 36; i++) {
				sum += inventory.getItem(i).getCount();
			}
			return sum;
		});
		expect(failures, "chest opened mid-sort: every item still there, got " + total, total == 12 + 1 + 10 + 4 + 64 + 3 + 1 + 1 + 1 + 1 + 2);
		context.runOnClient(minecraft -> minecraft.player.closeContainer());
		context.waitFor(minecraft -> minecraft.player.containerMenu == minecraft.player.inventoryMenu);
		context.runOnClient(minecraft -> liveConfig().sort.movesPerTick = 8);
		sortNow(context);
		expectRows(context, server, failures, "chest closed: a new sort finishes the job", rowsWith(BY_NAME, new String[0]));
		server.runCommand(place(chest, "@ air"));
		context.runOnClient(minecraft -> liveConfig().hotbars.count = 1);
	}

	/** Over a real connection: an inventory with every slot taken, then the usual mess. */
	private static void sortOnDedicatedServer(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		int mark = checks;
		String[] ids = {"tnt", "stone", "sand", "glass", "dirt", "cobblestone", "bricks", "apple", "bread", "carrot", "potato", "beef", "stick", "coal",
				"flint", "feather", "paper", "leather", "string", "bone", "egg", "snowball", "clay_ball", "brick", "gravel", "obsidian", "torch"};
		String[][] full = new String[ids.length][];
		for (int i = 0; i < ids.length; i++) {
			full[i] = new String[] {"inventory." + i, ids[i] + " 2"};
		}
		fillSort(context, server, full, SORT_FIXED);
		context.runOnClient(minecraft -> {
			liveConfig().sort.movesPerTick = 8;
			liveConfig().hotbars.count = 1;
		});
		setSort(context, SortConfig.Metric.TYPE, SortConfig.Preset.NONE);
		sortNow(context);
		String[] rows = rowsOf(server.computeOnServer(IndicatorBarsClientGameTest::serverInventory));
		Set<String> want = new HashSet<>();
		for (String id : ids) {
			want.add(id + ":2");
		}
		expect(failures, "server, full inventory: everything still there, got " + String.join(" ", rows), want.equals(new HashSet<>(Arrays.asList(rows))));
		expectFixed(context, server, failures, "server, full inventory");
		int clicks = context.computeOnClient(minecraft -> InventorySorter.clicks());
		expect(failures, "server, full inventory: at most 3 clicks a swap, got " + clicks, clicks > 0 && clicks <= 3 * 26);
		sortNow(context);
		int again = context.computeOnClient(minecraft -> InventorySorter.clicks());
		expect(failures, "server, full inventory: sorting again sends nothing, got " + again, again == 0);

		sortCase(context, server, failures, "server: combat", SortConfig.Metric.NAME, SortConfig.Preset.COMBAT, rowsWith(COMBAT_REST, COMBAT_PICKED));
		setSort(context, SortConfig.Metric.TYPE, SortConfig.Preset.NONE);
		sortChecks += checks - mark;
	}

	private static JsonObject savedSort() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(IndicatorBars.MOD_ID + ".json");
		try {
			return JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("sort");
		} catch (IOException e) {
			throw new AssertionError("Could not read " + path, e);
		}
	}

	/** Each bar's items by bar: slot i holds i + 1. Bar 0 has slot 8 empty, bar 1 slots 7 and 8. */
	private static final String[] BAR_ITEMS = {"stone", "dirt", "oak_planks", "cobblestone"};

	private static String barCell(int bar, int slot) {
		boolean empty = slot == 8 && bar <= 1 || slot == 7 && bar == 1;
		return empty ? "air:0" : BAR_ITEMS[bar] + ":" + (slot + 1);
	}

	/** What must be in the 36 inventory slots with {@code active} in the hotbar: bar 0 in its row, the rest at home. */
	private static String[] barLayout(int active) {
		String[] layout = new String[36];
		for (int slot = 0; slot < 9; slot++) {
			layout[slot] = barCell(active, slot);
			for (int bar = 1; bar < 4; bar++) {
				layout[HotbarSwitcher.rowStart(bar) + slot] = barCell(bar == active ? 0 : bar, slot);
			}
		}
		return layout;
	}

	private static void fillBars(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("clear @a");
		String[] home = barLayout(0);
		for (int i = 0; i < 36; i++) {
			if (!home[i].equals("air:0")) {
				String[] cell = home[i].split(":");
				// Inventory index 9 is the command's inventory.0.
				String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
				server.runCommand("item replace entity @a " + slot + " with minecraft:" + cell[0] + " " + cell[1]);
			}
		}
		context.waitTicks(10);
	}

	private static String[] readInventory(Inventory inventory) {
		String[] items = new String[36];
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inventory.getItem(i);
			items[i] = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ":" + stack.getCount();
		}
		return items;
	}

	private static String[] serverInventory(MinecraftServer minecraftServer) {
		return readInventory(minecraftServer.getPlayerList().getPlayers().get(0).getInventory());
	}

	/** The bar in the hotbar and all the rows, on the client and on the server, and the remembered bar. */
	private static void expectBar(ClientGameTestContext context, TestServerContext server, List<String> failures, String what, int active) {
		context.waitTicks(4);
		String[] want = barLayout(active);
		String[] client = context.computeOnClient(minecraft -> readInventory(minecraft.player.getInventory()));
		String[] onServer = server.computeOnServer(IndicatorBarsClientGameTest::serverInventory);
		int remembered = context.computeOnClient(minecraft -> liveConfig().hotbars.active(HotbarSwitcher.worldKey(minecraft)));
		expect(failures, what + ": client inventory for bar " + active + ", got " + String.join(" ", client), Arrays.equals(want, client));
		expect(failures, what + ": server inventory for bar " + active + ", got " + String.join(" ", onServer), Arrays.equals(want, onServer));
		expect(failures, what + ": remembers bar " + active + ", got " + remembered, remembered == active);
	}

	private static void pressBar(ClientGameTestContext context, TestServerContext server, List<String> failures, String what, KeyMapping key, int active) {
		context.getInput().pressKey(key);
		expectBar(context, server, failures, what, active);
	}

	/** The real config, the one the keys use. */
	private static IBConfig liveConfig() {
		try {
			Field field = ClientHooks.class.getDeclaredField("config");
			field.setAccessible(true);
			return (IBConfig) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Could not reach the config", e);
		}
	}

	/** The hotbars block of the config file, and the sums behind the bars. */
	private static void hotbarConfig(List<String> failures) {
		try {
			Path path = Files.createTempDirectory("indicatorbars-test").resolve("config.json");
			HotbarsConfig fresh = IBConfig.load(path).hotbars;
			expect(failures, "hotbar config: one bar by default", fresh.count == 1 && fresh.showIndicator && fresh.active.isEmpty());
			Files.writeString(path, "{\"hotbars\":{\"count\":0}}");
			expect(failures, "hotbar config: count 0 becomes 1", IBConfig.load(path).hotbars.count == 1);
			Files.writeString(path, "{\"hotbars\":{\"count\":9,\"indicatorAnchor\":\"nowhere\",\"active\":null}}");
			HotbarsConfig high = IBConfig.load(path).hotbars;
			expect(failures, "hotbar config: count 9 becomes 4", high.count == HotbarsConfig.MAX_COUNT);
			expect(failures, "hotbar config: bad values get defaults", high.indicatorAnchor == fresh.indicatorAnchor && high.active != null);
			Files.writeString(path, "{\"hotbars\":{\"active\":{\"a\":2,\"b\":99,\"c\":-1}}}");
			HotbarsConfig saved = IBConfig.load(path).hotbars;
			expect(failures, "hotbar config: a bar is kept per world, out of range is bar 0",
					saved.active("a") == 2 && saved.active("b") == 0 && saved.active("c") == 0 && saved.active("new") == 0);
			Files.writeString(path, "{\"crops\":{}}");
			expect(failures, "hotbar config: an older file gains hotbars", IBConfig.load(path).hotbars != null);
			expect(failures, "hotbar rows: bottom row first", HotbarSwitcher.rowStart(1) == 27 && HotbarSwitcher.rowStart(2) == 18 && HotbarSwitcher.rowStart(3) == 9);
			expect(failures, "hotbar step: wraps both ways", HotbarSwitcher.step(2, 3, 1) == 0 && HotbarSwitcher.step(0, 3, -1) == 2);
			expect(failures, "hotbar step: a bar past a lowered count comes back", HotbarSwitcher.step(3, 1, 1) == 0 && HotbarSwitcher.step(3, 2, -1) == 0);
		} catch (IOException e) {
			throw new AssertionError("Could not test the config", e);
		}
	}

	/** White pixels where the indicator goes, left of the hotbar, at the screenshot's GUI scale of 2. */
	private static int indicatorPixels(Path screenshot) {
		BufferedImage image;
		try {
			image = ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new AssertionError("Could not read " + screenshot, e);
		}
		int count = 0;
		for (int y = 436; y < image.getHeight(); y++) {
			for (int x = 150; x < 244; x++) {
				if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
					count++;
				}
			}
		}
		return count;
	}

	private static void hotbars(ClientGameTestContext context, TestServerContext server, List<String> failures) {
		HotbarsConfig config = context.computeOnClient(minecraft -> liveConfig().hotbars);
		server.runCommand("kill @e[type=!player]");
		context.runOnClient(minecraft -> {
			config.active.clear();
			config.count = 1;
			minecraft.player.getInventory().setSelectedSlot(0);
		});
		server.runCommand("gamemode survival @a");
		fillBars(context, server);

		// One bar: no key moves anything, and no indicator.
		pressBar(context, server, failures, "one bar, cycle", ClientHooks.CYCLE_HOTBAR, 0);
		pressBar(context, server, failures, "one bar, up", ClientHooks.HOTBAR_UP, 0);
		pressBar(context, server, failures, "one bar, down", ClientHooks.HOTBAR_DOWN, 0);
		int none = indicatorPixels(context.takeScreenshot("hotbar-one"));
		expect(failures, "indicator: not with one bar, got " + none, none == 0);

		// The keys, in survival with 3 bars. Bar 3 stays in its row, untouched.
		context.runOnClient(minecraft -> config.count = 3);
		pressBar(context, server, failures, "cycle to 2", ClientHooks.CYCLE_HOTBAR, 1);
		expect(failures, "switching: no message, the indicator says it", !Language.getInstance().has("indicatorbars.hotbars.active"));
		int second = indicatorPixels(context.takeScreenshot("hotbar-2-of-3"));
		expect(failures, "indicator: shows with 3 bars, got " + second, second > 0);
		pressBar(context, server, failures, "cycle to 3", ClientHooks.CYCLE_HOTBAR, 2);
		int third = indicatorPixels(context.takeScreenshot("hotbar-3-of-3"));
		expect(failures, "indicator: 3/3 isn't drawn as 2/3, got " + third + " and " + second, third != second);
		pressBar(context, server, failures, "cycle wraps to 1", ClientHooks.CYCLE_HOTBAR, 0);
		pressBar(context, server, failures, "up to 2", ClientHooks.HOTBAR_UP, 1);
		pressBar(context, server, failures, "up to 3", ClientHooks.HOTBAR_UP, 2);
		pressBar(context, server, failures, "up wraps to 1", ClientHooks.HOTBAR_UP, 0);
		pressBar(context, server, failures, "down wraps to 3", ClientHooks.HOTBAR_DOWN, 2);
		pressBar(context, server, failures, "down to 2", ClientHooks.HOTBAR_DOWN, 1);
		pressBar(context, server, failures, "down to 1", ClientHooks.HOTBAR_DOWN, 0);

		// Slots empty on both sides send nothing: bar 1 is empty in slots 7 and 8, bar 0 in slot 8.
		int out = context.computeOnClient(minecraft -> HotbarSwitcher.swapRow(minecraft, 1));
		int back = context.computeOnClient(minecraft -> HotbarSwitcher.swapRow(minecraft, 1));
		expect(failures, "swaps: both-empty slots skipped, got " + out + " and " + back, out == 8 && back == 8);
		expectBar(context, server, failures, "two swaps cancel out", 0);

		// Several switches in one tick land in the right place.
		context.runOnClient(minecraft -> {
			HotbarSwitcher.change(minecraft, liveConfig(), 1);
			HotbarSwitcher.change(minecraft, liveConfig(), 1);
		});
		expectBar(context, server, failures, "two switches in one tick", 2);
		context.runOnClient(minecraft -> HotbarSwitcher.change(minecraft, liveConfig(), 1));
		expectBar(context, server, failures, "and back", 0);

		// The held slot doesn't move, whichever bar comes in.
		context.runOnClient(minecraft -> minecraft.player.getInventory().setSelectedSlot(4));
		pressBar(context, server, failures, "selected slot 4, to bar 2", ClientHooks.CYCLE_HOTBAR, 1);
		int selected = context.computeOnClient(minecraft -> minecraft.player.getInventory().getSelectedSlot());
		String held = context.computeOnClient(minecraft -> BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()).getPath());
		expect(failures, "selected slot kept, holding bar 2's item, got " + selected + " " + held, selected == 4 && held.equals("dirt"));
		pressBar(context, server, failures, "selected slot 4, to bar 3", ClientHooks.CYCLE_HOTBAR, 2);
		pressBar(context, server, failures, "selected slot 4, home", ClientHooks.CYCLE_HOTBAR, 0);
		context.runOnClient(minecraft -> minecraft.player.getInventory().setSelectedSlot(0));

		// Creative, all 4 bars.
		server.runCommand("gamemode creative @a");
		context.runOnClient(minecraft -> config.count = 4);
		context.waitTicks(5);
		pressBar(context, server, failures, "creative: cycle to 2", ClientHooks.CYCLE_HOTBAR, 1);
		pressBar(context, server, failures, "creative: cycle to 3", ClientHooks.CYCLE_HOTBAR, 2);
		pressBar(context, server, failures, "creative: cycle to 4", ClientHooks.CYCLE_HOTBAR, 3);
		pressBar(context, server, failures, "creative: cycle wraps to 1", ClientHooks.CYCLE_HOTBAR, 0);
		pressBar(context, server, failures, "creative: down wraps to 4", ClientHooks.HOTBAR_DOWN, 3);
		pressBar(context, server, failures, "creative: up wraps to 1", ClientHooks.HOTBAR_UP, 0);
		server.runCommand("gamemode survival @a");

		// Lowering the count with a bar out: the next press brings the hotbar home.
		context.runOnClient(minecraft -> config.count = 3);
		pressBar(context, server, failures, "before lowering", ClientHooks.HOTBAR_DOWN, 2);
		context.runOnClient(minecraft -> config.count = 1);
		pressBar(context, server, failures, "count lowered to 1, a press goes home", ClientHooks.CYCLE_HOTBAR, 0);
		context.runOnClient(minecraft -> config.count = 3);

		// Nothing moves while a chest is open, by key or by a direct call.
		BlockPos chest = context.computeOnClient(minecraft -> minecraft.player.blockPosition()).offset(0, 0, 2);
		server.runCommand(place(chest, "@ chest"));
		server.runCommand(place(chest, "@0,1,0 air"));
		context.waitTicks(5);
		context.runOnClient(minecraft -> minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false)));
		context.waitFor(minecraft -> minecraft.player.containerMenu != minecraft.player.inventoryMenu);
		pressBar(context, server, failures, "chest open, key", ClientHooks.CYCLE_HOTBAR, 0);
		context.runOnClient(minecraft -> HotbarSwitcher.change(minecraft, liveConfig(), 1));
		expectBar(context, server, failures, "chest open, direct call", 0);
		context.runOnClient(minecraft -> minecraft.player.closeContainer());
		context.waitFor(minecraft -> minecraft.player.containerMenu == minecraft.player.inventoryMenu);
		server.runCommand(place(chest, "@ air"));
		pressBar(context, server, failures, "chest closed, the key works again", ClientHooks.CYCLE_HOTBAR, 1);

		// Indicator off.
		context.runOnClient(minecraft -> config.showIndicator = false);
		context.waitTicks(2);
		int off = indicatorPixels(context.takeScreenshot("hotbar-indicator-off"));
		expect(failures, "indicator: off in the config, got " + off, off == 0);
		context.runOnClient(minecraft -> config.showIndicator = true);

		pressBar(context, server, failures, "leaving on bar 3", ClientHooks.CYCLE_HOTBAR, 2);
		expect(failures, "bar saved to the file", savedActiveBars() == 1);
	}

	/** Left on bar 3: it must still know that, and each bar must go back where it came from. */
	private static void hotbarsAfterRejoin(ClientGameTestContext context, TestWorldSave save, List<String> failures) {
		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			TestServerContext server = world.getServer();
			expectBar(context, server, failures, "rejoin: still bar 3", 2);
			pressBar(context, server, failures, "rejoin: cycle home", ClientHooks.CYCLE_HOTBAR, 0);
			pressBar(context, server, failures, "rejoin: up to 2", ClientHooks.HOTBAR_UP, 1);
			pressBar(context, server, failures, "rejoin: back home", ClientHooks.HOTBAR_DOWN, 0);
		}
	}

	/** Over a real connection to a real server, as a plain survival player. */
	private static void hotbarsOnDedicatedServer(ClientGameTestContext context, List<String> failures, List<String> sortFailures, List<String> craftFailures) {
		HotbarsConfig config = context.computeOnClient(minecraft -> liveConfig().hotbars);
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				var connection = server.connect()) {
			connection.getClientLevel().waitForChunksRender();
			server.runCommand("gamemode survival @a");
			fillBars(context, server);
			context.runOnClient(minecraft -> {
				config.count = 3;
				minecraft.player.getInventory().setSelectedSlot(0);
			});
			String key = context.computeOnClient(HotbarSwitcher::worldKey);
			expect(failures, "server: its own remembered bar, got " + key, key.startsWith("server:") && config.active(key) == 0);
			pressBar(context, server, failures, "server: cycle to 2", ClientHooks.CYCLE_HOTBAR, 1);
			pressBar(context, server, failures, "server: up to 3", ClientHooks.HOTBAR_UP, 2);
			pressBar(context, server, failures, "server: cycle wraps to 1", ClientHooks.CYCLE_HOTBAR, 0);
			pressBar(context, server, failures, "server: down wraps to 3", ClientHooks.HOTBAR_DOWN, 2);
			pressBar(context, server, failures, "server: down to 2", ClientHooks.HOTBAR_DOWN, 1);
			pressBar(context, server, failures, "server: down home", ClientHooks.HOTBAR_DOWN, 0);
			sortOnDedicatedServer(context, server, sortFailures);
			craftOnDedicatedServer(context, server, craftFailures);
		}
		context.runOnClient(minecraft -> {
			config.count = 1;
			config.active.clear();
			liveConfig().save();
		});
	}

	private static int savedActiveBars() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(IndicatorBars.MOD_ID + ".json");
		try {
			return JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("hotbars").getAsJsonObject("active").size();
		} catch (IOException e) {
			throw new AssertionError("Could not read " + path, e);
		}
	}

	/** Points the crosshair {@code height} up the block {@code dx, dz} from the player's feet. */
	private static void lookAtBlock(Minecraft minecraft, int dx, int dz, double height) {
		Vec3 target = Vec3.atBottomCenterOf(minecraft.player.blockPosition().offset(dx, 0, dz)).add(0, height, 0);
		minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
	}

	/** Worn and held items at different wear: the durability column, slot counters, the inventory. */
	private static void durability(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("kill @e[type=!player]");
		String[][] items = {
				{"armor.head", "diamond_helmet", "300"},
				// 10 of 240 left: at the warning line, so red.
				{"armor.chest", "iron_chestplate", "230"},
				// Full: no counter, not in the column.
				{"armor.legs", "golden_leggings", "0"},
				{"armor.feet", "leather_boots", "20"},
				{"weapon.mainhand", "diamond_sword", "800"},
				{"weapon.offhand", "shield", "100"},
				{"hotbar.1", "diamond_pickaxe", "1500"},
				{"hotbar.2", "bow", "0"},
				// Four digits: 2000 left.
				{"hotbar.3", "netherite_axe", "31"},
				{"hotbar.4", "elytra", "400"},
		};
		for (String[] item : items) {
			server.runCommand("item replace entity @p " + item[0] + " with minecraft:" + item[1] + "[minecraft:damage=" + item[2] + "]");
		}
		context.waitTicks(10);
		context.takeScreenshot("durability-hud");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("durability-inventory");
		context.setScreen(() -> null);

		// J hides every durability number, and brings them back.
		context.getInput().pressKey(ClientHooks.TOGGLE_DURABILITY);
		context.waitTicks(5);
		context.takeScreenshot("durability-hidden");
		context.getInput().pressKey(ClientHooks.TOGGLE_DURABILITY);
	}

	/** The reference image: a herd of cows, the one straight ahead hurt to 6/10. */
	private static void cows(ClientGameTestContext context, TestServerContext server) {
		summon(server, "cow", "~ ~ ~5", "Health:6f");
		summon(server, "cow", "~3 ~ ~8", "");
		summon(server, "cow", "~-4 ~ ~10", "Age:-24000");
		summon(server, "cow", "~-2 ~ ~4", "CustomName:\"Daisy\",Health:3f");
		summon(server, "cow", "~6 ~ ~18", "Health:9f");
		summon(server, "cow", "~-8 ~ ~22", "");
		context.waitTicks(20);
		context.takeScreenshot("cows");

		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("cows-third-person");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
	}

	/** Points the crosshair at the middle of the one mob in the world. */
	private static void lookAtMob(Minecraft minecraft) {
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof Mob) {
				minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getBoundingBox().getCenter());
				return;
			}
		}
	}

	private static void summon(TestServerContext server, String type, String pos, String extraNbt) {
		String nbt = "NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]" + (extraNbt.isEmpty() ? "" : "," + extraNbt);
		server.runCommand("execute as @p at @s run summon " + type + " " + pos + " {" + nbt + "}");
	}
}
