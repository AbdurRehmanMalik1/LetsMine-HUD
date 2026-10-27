package net.firstwemine1.indicatorbars;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class IndicatorBars {
	public static final String MOD_ID = "indicatorbars";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private IndicatorBars() {
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
