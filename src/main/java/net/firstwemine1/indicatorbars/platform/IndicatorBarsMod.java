package net.firstwemine1.indicatorbars.platform;

import net.firstwemine1.indicatorbars.IndicatorBars;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge entrypoint. Forge constructs it on servers too, so every client class stays behind the
 * dist check.
 */
@Mod(IndicatorBars.MOD_ID)
public final class IndicatorBarsMod {
	public IndicatorBarsMod() {
		// Client-only: servers don't need it, and clients can join servers without it (1.16 has no
		// displayTest key in mods.toml).
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
				() -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
		if (FMLEnvironment.dist == Dist.CLIENT) {
			IndicatorBarsClient.init(FMLJavaModLoadingContext.get().getModEventBus());
		}
	}
}
