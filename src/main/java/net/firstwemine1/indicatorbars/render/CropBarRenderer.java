package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

/**
 * The small bar floating over each crop, drawn like the mob bars ({@link WorldBarRenderer}) but a
 * third of their size: 26.2 has no polygon-offset mode for these quads either, so none of them
 * overlap. A crop that isn't growing as fast as it could gets a red dotted line under its bar; the
 * crop panel says why.
 *
 * <pre>
 * [=====-----]
 * - - - - - -    (red: too dark, dry, no room)
 * </pre>
 */
public final class CropBarRenderer {
	/** In name-tag units: 40 is one block. */
	private static final float WIDTH = 16;
	private static final float HEIGHT = 2;
	private static final float BORDER = 1;
	/** Above the top of the plant. */
	private static final float LIFT = 0.2f;
	/** The dotted line: dashes this long with gaps as long, a row under the bar. */
	private static final float DASH = 2;
	private static final float DOT_HEIGHT = 1;
	private static final float DOT_GAP = 1;

	private CropBarRenderer() {
	}

	/** The pose stack is the level's, with the camera at the origin. No frustum here: few bars, all near, clipped as drawn. */
	public static void render(List<Crop> crops, PoseStack poseStack, MultiBufferSource buffers, CameraRenderState camera,
			CropsConfig config) {
		VertexConsumer consumer = buffers.getBuffer(RenderTypes.textBackground());
		Vec3 cameraPos = camera.pos;
		float scale = EntityRenderer.NAMETAG_SCALE * (float) config.barScale;
		for (Crop crop : crops) {
			BlockPos pos = crop.pos();
			poseStack.pushPose();
			poseStack.translate(pos.getX() + 0.5 - cameraPos.x, pos.getY() + crop.top() + LIFT - cameraPos.y,
					pos.getZ() + 0.5 - cameraPos.z);
			poseStack.mulPose(camera.orientation);
			poseStack.scale(scale, -scale, scale);
			bar(crop, poseStack.last(), consumer, config);
			poseStack.popPose();
		}
	}

	private static void bar(Crop crop, PoseStack.Pose pose, VertexConsumer consumer, CropsConfig config) {
		boolean warn = crop.warns(config);
		float fraction = crop.fraction();
		int color = crop.color();
		float left = -WIDTH / 2;
		float right = WIDTH / 2;
		float split = left + WIDTH * fraction;
		float inner = HEIGHT + (warn ? DOT_GAP + DOT_HEIGHT : 0);
		quad(consumer, pose, left - BORDER, -BORDER, right + BORDER, 0, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, inner, right + BORDER, inner + BORDER, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, 0, left, inner, MeterStyle.BACKGROUND);
		quad(consumer, pose, right, 0, right + BORDER, inner, MeterStyle.BACKGROUND);
		if (fraction > 0) {
			quad(consumer, pose, left, 0, split, HEIGHT, color);
		}
		if (fraction < 1) {
			quad(consumer, pose, split, 0, right, HEIGHT, MeterStyle.BACKGROUND);
		}
		if (warn) {
			float dotTop = HEIGHT + DOT_GAP;
			quad(consumer, pose, left, HEIGHT, right, dotTop, MeterStyle.BACKGROUND);
			for (float x = left; x < right; x += 2 * DASH) {
				float dashEnd = Math.min(x + DASH, right);
				quad(consumer, pose, x, dotTop, dashEnd, inner, MeterStyle.RED);
				if (dashEnd < right) {
					quad(consumer, pose, dashEnd, dotTop, Math.min(x + 2 * DASH, right), inner, MeterStyle.BACKGROUND);
				}
			}
		}
	}

	/** Same corner order as vanilla's text background, so the quad faces the camera. */
	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, int color) {
		consumer.addVertex(pose, x0, y1, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y1, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x1, y0, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex(pose, x0, y0, 0).setColor(color).setLight(LightTexture.FULL_BRIGHT);
	}
}
