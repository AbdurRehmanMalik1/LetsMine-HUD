package net.firstwemine1.indicatorbars.meter.health;

import javax.annotation.Nullable;

import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

/**
 * The mob shown in the target panel: whatever the crosshair is on, out to {@link HealthConfig#targetRange}
 * (vanilla's own pick only reaches about 3 blocks). Kept for {@link HealthConfig#panelLingerSeconds} after
 * you look away, then faded out.
 */
public class TargetTracker {
	private static final int FADE_TICKS = 5;

	@Nullable
	private LivingEntity target;
	/** Ticks since the crosshair was last on the target. */
	private int ticksAway;

	@Nullable
	public LivingEntity target() {
		return target;
	}

	/** 1 while the panel is fully shown, falling to 0 as it fades out. */
	public float alpha(HealthConfig config, float partialTick) {
		float fading = ticksAway + partialTick - lingerTicks(config);
		return MathHelper.clamp(1 - fading / FADE_TICKS, 0, 1);
	}

	public void tick(Minecraft minecraft, HealthConfig config) {
		LivingEntity seen = pick(minecraft, config);
		if (seen != null) {
			target = seen;
			ticksAway = 0;
		} else if (target != null) {
			ticksAway++;
			// A dying mob stays on the panel at 0 health until its body disappears.
			if (ticksAway > lingerTicks(config) + FADE_TICKS || target.removed || target.level != minecraft.level) {
				reset();
			}
		}
	}

	public void reset() {
		target = null;
		ticksAway = 0;
	}

	private static int lingerTicks(HealthConfig config) {
		return (int) Math.round(config.panelLingerSeconds * 20);
	}

	@Nullable
	private static LivingEntity pick(Minecraft minecraft, HealthConfig config) {
		Entity camera = minecraft.getCameraEntity();
		PlayerEntity player = minecraft.player;
		if (camera == null || player == null || minecraft.level == null) {
			return null;
		}
		if (minecraft.hitResult instanceof EntityRayTraceResult) {
			Entity hit = ((EntityRayTraceResult) minecraft.hitResult).getEntity();
			if (hit instanceof LivingEntity && targetable((LivingEntity) hit, player, config)) {
				return (LivingEntity) hit;
			}
		}

		// Past vanilla's reach: stop at the first block in the way.
		double range = config.targetRange;
		Vector3d eye = camera.getEyePosition(1);
		Vector3d look = camera.getViewVector(1);
		RayTraceResult block = camera.pick(range, 1, false);
		if (block.getType() != RayTraceResult.Type.MISS) {
			range = eye.distanceTo(block.getLocation());
		}
		Vector3d end = eye.add(look.scale(range));
		AxisAlignedBB searchArea = camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
		EntityRayTraceResult hit = ProjectileHelper.getEntityHitResult(camera, eye, end, searchArea,
				entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable()
						&& targetable((LivingEntity) entity, player, config),
				range * range);
		return hit != null ? (LivingEntity) hit.getEntity() : null;
	}

	private static boolean targetable(LivingEntity entity, PlayerEntity player, HealthConfig config) {
		return entity != player && !entity.isInvisibleTo(player) && !config.isBlacklisted(entity.getType())
				&& (config.showForPlayers || !(entity instanceof PlayerEntity));
	}
}
