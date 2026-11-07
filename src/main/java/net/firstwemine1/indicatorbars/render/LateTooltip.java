package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Slots draw after widgets, so a widget's tooltip waits for the end of the frame to land on top of them. */
public final class LateTooltip {
	private static List<Component> lines;

	private LateTooltip() {
	}

	static void set(List<Component> tooltip) {
		lines = tooltip;
	}

	public static void draw(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
		if (lines != null) {
			screen.renderComponentTooltip(poseStack, lines, mouseX, mouseY);
			lines = null;
		}
	}
}
