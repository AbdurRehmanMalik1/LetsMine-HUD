package net.firstwemine1.indicatorbars.meter.crops;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A crop's growth stage, and what is holding it back. Everything is read from block states and light
 * the client already has; the rules are vanilla's own random-tick checks (26.3), so "optimal" means
 * what the server actually rewards.
 */
public final class CropGrowth {
	/** Crops, stems and berry bushes only grow at this light or brighter. */
	private static final int LIGHT_TO_GROW = 9;

	public enum Problem {
		/** Growth stopped. */
		TOO_DARK(true),
		/** Not on moist farmland: grows a lot slower. */
		DRY(true),
		/** A fully grown stem with nowhere to put its pumpkin or melon. */
		NO_FRUIT_ROOM(true),
		/** The same crop on both axes, or on a diagonal: half speed. */
		CROWDED(false);

		/** Whether it warns on the bar by default; {@link #CROWDED} is up to {@link CropsConfig#warnCrowded}. */
		private final boolean warns;

		Problem(boolean warns) {
			this.warns = warns;
		}

		public boolean warns(CropsConfig config) {
			return warns || config.warnCrowded;
		}

		public String translationKey() {
			return "indicatorbars.crops." + name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * One crop. {@code top} is where its plant ends, in blocks above {@code pos}; {@code light} and
	 * {@code lightNeeded} are 0 for crops that don't care.
	 */
	public record Crop(BlockPos pos, BlockState state, int age, int maxAge, float top, int light, int lightNeeded,
			Set<Problem> problems) {
		public boolean ripe() {
			return age >= maxAge;
		}

		/** A stem at full age still has its fruit to grow, so it keeps its problems. */
		public boolean stem() {
			return state.getBlock() instanceof StemBlock;
		}

		public float fraction() {
			return MeterStyle.fraction(age, maxAge);
		}

		/** Yellow while growing, green once there is something to harvest. */
		public int color() {
			return ripe() ? MeterStyle.GREEN : MeterStyle.YELLOW;
		}

		/** The bar gets the red dotted line. */
		public boolean warns(CropsConfig config) {
			for (Problem problem : problems) {
				if (problem.warns(config)) {
					return true;
				}
			}
			return false;
		}
	}

	private CropGrowth() {
	}

	/** The crop at {@code pos}, or null if it isn't one. */
	public static @Nullable Crop read(Level level, BlockPos pos, BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return farmed(level, pos.immutable(), state, state.getValue(crop.getAgeProperty()), crop.getMaxAge(), top(level, pos, state), LIGHT_TO_GROW);
		}
		if (block instanceof StemBlock) {
			Crop stem = farmed(level, pos.immutable(), state, state.getValue(StemBlock.AGE), StemBlock.MAX_AGE,
					top(level, pos, state), LIGHT_TO_GROW);
			if (stem.ripe() && !fruitRoom(level, stem.pos)) {
				stem.problems.add(Problem.NO_FRUIT_ROOM);
			}
			return stem;
		}
		if (block instanceof SweetBerryBushBlock) {
			int age = state.getValue(SweetBerryBushBlock.AGE);
			BlockPos at = pos.immutable();
			// The bush checks the light above itself, not in its own block.
			int light = level.getRawBrightness(at.above(), 0);
			Set<Problem> problems = EnumSet.noneOf(Problem.class);
			if (age < SweetBerryBushBlock.MAX_AGE && light < LIGHT_TO_GROW) {
				problems.add(Problem.TOO_DARK);
			}
			return new Crop(at, state, age, SweetBerryBushBlock.MAX_AGE, top(level, pos, state), light, LIGHT_TO_GROW, problems);
		}
		// Nether wart and cocoa grow anywhere they can stand, at a fixed rate.
		if (block instanceof NetherWartBlock) {
			return new Crop(pos.immutable(), state, state.getValue(NetherWartBlock.AGE), NetherWartBlock.MAX_AGE,
					top(level, pos, state), 0, 0, EnumSet.noneOf(Problem.class));
		}
		if (block instanceof CocoaBlock) {
			return new Crop(pos.immutable(), state, state.getValue(CocoaBlock.AGE), CocoaBlock.MAX_AGE,
					top(level, pos, state), 0, 0, EnumSet.noneOf(Problem.class));
		}
		return null;
	}

	/** The top of the plant's outline, so the bar sits on the plant and rises as it grows. */
	private static float top(Level level, BlockPos pos, BlockState state) {
		VoxelShape shape = state.getShape(level, pos);
		return shape.isEmpty() ? 1 : (float) shape.max(Direction.Axis.Y);
	}

	/** Crops on farmland: light, water and crowding, as {@code CropBlock.getGrowthSpeed} weighs them. */
	private static Crop farmed(Level level, BlockPos pos, BlockState state, int age, int maxAge, float top, int lightNeeded) {
		int light = level.getRawBrightness(pos, 0);
		Set<Problem> problems = EnumSet.noneOf(Problem.class);
		Crop crop = new Crop(pos, state, age, maxAge, top, light, lightNeeded, problems);
		// A ripe crop is done; a ripe stem still has its fruit to grow.
		if (crop.ripe() && !crop.stem()) {
			return crop;
		}
		if (light < lightNeeded) {
			problems.add(Problem.TOO_DARK);
		}
		// Moist farmland counts three times dry farmland, the block under the crop most of all.
		BlockState soil = level.getBlockState(pos.below());
		if (soil.is(Blocks.FARMLAND) && soil.getValue(FarmBlock.MOISTURE) == 0) {
			problems.add(Problem.DRY);
		}
		if (crowded(level, pos, state.getBlock())) {
			problems.add(Problem.CROWDED);
		}
		return crop;
	}

	/** Vanilla halves the speed for the same crop on both axes, or on any diagonal. */
	private static boolean crowded(Level level, BlockPos pos, Block type) {
		boolean eastWest = level.getBlockState(pos.west()).is(type) || level.getBlockState(pos.east()).is(type);
		boolean northSouth = level.getBlockState(pos.north()).is(type) || level.getBlockState(pos.south()).is(type);
		if (eastWest && northSouth) {
			return true;
		}
		return level.getBlockState(pos.north().west()).is(type) || level.getBlockState(pos.north().east()).is(type)
				|| level.getBlockState(pos.south().east()).is(type) || level.getBlockState(pos.south().west()).is(type);
	}

	/** A stem places its fruit on an empty side, over farmland or dirt. */
	private static boolean fruitRoom(Level level, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos side = pos.relative(direction);
			BlockState below = level.getBlockState(side.below());
			if (level.getBlockState(side).isAir() && (below.is(Blocks.FARMLAND) || below.is(BlockTags.DIRT))) {
				return true;
			}
		}
		return false;
	}
}
