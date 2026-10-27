package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.firstwemine1.indicatorbars.config.IBConfig.HealthConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;

/**
 * The bar floating above a mob. Drawn like a name tag: facing the camera, at name-tag scale, so it
 * shrinks with distance, with the render type of a name tag's dark box. 26.2 has no polygon-offset
 * mode for these quads, so none of them overlap: a border, then the fill and the empty part side by
 * side.
 */
public final class WorldBarRenderer {
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

	public static void submit(HealthBarState.Bar bar, LivingEntityRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera, HealthConfig config) {
		poseStack.pushPose();
		float lift = LIFT + (state.nameTag != null ? NAME_TAG_CLEARANCE : 0);
		poseStack.translate(0, state.boundingBoxHeight + lift, 0);
		poseStack.mulPose(camera.orientation);
		float scale = EntityRenderer.NAMETAG_SCALE * (float) config.barScale;
		poseStack.scale(scale, -scale, scale);

		float fraction = bar.fraction;
		int color = bar.color;
		collector.submitCustomGeometry(poseStack, config.seeThrough ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(),
				(pose, consumer) -> {
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
				});
		poseStack.popPose();
	}

	/** Same corner order as vanilla's text background, so the quad faces the camera. */
	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, int color) {
		consumer.addVertex(pose, x0, y1, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y1, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y0, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		consumer.addVertex(pose, x0, y0, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
	}
}
