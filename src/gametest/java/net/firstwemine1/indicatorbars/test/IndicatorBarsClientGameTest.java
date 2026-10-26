package net.firstwemine1.indicatorbars.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/**
 * Rebuilds the reference image: a herd of cows in a flat world, the one straight ahead hurt to 6/10.
 * Screenshots go to build/run/clientGameTest/screenshots; look at them, nothing is compared.
 */
public class IndicatorBarsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			// Face south, looking slightly down at the targeted cow's body.
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");
			context.waitTicks(5);

			summon(server, "~ ~ ~5", "Health:6f");
			summon(server, "~3 ~ ~8", "");
			summon(server, "~-4 ~ ~10", "Age:-24000");
			summon(server, "~-2 ~ ~4", "CustomName:\"Daisy\",Health:3f");
			summon(server, "~6 ~ ~18", "Health:9f");
			summon(server, "~-8 ~ ~22", "");
			context.waitTicks(20);
			context.takeScreenshot("indicator-bars-cows");

			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("indicator-bars-cows-third-person");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	private static void summon(TestServerContext server, String pos, String extraNbt) {
		String nbt = "NoAI:1b,Rotation:[90f,0f]" + (extraNbt.isEmpty() ? "" : "," + extraNbt);
		server.runCommand("execute as @p at @s run summon cow " + pos + " {" + nbt + "}");
	}
}
