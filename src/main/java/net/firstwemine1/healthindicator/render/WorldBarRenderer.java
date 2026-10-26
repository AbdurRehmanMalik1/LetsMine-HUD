package net.firstwemine1.healthindicator.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.healthindicator.core.BarStyle;
import net.firstwemine1.healthindicator.core.HIConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;

/**
 * The bar floating above a mob. Drawn like a name tag: facing the camera, at name-tag scale, so it
 * shrinks with distance. Both quads are text backgrounds, the same render type as a name tag's dark
 * box; the fill uses the polygon offset vanilla uses to lift text above its background.
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
			SubmitNodeCollector collector, CameraRenderState camera, HIConfig config) {
		poseStack.pushPose();
		float lift = LIFT + (state.nameTag != null ? NAME_TAG_CLEARANCE : 0);
		poseStack.translate(0, state.boundingBoxHeight + lift, 0);
		poseStack.rotate(camera.orientation);
		float scale = EntityRenderer.NAMETAG_SCALE * (float) config.barScale;
		poseStack.scale(scale, -scale, scale);

		Font.DisplayMode back = config.seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
		Font.DisplayMode front = config.seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET;
		float left = -WIDTH / 2;
		collector.submitTextBackground(poseStack, left - BORDER, -BORDER, -left + BORDER, HEIGHT + BORDER,
				BarStyle.BACKGROUND, back, LightCoordsUtil.FULL_BRIGHT);
		if (bar.fraction > 0) {
			collector.submitTextBackground(poseStack, left, 0, left + WIDTH * bar.fraction, HEIGHT,
					bar.color, front, LightCoordsUtil.FULL_BRIGHT);
		}
		poseStack.popPose();
	}
}
