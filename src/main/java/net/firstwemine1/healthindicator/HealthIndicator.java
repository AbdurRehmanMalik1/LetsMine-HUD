package net.firstwemine1.healthindicator;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HealthIndicator {
	public static final String MOD_ID = "healthindicator";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private HealthIndicator() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
