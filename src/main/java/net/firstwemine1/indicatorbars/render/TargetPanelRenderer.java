package net.firstwemine1.indicatorbars.render;

import java.util.Locale;

import com.mojang.blaze3d.systems.RenderSystem;
import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
	/** Vanilla's heart, so resource packs re-skin it. */
	private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");

	private static final int PADDING = 4;
	private static final int PORTRAIT = 36;
	private static final int MIN_TEXT_WIDTH = 64;
	private static final int HEART_SIZE = 9;
	private static final int BAR_HEIGHT = 5;
	private static final int HEIGHT = PORTRAIT + 2 * PADDING;

	private static final int PORTRAIT_BACKGROUND = 0x60000000;
	/** The font draws any colour with alpha below 4 fully opaque, so fading text stops here. */
	private static final float MIN_TEXT_ALPHA = 0.05f;

	/** Turns the portrait to a three-quarter view. */
	private static final float PORTRAIT_YAW = 30;
	private static final float PORTRAIT_PITCH = -10;

	/** True while the portrait draws, so its name tag can be hidden. */
	private static boolean drawingPortrait;

	private TargetPanelRenderer() {
	}

	public static boolean drawingPortrait() {
		return drawingPortrait;
	}

	public static void render(GuiGraphics graphics, LivingEntity target, float alpha, HealthConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Component name = target.getDisplayName();
		int textWidth = Math.max(MIN_TEXT_WIDTH, font.width(name));
		int width = PADDING + PORTRAIT + PADDING + textWidth + PADDING;
		int[] corner = config.panelAnchor.place(graphics.guiWidth(), graphics.guiHeight(), width, HEIGHT,
				config.panelX, config.panelY, true, true);
		int x = corner[0];
		int y = corner[1];

		graphics.fill(x, y, x + width, y + HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.PANEL, alpha));
		graphics.renderOutline(x, y, width, HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.FRAME, alpha));

		int portraitX = x + PADDING;
		int portraitY = y + PADDING;
		graphics.fill(portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT, MeterStyle.multiplyAlpha(PORTRAIT_BACKGROUND, alpha));
		// A 3D model can't fade; it goes when the panel is half faded.
		if (alpha >= 0.5f) {
			portrait(graphics, target, portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT);
		}

		int textX = portraitX + PORTRAIT + PADDING;
		int text = MeterStyle.multiplyAlpha(MeterStyle.TEXT, alpha);
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
			graphics.blitSprite(HEART, textX, rowY - 1, HEART_SIZE, HEART_SIZE);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			graphics.drawString(font, healthText, textX + HEART_SIZE + 2, rowY, text);
		}

		float fraction = MeterStyle.fraction(health, maxHealth);
		int barY = y + HEIGHT - PADDING - BAR_HEIGHT;
		int barRight = x + width - PADDING;
		int filled = textX + Math.round((barRight - textX) * fraction);
		graphics.fill(textX, barY, barRight, barY + BAR_HEIGHT, MeterStyle.multiplyAlpha(MeterStyle.MISSING, alpha));
		graphics.fill(textX, barY, filled, barY + BAR_HEIGHT,
				MeterStyle.multiplyAlpha(HealthColors.fill(target, fraction, config.barColor), alpha));
	}

	/**
	 * The mob's model, drawn like the player in the inventory screen. Every port changes this method:
	 * the inventory's entity preview call changes in almost every version. Before render states it
	 * draws the live entity, turned for the portrait while it draws.
	 */
	private static void portrait(GuiGraphics graphics, LivingEntity target, int x0, int y0, int x1, int y1) {
		Quaternionf pitch = new Quaternionf().rotateX(PORTRAIT_PITCH * ((float) Math.PI / 180));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(pitch);

		float bodyRot = target.yBodyRot;
		float bodyRotO = target.yBodyRotO;
		float yRot = target.getYRot();
		float yRotO = target.yRotO;
		float xRot = target.getXRot();
		float xRotO = target.xRotO;
		float headRot = target.yHeadRot;
		float headRotO = target.yHeadRotO;
		int deathTime = target.deathTime;
		target.yBodyRot = target.yBodyRotO = 180 + PORTRAIT_YAW;
		target.setYRot(180 + PORTRAIT_YAW);
		target.yRotO = target.getYRot();
		target.setXRot(0);
		target.xRotO = 0;
		target.yHeadRot = target.yHeadRotO = target.getYRot();
		// A dying mob keeps its red tint but doesn't fall over out of the box.
		target.deathTime = 0;
		drawingPortrait = true;
		try {
			PortraitFit.Fit fit = PortraitFit.fit(target, rotation, x0, y0, x1, y1);
			graphics.enableScissor(x0, y0, x1, y1);
			InventoryScreen.renderEntityInInventory(graphics, fit.x(), fit.y(), fit.scale(), new Vector3f(), rotation, pitch, target);
			graphics.disableScissor();
		} finally {
			drawingPortrait = false;
			target.yBodyRot = bodyRot;
			target.yBodyRotO = bodyRotO;
			target.setYRot(yRot);
			target.yRotO = yRotO;
			target.setXRot(xRot);
			target.xRotO = xRotO;
			target.yHeadRot = headRot;
			target.yHeadRotO = headRotO;
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
