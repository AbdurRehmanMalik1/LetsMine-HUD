package net.firstwemine1.healthindicator.render;

import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;
import net.firstwemine1.healthindicator.core.BarStyle;
import net.firstwemine1.healthindicator.core.HIConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

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

	private static final int BACKGROUND = 0xB0101010;
	private static final int FRAME = 0xFF606060;
	private static final int PORTRAIT_BACKGROUND = 0x60000000;
	private static final int TEXT = 0xFFFFFFFF;
	/** The font draws any colour with alpha below 4 fully opaque, so fading text stops here. */
	private static final float MIN_TEXT_ALPHA = 0.05f;

	/** Turns the portrait to a three-quarter view. */
	private static final float PORTRAIT_YAW = 30;
	private static final float PORTRAIT_PITCH = -10;
	/** Pixels per block, so tiny mobs (silverfish, endermites) don't fill the whole box. */
	private static final float MAX_PORTRAIT_SCALE = 40;

	/** True while the portrait draws, so its name tag can be hidden. */
	private static boolean drawingPortrait;

	private TargetPanelRenderer() {
	}

	public static boolean drawingPortrait() {
		return drawingPortrait;
	}

	public static void render(GuiGraphics graphics, LivingEntity target, float alpha, HIConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Component name = target.getDisplayName();
		int textWidth = Math.max(MIN_TEXT_WIDTH, font.width(name));
		int width = PADDING + PORTRAIT + PADDING + textWidth + PADDING;
		int x = switch (config.panelAnchor) {
			case TOP_LEFT, BOTTOM_LEFT -> config.panelX;
			case TOP_CENTER -> (graphics.guiWidth() - width) / 2 + config.panelX;
			case TOP_RIGHT, BOTTOM_RIGHT -> graphics.guiWidth() - width - config.panelX;
		};
		int y = switch (config.panelAnchor) {
			case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> config.panelY;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - HEIGHT - config.panelY;
		};

		graphics.fill(x, y, x + width, y + HEIGHT, BarStyle.multiplyAlpha(BACKGROUND, alpha));
		graphics.renderOutline(x, y, width, HEIGHT, BarStyle.multiplyAlpha(FRAME, alpha));

		int portraitX = x + PADDING;
		int portraitY = y + PADDING;
		graphics.fill(portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT, BarStyle.multiplyAlpha(PORTRAIT_BACKGROUND, alpha));
		// A 3D model can't fade; it goes when the panel is half faded.
		if (alpha >= 0.5f) {
			portrait(graphics, target, portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT);
		}

		int textX = portraitX + PORTRAIT + PADDING;
		int text = BarStyle.multiplyAlpha(TEXT, alpha);
		if (alpha >= MIN_TEXT_ALPHA) {
			graphics.drawString(font, name, textX, y + PADDING, text);
		}

		float health = Math.max(target.getHealth(), 0);
		float maxHealth = target.getMaxHealth();
		String healthText = healthText(health, maxHealth, config.healthText);
		if (healthText != null && alpha >= MIN_TEXT_ALPHA) {
			int rowY = y + PADDING + font.lineHeight + 3;
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1, 1, 1, alpha);
			graphics.blit(ICONS, textX, rowY - 1, HEART_U, HEART_V, HEART_SIZE, HEART_SIZE);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			graphics.drawString(font, healthText, textX + HEART_SIZE + 2, rowY, text);
		}

		float fraction = BarStyle.fraction(health, maxHealth);
		int barY = y + HEIGHT - PADDING - BAR_HEIGHT;
		int barRight = x + width - PADDING;
		int filled = textX + Math.round((barRight - textX) * fraction);
		graphics.fill(textX, barY, barRight, barY + BAR_HEIGHT, BarStyle.multiplyAlpha(BarStyle.MISSING, alpha));
		graphics.fill(textX, barY, filled, barY + BAR_HEIGHT,
				BarStyle.multiplyAlpha(BarStyle.fillColor(target, fraction, config.barColor), alpha));
	}

	/**
	 * The mob's model, drawn like the player in the inventory screen. Every port changes this method:
	 * the inventory's entity preview call changes in almost every version. Before render states it
	 * draws the live entity, so its rotations are set for the portrait and put back afterwards.
	 */
	private static void portrait(GuiGraphics graphics, LivingEntity target, int x0, int y0, int x1, int y1) {
		float size = Math.max(target.getBbHeight(), target.getBbWidth());
		int scale = Math.round(Math.min((x1 - x0) * 0.8f / size, MAX_PORTRAIT_SCALE));
		Quaternionf pitch = new Quaternionf().rotateX(PORTRAIT_PITCH * ((float) Math.PI / 180));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(pitch);
		// The entity is drawn standing on (x, y).
		int x = (x0 + x1) / 2;
		int y = (y0 + y1) / 2 + Math.round(target.getBbHeight() * scale / 2);

		float bodyRot = target.yBodyRot;
		float yRot = target.getYRot();
		float xRot = target.getXRot();
		float headRot = target.yHeadRot;
		float headRotO = target.yHeadRotO;
		target.yBodyRot = 180 + PORTRAIT_YAW;
		target.setYRot(180 + PORTRAIT_YAW);
		target.setXRot(0);
		target.yHeadRot = target.getYRot();
		target.yHeadRotO = target.getYRot();
		drawingPortrait = true;
		try {
			graphics.enableScissor(x0, y0, x1, y1);
			InventoryScreen.renderEntityInInventory(graphics, x, y, scale, rotation, pitch, target);
			graphics.disableScissor();
		} finally {
			drawingPortrait = false;
			target.yBodyRot = bodyRot;
			target.setYRot(yRot);
			target.setXRot(xRot);
			target.yHeadRot = headRot;
			target.yHeadRotO = headRotO;
		}
	}

	private static String healthText(float health, float maxHealth, HIConfig.HealthText mode) {
		return switch (mode) {
			case FRACTION -> number(health) + "/" + number(maxHealth);
			case PERCENT -> Math.round(BarStyle.fraction(health, maxHealth) * 100) + "%";
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
