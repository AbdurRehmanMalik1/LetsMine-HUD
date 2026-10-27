package net.firstwemine1.indicatorbars.render;

import java.util.Locale;

import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.health.HealthColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
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
	/** Vanilla's heart, so resource packs re-skin it. */
	private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");

	private static final int PADDING = 4;
	private static final int PORTRAIT = 36;
	private static final int MIN_TEXT_WIDTH = 64;
	private static final int HEART_SIZE = 9;
	private static final int BAR_HEIGHT = 5;
	private static final int HEIGHT = PORTRAIT + 2 * PADDING;

	private static final int PORTRAIT_BACKGROUND = 0x60000000;

	/** Turns the portrait to a three-quarter view. */
	private static final float PORTRAIT_YAW = 30;
	private static final float PORTRAIT_PITCH = -10;

	private TargetPanelRenderer() {
	}

	public static void render(GuiGraphics graphics, LivingEntity target, float alpha, float partialTick, HealthConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		Component name = target.getDisplayName();
		int textWidth = Math.max(MIN_TEXT_WIDTH, font.width(name));
		int width = PADDING + PORTRAIT + PADDING + textWidth + PADDING;
		int[] corner = config.panelAnchor.place(graphics.guiWidth(), graphics.guiHeight(), width, HEIGHT,
				config.panelX, config.panelY, true, true);
		int x = corner[0];
		int y = corner[1];

		graphics.fill(x, y, x + width, y + HEIGHT, ARGB.multiplyAlpha(MeterStyle.PANEL, alpha));
		graphics.renderOutline(x, y, width, HEIGHT, ARGB.multiplyAlpha(MeterStyle.FRAME, alpha));

		int portraitX = x + PADDING;
		int portraitY = y + PADDING;
		graphics.fill(portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT, ARGB.multiplyAlpha(PORTRAIT_BACKGROUND, alpha));
		// A 3D model can't fade; it goes when the panel is half faded.
		if (alpha >= 0.5f) {
			portrait(graphics, minecraft, target, partialTick, portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT);
		}

		int textX = portraitX + PORTRAIT + PADDING;
		int text = ARGB.multiplyAlpha(MeterStyle.TEXT, alpha);
		graphics.drawString(font, name, textX, y + PADDING, text);

		float health = Math.max(target.getHealth(), 0);
		float maxHealth = target.getMaxHealth();
		String healthText = healthText(health, maxHealth, config.healthText);
		if (healthText != null) {
			int rowY = y + PADDING + font.lineHeight + 3;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, textX, rowY - 1, HEART_SIZE, HEART_SIZE, ARGB.white(alpha));
			graphics.drawString(font, healthText, textX + HEART_SIZE + 2, rowY, text);
		}

		float fraction = MeterStyle.fraction(health, maxHealth);
		int barY = y + HEIGHT - PADDING - BAR_HEIGHT;
		int barRight = x + width - PADDING;
		int filled = textX + Math.round((barRight - textX) * fraction);
		graphics.fill(textX, barY, barRight, barY + BAR_HEIGHT, ARGB.multiplyAlpha(MeterStyle.MISSING, alpha));
		graphics.fill(textX, barY, filled, barY + BAR_HEIGHT,
				ARGB.multiplyAlpha(HealthColors.fill(target, fraction, config.barColor), alpha));
	}

	/**
	 * The mob's model, drawn like the player in the inventory screen. Every port changes this method:
	 * the inventory's entity preview call changes in almost every version.
	 */
	private static void portrait(GuiGraphics graphics, Minecraft minecraft, LivingEntity target, float partialTick,
			int x0, int y0, int x1, int y1) {
		EntityRenderer<? super LivingEntity, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(target);
		EntityRenderState state = renderer.createRenderState(target, partialTick);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		state.leashStates = null;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180 + PORTRAIT_YAW;
			living.yRot = 0;
			living.xRot = 0;
			// Standing still, so the fit measured once stays right. A dying mob keeps its red tint but
			// doesn't fall over out of the box.
			living.walkAnimationPos = 0;
			living.walkAnimationSpeed = 0;
			living.deathTime = 0;
			// Babies and scaled mobs are drawn at their own size, as the inventory does.
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1;
		}
		Quaternionf pitch = new Quaternionf().rotateX(PORTRAIT_PITCH * ((float) Math.PI / 180));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(pitch);
		PortraitFit.Fit fit = PortraitFit.fit(target, state, rotation, x1 - x0);
		graphics.submitEntityRenderState(state, fit.scale(), fit.translation(), rotation, pitch, x0, y0, x1, y1);
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
