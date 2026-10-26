package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * The bar floating above a mob. Drawn like a name tag: facing the camera, at name-tag scale, so it
 * shrinks with distance, in {@link BarRenderType}. There is no polygon offset for these quads, so none
 * of them overlap: a border, then the fill and the empty part side by side.
 */
public final class WorldBarRenderer {
	/** Full block and sky light, packed (1.17+ has it as {@code LightTexture.FULL_BRIGHT}). */
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Vanilla's name tag scale ({@code EntityRenderer.NAMETAG_SCALE} on later versions). */
	private static final float NAMETAG_SCALE = 0.025f;
	/** In name-tag units: 40 is one block. */
	private static final float WIDTH = 40;
	private static final float HEIGHT = 3;
	private static final float BORDER = 1;
	/** Above the mob's bounding box, where a name tag would sit. */
	private static final float LIFT = 0.5f;
	/** Extra lift when a name tag shows, so the bar sits above it. */
	private static final float NAME_TAG_CLEARANCE = 0.3f;

	private WorldBarRenderer() {
	}

	/** Called with the matrix stack at the mob's feet, right after vanilla renders it. */
	public static void render(LivingEntity entity, float fraction, int color, MatrixStack poseStack, IRenderTypeBuffer buffers, HealthConfig config) {
		Minecraft minecraft = Minecraft.getInstance();
		poseStack.pushPose();
		float lift = LIFT + (nameTagShown(entity, minecraft) ? NAME_TAG_CLEARANCE : 0);
		poseStack.translate(0, entity.getBbHeight() + lift, 0);
		poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
		float scale = NAMETAG_SCALE * (float) config.barScale;
		poseStack.scale(-scale, -scale, scale);

		Matrix4f pose = poseStack.last().pose();
		IVertexBuilder consumer = buffers.getBuffer(config.seeThrough ? BarRenderType.BAR_SEE_THROUGH : BarRenderType.BAR);
		float left = -WIDTH / 2;
		float right = WIDTH / 2;
		float split = left + WIDTH * fraction;
		quad(consumer, pose, left - BORDER, -BORDER, right + BORDER, 0, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, HEIGHT, right + BORDER, HEIGHT + BORDER, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, 0, left, HEIGHT, MeterStyle.BACKGROUND);
		quad(consumer, pose, right, 0, right + BORDER, HEIGHT, MeterStyle.BACKGROUND);
		if (fraction > 0) {
			quad(consumer, pose, left, 0, split, HEIGHT, color);
		}
		if (fraction < 1) {
			quad(consumer, pose, split, 0, right, HEIGHT, MeterStyle.BACKGROUND);
		}
		poseStack.popPose();
	}

	private static void quad(IVertexBuilder consumer, Matrix4f pose, float x0, float y0, float x1, float y1, int color) {
		int a = color >>> 24;
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		consumer.vertex(pose, x0, y1, 0).color(r, g, b, a).uv2(FULL_BRIGHT).endVertex();
		consumer.vertex(pose, x1, y1, 0).color(r, g, b, a).uv2(FULL_BRIGHT).endVertex();
		consumer.vertex(pose, x1, y0, 0).color(r, g, b, a).uv2(FULL_BRIGHT).endVertex();
		consumer.vertex(pose, x0, y0, 0).color(r, g, b, a).uv2(FULL_BRIGHT).endVertex();
	}

	/**
	 * Close to {@code LivingRenderer.shouldShowName}, which is protected. Later versions read the
	 * answer from the render state instead.
	 */
	private static boolean nameTagShown(LivingEntity entity, Minecraft minecraft) {
		return entity instanceof PlayerEntity || entity.isCustomNameVisible()
				|| entity.hasCustomName() && entity == minecraft.getEntityRenderDispatcher().crosshairPickEntity;
	}
}
