package net.firstwemine1.indicatorbars.render;

import net.firstwemine1.indicatorbars.config.IBConfig.HotbarsConfig;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Which hotbar is in use, as {@code 2/3} beside the hotbar. Only with more than one. */
public final class HotbarIndicatorRenderer {
	private static final int WHITE = 0xFFFFFFFF;

	private HotbarIndicatorRenderer() {
	}

	public static void render(GuiGraphics graphics, HotbarsConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (!config.showIndicator || config.count <= 1 || player == null || player.isSpectator()) {
			return;
		}
		Font font = minecraft.font;
		String text = (config.active(HotbarSwitcher.worldKey(minecraft)) + 1) + "/" + config.count;
		boolean offhand = !player.getOffhandItem().isEmpty();
		boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
		int[] corner = config.indicatorAnchor.place(graphics.guiWidth(), graphics.guiHeight(), font.width(text), font.lineHeight,
				config.indicatorX, config.indicatorY, offhand && rightHanded, offhand && !rightHanded);
		graphics.drawString(font, text, corner[0], corner[1], WHITE, true);
	}
}
