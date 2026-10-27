package net.firstwemine1.indicatorbars.meter.health;

import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Whether a mob gets a floating bar this frame. Checked before anything is drawn. */
public final class BarVisibility {
	private BarVisibility() {
	}

	public static boolean shouldShow(LivingEntity entity, Minecraft minecraft, HealthConfig config, @Nullable LivingEntity target) {
		Player player = minecraft.player;
		Entity camera = minecraft.getCameraEntity();
		if (!config.showBars || player == null || camera == null) {
			return false;
		}
		// A bar above your own head (third person) is a later option.
		if (entity == player || !entity.isAlive() || entity.isInvisibleTo(player)) {
			return false;
		}
		// Sneaking players hide their bar, as vanilla hides their name tag.
		if (entity instanceof Player && (!config.showForPlayers || entity.isDiscrete())) {
			return false;
		}
		if (config.isBlacklisted(entity.getType())
				|| entity.distanceToSqr(camera) > config.barDistance * config.barDistance) {
			return false;
		}
		switch (config.barMode) {
			case DAMAGED:
				return entity.getHealth() < entity.getMaxHealth();
			case TARGET:
				return entity == target;
			default:
				return true;
		}
	}
}
