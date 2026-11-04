package net.firstwemine1.indicatorbars.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Problem;
import net.firstwemine1.indicatorbars.meter.crops.CropTracker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Test scenes in a flat world, screenshotted to build/run/clientGameTest/screenshots. The mob and
 * durability scenes are for looking at; the crop meter is asserted, and any failure fails the run.
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

	/** How many crop checks ran, for the log. */
	private static int checks;

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> failures = new ArrayList<>();
		cropConfig(failures);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			// Not peaceful, which would remove the ghast and the slime.
			server.runCommand("difficulty easy");
			// Face south, looking slightly down at the targeted mob's body.
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");
			context.waitTicks(5);

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
		IndicatorBars.LOGGER.info("[crop-test] {} checks, {} failed", checks, failures.size());
		if (!failures.isEmpty()) {
			throw new AssertionError("Crops:\n  " + String.join("\n  ", failures));
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
	 * fill, the dotted line, K, and which panel wins. Colours are matched within a tolerance: the world
	 * bars and the HUD text are blended a little. Nothing else in these scenes comes close.
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
