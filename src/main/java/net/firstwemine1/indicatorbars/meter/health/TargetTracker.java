package net.firstwemine1.indicatorbars.meter.health;

import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
		return Mth.clamp(1 - fading / FADE_TICKS, 0, 1);
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
		Player player = minecraft.player;
		if (camera == null || player == null || minecraft.level == null) {
			return null;
		}
		if (minecraft.hitResult instanceof EntityHitResult) {
			Entity hit = ((EntityHitResult) minecraft.hitResult).getEntity();
			if (hit instanceof LivingEntity && targetable((LivingEntity) hit, player, config)) {
				return (LivingEntity) hit;
			}
		}

		// Past vanilla's reach: stop at the first block in the way.
		double range = config.targetRange;
		Vec3 eye = camera.getEyePosition(1);
		Vec3 look = camera.getViewVector(1);
		HitResult block = camera.pick(range, 1, false);
		if (block.getType() != HitResult.Type.MISS) {
			range = eye.distanceTo(block.getLocation());
		}
		Vec3 end = eye.add(look.scale(range));
		AABB searchArea = camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eye, end, searchArea,
				entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable()
						&& targetable((LivingEntity) entity, player, config),
				range * range);
		return hit != null ? (LivingEntity) hit.getEntity() : null;
	}

	private static boolean targetable(LivingEntity entity, Player player, HealthConfig config) {
		return entity != player && !entity.isInvisibleTo(player) && !config.isBlacklisted(entity.getType())
				&& (config.showForPlayers || !(entity instanceof Player));
	}
}
