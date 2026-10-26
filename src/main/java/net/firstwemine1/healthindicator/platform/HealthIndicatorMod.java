package net.firstwemine1.healthindicator.platform;

import net.firstwemine1.healthindicator.HealthIndicator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint. Forge constructs it on servers too, so every client class stays behind the
 * dist check.
 */
@Mod(HealthIndicator.MOD_ID)
public final class HealthIndicatorMod {
	public HealthIndicatorMod() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			HealthIndicatorClient.init(FMLJavaModLoadingContext.get().getModEventBus());
		}
	}
}
