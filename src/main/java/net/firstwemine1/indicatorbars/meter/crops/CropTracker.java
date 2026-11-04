package net.firstwemine1.indicatorbars.meter.crops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Crops have no entity or block entity to hang a bar on, so the blocks around you are scanned a few
 * times a second and the bars drawn from that list. The crop under the crosshair is read every tick.
 */
public class CropTracker {
	private static final int SCAN_TICKS = 5;
	/** Blocks above and below you that are scanned. */
	private static final int SCAN_HEIGHT = 6;

	private List<Crop> crops = Collections.emptyList();
	@Nullable
	private Crop target;
	private int ticksToScan;

	/** The crops that get a bar, nearest first. */
	public List<Crop> crops() {
		return crops;
	}

	/** The crop under the crosshair, for the panel. */
	@Nullable
	public Crop target() {
		return target;
	}

	public void tick(Minecraft minecraft, CropsConfig config) {
		Entity camera = minecraft.getCameraEntity();
		Level level = minecraft.level;
		if (camera == null || level == null) {
			reset();
			return;
		}
		target = pick(camera, level, config);
		if (!config.showBars) {
			crops = Collections.emptyList();
			return;
		}
		if (config.barMode == CropsConfig.BarMode.TARGET) {
			crops = target != null ? Collections.singletonList(target) : Collections.<Crop>emptyList();
			return;
		}
		if (--ticksToScan > 0) {
			return;
		}
		ticksToScan = SCAN_TICKS;
		crops = scan(camera, level, config);
	}

	public void reset() {
		crops = Collections.emptyList();
		target = null;
		ticksToScan = 0;
	}

	private static List<Crop> scan(Entity camera, Level level, CropsConfig config) {
		int radius = config.barDistance;
		double maxDistance = (double) radius * radius;
		BlockPos centre = camera.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<Crop> found = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > maxDistance) {
					continue;
				}
				for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
					pos.setWithOffset(centre, dx, dy, dz);
					Crop crop = CropGrowth.read(level, pos, level.getBlockState(pos));
					if (crop != null && (config.barMode == CropsConfig.BarMode.ALL || !crop.ripe() || crop.stem())) {
						found.add(crop);
					}
				}
			}
		}
		if (found.size() > config.maxBars) {
			found.sort(Comparator.comparingDouble(crop -> crop.pos().distSqr(camera.position(), true)));
			found = found.subList(0, Math.max(config.maxBars, 0));
		}
		return found;
	}

	@Nullable
	private static Crop pick(Entity camera, Level level, CropsConfig config) {
		HitResult hit = camera.pick(config.targetRange, 1, false);
		if (!(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		BlockState state = level.getBlockState(pos);
		return CropGrowth.read(level, pos, state);
	}
}
