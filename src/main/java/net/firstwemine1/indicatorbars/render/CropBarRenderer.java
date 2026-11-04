package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Matrix4f;
import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/**
 * The small bar floating over each crop, drawn like the mob bars ({@link WorldBarRenderer}) but a
 * third of their size, with no quads overlapping. A crop that isn't growing as fast as it could gets
 * a red dotted line under its bar; the crop panel says why.
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
	private static final float NAMETAG_SCALE = 0.025f;
	private static final int FULL_BRIGHT = 0xF000F0;

	private CropBarRenderer() {
	}

	/** The pose stack is the level's, with the camera at the origin. No frustum here: few bars, all near, clipped as drawn. */
	public static void render(List<Crop> crops, MatrixStack poseStack, IRenderTypeBuffer buffers, ActiveRenderInfo camera,
			CropsConfig config) {
		Vector3d cameraPos = camera.getPosition();
		float scale = NAMETAG_SCALE * (float) config.barScale;
		IVertexBuilder consumer = buffers.getBuffer(BarRenderType.BAR);
		for (Crop crop : crops) {
			BlockPos pos = crop.pos();
			poseStack.pushPose();
			poseStack.translate(pos.getX() + 0.5 - cameraPos.x, pos.getY() + crop.top() + LIFT - cameraPos.y,
					pos.getZ() + 0.5 - cameraPos.z);
			poseStack.mulPose(camera.rotation());
			poseStack.scale(-scale, -scale, scale);
			bar(crop, poseStack.last().pose(), consumer, config);
			poseStack.popPose();
		}
	}

	private static void bar(Crop crop, Matrix4f pose, IVertexBuilder consumer, CropsConfig config) {
		boolean warn = crop.warns(config);
		float fraction = crop.fraction();
		float left = -WIDTH / 2;
		float right = WIDTH / 2;
		float split = left + WIDTH * fraction;
		float inner = HEIGHT + (warn ? DOT_GAP + DOT_HEIGHT : 0);
		quad(consumer, pose, left - BORDER, -BORDER, right + BORDER, 0, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, inner, right + BORDER, inner + BORDER, MeterStyle.BACKGROUND);
		quad(consumer, pose, left - BORDER, 0, left, inner, MeterStyle.BACKGROUND);
		quad(consumer, pose, right, 0, right + BORDER, inner, MeterStyle.BACKGROUND);
		if (fraction > 0) {
			quad(consumer, pose, left, 0, split, HEIGHT, crop.color());
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
}
