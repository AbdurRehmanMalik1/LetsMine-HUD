package net.firstwemine1.indicatorbars.render;

import java.util.Locale;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The HUD panel for the targeted mob: a live portrait, its name, a heart with its health, and a bar.
 *
 * <pre>
 * +-----------------------------+
 * | +------+ Cow                |
 * | |portr.| (heart) 6/10       |
 * | +------+ [======----]       |
 * +-----------------------------+
 * </pre>
 */
public final class TargetPanelRenderer {
	/** Vanilla's heart, so resource packs re-skin it. Before 1.20.2 it is a region of icons.png. */
	private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
	private static final int HEART_U = 52;
	private static final int HEART_V = 0;

	private static final int PADDING = 4;
	private static final int PORTRAIT = 36;
	private static final int MIN_TEXT_WIDTH = 64;
	private static final int HEART_SIZE = 9;
	private static final int BAR_HEIGHT = 5;
	private static final int HEIGHT = PORTRAIT + 2 * PADDING;

	private static final int PORTRAIT_BACKGROUND = 0x60000000;
	/** The font draws any colour with alpha below 4 fully opaque, so fading text stops here. */
	private static final float MIN_TEXT_ALPHA = 0.05f;

	/** True while the portrait draws, so its name tag can be hidden. */
	private static boolean drawingPortrait;

	private TargetPanelRenderer() {
	}

	public static boolean drawingPortrait() {
		return drawingPortrait;
	}

	public static void render(PoseStack poseStack, LivingEntity target, float alpha, HealthConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Component name = target.getDisplayName();
		int textWidth = Math.max(MIN_TEXT_WIDTH, font.width(name));
		int width = PADDING + PORTRAIT + PADDING + textWidth + PADDING;
		int[] corner = config.panelAnchor.place(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
				width, HEIGHT, config.panelX, config.panelY, true, true);
		int x = corner[0];
		int y = corner[1];

		GuiComponent.fill(poseStack, x, y, x + width, y + HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.PANEL, alpha));
		outline(poseStack, x, y, width, HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.FRAME, alpha));

		int portraitX = x + PADDING;
		int portraitY = y + PADDING;
		GuiComponent.fill(poseStack, portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT, MeterStyle.multiplyAlpha(PORTRAIT_BACKGROUND, alpha));
		// A 3D model can't fade; it goes when the panel is half faded.
		if (alpha >= 0.5f) {
			portrait(target, portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT);
		}

		int textX = portraitX + PORTRAIT + PADDING;
		int text = MeterStyle.multiplyAlpha(MeterStyle.TEXT, alpha);
		if (alpha >= MIN_TEXT_ALPHA) {
			font.drawShadow(poseStack, name, textX, y + PADDING, text);
		}

		float health = Math.max(target.getHealth(), 0);
		float maxHealth = target.getMaxHealth();
		String healthText = healthText(health, maxHealth, config.healthText);
		if (healthText != null && alpha >= MIN_TEXT_ALPHA) {
			int rowY = y + PADDING + font.lineHeight + 3;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, ICONS);
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1, 1, 1, alpha);
			GuiComponent.blit(poseStack, textX, rowY - 1, 0, HEART_U, HEART_V, HEART_SIZE, HEART_SIZE, 256, 256);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			font.drawShadow(poseStack, healthText, textX + HEART_SIZE + 2, rowY, text);
		}

		float fraction = MeterStyle.fraction(health, maxHealth);
		int barY = y + HEIGHT - PADDING - BAR_HEIGHT;
		int barRight = x + width - PADDING;
		int filled = textX + Math.round((barRight - textX) * fraction);
		GuiComponent.fill(poseStack, textX, barY, barRight, barY + BAR_HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.MISSING, alpha));
		if (filled > textX) {
			GuiComponent.fill(poseStack, textX, barY, filled, barY + BAR_HEIGHT,
					MeterStyle.multiplyAlpha(HealthColors.fill(target, fraction, config.barColor), alpha));
		}
	}

	private static void outline(PoseStack poseStack, int x, int y, int width, int height, int color) {
		GuiComponent.fill(poseStack, x, y, x + width, y + 1, color);
		GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, color);
		GuiComponent.fill(poseStack, x, y + 1, x + 1, y + height - 1, color);
		GuiComponent.fill(poseStack, x + width - 1, y + 1, x + width, y + height - 1, color);
	}

	/**
	 * The mob's model, drawn like the player in the inventory screen. Every port changes this method:
	 * the inventory's entity preview call changes in almost every version. On 1.19.2 it takes the angle
	 * as a mouse offset and turns the live entity itself; {@link PortraitFit#MOUSE_X} picks the offset.
	 */
	private static void portrait(LivingEntity target, int x0, int y0, int x1, int y1) {
		int deathTime = target.deathTime;
		// A dying mob keeps its red tint but doesn't fall over out of the box.
		target.deathTime = 0;
		drawingPortrait = true;
		try {
			PortraitFit.Fit fit = PortraitFit.fit(target, x0, y0, x1, y1);
			// No GUI scissor helper yet: clip in window pixels.
			Window window = Minecraft.getInstance().getWindow();
			double scale = window.getGuiScale();
			RenderSystem.enableScissor((int) (x0 * scale), (int) (window.getHeight() - y1 * scale),
					(int) ((x1 - x0) * scale), (int) ((y1 - y0) * scale));
			InventoryScreen.renderEntityInInventory(fit.x(), fit.y(), fit.scale(), PortraitFit.MOUSE_X, PortraitFit.MOUSE_Y, target);
			RenderSystem.disableScissor();
		} finally {
			drawingPortrait = false;
			target.deathTime = deathTime;
		}
	}

	private static String healthText(float health, float maxHealth, HealthConfig.HealthText mode) {
		return switch (mode) {
			case FRACTION -> number(health) + "/" + number(maxHealth);
			case PERCENT -> Math.round(MeterStyle.fraction(health, maxHealth) * 100) + "%";
			case HEARTS -> number(health / 2) + "/" + number(maxHealth / 2);
			case OFF -> null;
		};
	}

	/** Whole numbers without a decimal point, others to one place: 6, 6.5. */
	private static String number(float value) {
		float rounded = Math.round(value * 10) / 10f;
		return rounded == (int) rounded ? Integer.toString((int) rounded) : String.format(Locale.ROOT, "%.1f", rounded);
	}
}
