package net.firstwemine1.indicatorbars.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Test scenes in a flat world, screenshotted to build/run/clientGameTest/screenshots. Look at them,
 * nothing is compared.
 */
public class IndicatorBarsClientGameTest implements FabricClientGameTest {
	/** One mob at a time straight ahead: the portrait must fit each in its box. */
	private static final String[][] PORTRAITS = {
			{"pig", "~ ~ ~4", ""},
			{"pig-baby", "~ ~ ~4", "Age:-24000"},
			{"cow", "~ ~ ~4", ""},
			{"chicken", "~ ~ ~3", ""},
			{"horse", "~ ~ ~5", ""},
			{"sheep", "~ ~ ~4", ""},
			{"spider", "~ ~ ~4", ""},
			{"wolf", "~ ~ ~4", ""},
			{"ghast", "~ ~ ~12", ""},
			{"slime", "~ ~ ~5", "Size:3"},
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			// Not peaceful, which would remove the ghast and the slime.
			server.runCommand("difficulty easy");
			// Face south, looking slightly down at the targeted mob's body.
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");
			context.waitTicks(5);

			cows(context, server);
			for (String[] portrait : PORTRAITS) {
				server.runCommand("kill @e[type=!player]");
				// Let the last mob's death animation finish, or it stays in the panel.
				context.waitTicks(25);
				String type = portrait[0].replace("-baby", "");
				summon(server, type, portrait[1], portrait[2]);
				context.waitTicks(5);
				context.runOnClient(IndicatorBarsClientGameTest::lookAtMob);
				context.waitTicks(10);
				context.takeScreenshot("portrait-" + portrait[0]);
			}
		}
	}

	/** The reference image: a herd of cows, the one straight ahead hurt to 6/10. */
	private static void cows(ClientGameTestContext context, TestServerContext server) {
		summon(server, "cow", "~ ~ ~5", "Health:6f");
		summon(server, "cow", "~3 ~ ~8", "");
		summon(server, "cow", "~-4 ~ ~10", "Age:-24000");
		summon(server, "cow", "~-2 ~ ~4", "CustomName:\"Daisy\",Health:3f");
		summon(server, "cow", "~6 ~ ~18", "Health:9f");
		summon(server, "cow", "~-8 ~ ~22", "");
		context.waitTicks(20);
		context.takeScreenshot("cows");

		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("cows-third-person");
		context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
	}

	/** Points the crosshair at the middle of the one mob in the world. */
	private static void lookAtMob(Minecraft minecraft) {
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof Mob) {
				minecraft.player.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getBoundingBox().getCenter());
				return;
			}
		}
	}

	private static void summon(TestServerContext server, String type, String pos, String extraNbt) {
		String nbt = "NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]" + (extraNbt.isEmpty() ? "" : "," + extraNbt);
		server.runCommand("execute as @p at @s run summon " + type + " " + pos + " {" + nbt + "}");
	}
}
