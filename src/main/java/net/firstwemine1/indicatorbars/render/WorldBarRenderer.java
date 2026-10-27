package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Matrix4f;

/**
 * The bar floating above a mob. Drawn like a name tag: facing the camera, at name-tag scale, so it
 * shrinks with distance, with the render type of a name tag's dark box. There is no polygon-offset
 * mode for these quads, so none of them overlap: a border, then the fill and the empty part side by
 * side.
 */
public final class WorldBarRenderer {
	/** Vanilla's name tag scale ({@code EntityRenderer.NAMETAG_SCALE}, protected here). */
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

	/** Called with the pose stack at the mob's feet, right after vanilla renders it. */
	public static void render(HealthBarState.Bar bar, LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers,
			HealthConfig config) {
		poseStack.pushPose();
		float lift = LIFT + (state.nameTag != null ? NAME_TAG_CLEARANCE : 0);
		poseStack.translate(0, state.boundingBoxHeight + lift, 0);
		poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
		float scale = NAMETAG_SCALE * (float) config.barScale;
		poseStack.scale(scale, -scale, scale);

		Matrix4f pose = poseStack.last().pose();
		VertexConsumer consumer = buffers.getBuffer(config.seeThrough ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground());
		float left = -WIDTH / 2;
		float right = WIDTH / 2;
		float split = left + WIDTH * bar.fraction;
		quad(consumer, pose, left - BORDER, -BORDER, right + BORDER, 0, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, HEIGHT, right + BORDER, HEIGHT + BORDER, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, 0, left, HEIGHT, MeterStyle.BACKGROUND);
		quad(consumer, pose, right, 0, right + BORDER, HEIGHT, MeterStyle.BACKGROUND);
		if (bar.fraction > 0) {
			quad(consumer, pose, left, 0, split, HEIGHT, bar.color);
		}
		if (bar.fraction < 1) {
			quad(consumer, pose, split, 0, right, HEIGHT, MeterStyle.BACKGROUND);
		}
		poseStack.popPose();
	}

	/** Same corner order as vanilla's text background, so the quad faces the camera. */
	private static void quad(VertexConsumer consumer, Matrix4f pose, float x0, float y0, float x1, float y1, int color) {
		consumer.addVertex(pose, x0, y1, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y1, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y0, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x0, y0, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
	}
}
