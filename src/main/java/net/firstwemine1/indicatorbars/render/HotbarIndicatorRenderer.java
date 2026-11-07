package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.firstwemine1.indicatorbars.config.IBConfig.HotbarsConfig;
import net.firstwemine1.indicatorbars.hotbar.HotbarSwitcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.HandSide;
import net.minecraft.entity.player.PlayerEntity;

/** Which hotbar is in use, as {@code 2/3} beside the hotbar. Only with more than one. */
public final class HotbarIndicatorRenderer {
	private static final int WHITE = 0xFFFFFFFF;

	private HotbarIndicatorRenderer() {
	}

	public static void render(MatrixStack poseStack, HotbarsConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		PlayerEntity player = minecraft.player;
		if (!config.showIndicator || config.count <= 1 || player == null || player.isSpectator()) {
			return;
		}
		FontRenderer font = minecraft.font;
		String text = (config.active(HotbarSwitcher.worldKey(minecraft)) + 1) + "/" + config.count;
		boolean offhand = !player.getOffhandItem().isEmpty();
		boolean rightHanded = player.getMainArm() == HandSide.RIGHT;
		int[] corner = config.indicatorAnchor.place(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), font.width(text), font.lineHeight,
				config.indicatorX, config.indicatorY, offhand && rightHanded, offhand && !rightHanded);
		font.drawShadow(poseStack, text, corner[0], corner[1], WHITE);
	}
}
