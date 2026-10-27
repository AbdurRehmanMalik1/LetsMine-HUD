package net.firstwemine1.indicatorbars;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IndicatorBars {
	public static final String MOD_ID = "indicatorbars";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private IndicatorBars() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
