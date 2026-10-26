package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.IndicatorBars;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint. Forge constructs it on servers too, so every client class stays behind the
 * dist check.
 */
@Mod(IndicatorBars.MOD_ID)
public final class IndicatorBarsMod {
	public IndicatorBarsMod() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			IndicatorBarsClient.init(FMLJavaModLoadingContext.get().getModEventBus());
		}
	}
}
