package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

/** Slots draw after widgets, so a widget's tooltip waits for the end of the frame to land on top of them. */
public final class LateTooltip {
	private static List<ITextComponent> lines;

	private LateTooltip() {
	}

	static void set(List<ITextComponent> tooltip) {
		lines = tooltip;
	}

	public static void draw(Screen screen, MatrixStack poseStack, int mouseX, int mouseY) {
		if (lines != null) {
			screen.renderComponentTooltip(poseStack, lines, mouseX, mouseY);
			lines = null;
		}
	}
}
