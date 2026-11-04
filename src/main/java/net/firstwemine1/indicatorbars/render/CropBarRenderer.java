package net.firstwemine1.indicatorbars.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.indicatorbars.config.IBConfig.CropsConfig;
import net.firstwemine1.indicatorbars.meter.MeterStyle;
import net.firstwemine1.indicatorbars.meter.crops.CropGrowth.Crop;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The small bar floating over each crop, drawn like the mob bars ({@link WorldBarRenderer}) but a
 * third of their size. A crop that isn't growing as fast as it could gets a red dotted line under its
 * bar; the crop panel says why.
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

	/** The pose stack is the level's, with the camera at the origin. */
	public static void submit(List<Crop> crops, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera,
			CropsConfig config) {
		Vec3 cameraPos = camera.pos;
		float scale = EntityRenderer.NAMETAG_SCALE * (float) config.barScale;
		for (Crop crop : crops) {
			BlockPos pos = crop.pos();
			if (!camera.cullFrustum.isVisible(new AABB(pos).expandTowards(0, crop.top(), 0))) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(pos.getX() + 0.5 - cameraPos.x, pos.getY() + crop.top() + LIFT - cameraPos.y,
					pos.getZ() + 0.5 - cameraPos.z);
			poseStack.rotate(camera.orientation);
			poseStack.scale(scale, -scale, scale);
			bar(crop, poseStack, collector, config);
			poseStack.popPose();
		}
	}

	private static void bar(Crop crop, PoseStack poseStack, SubmitNodeCollector collector, CropsConfig config) {
		boolean warn = crop.warns(config);
		float left = -WIDTH / 2;
		float bottom = HEIGHT + (warn ? DOT_GAP + DOT_HEIGHT : 0) + BORDER;
		collector.submitTextBackground(poseStack, left - BORDER, -BORDER, -left + BORDER, bottom,
				MeterStyle.BACKGROUND, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT);
		float fraction = crop.fraction();
		if (fraction > 0) {
			collector.submitTextBackground(poseStack, left, 0, left + WIDTH * fraction, HEIGHT,
					crop.color(), Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT);
		}
		if (warn) {
			float dotTop = HEIGHT + DOT_GAP;
			for (float x = left; x < -left; x += 2 * DASH) {
				collector.submitTextBackground(poseStack, x, dotTop, Math.min(x + DASH, -left), dotTop + DOT_HEIGHT,
						MeterStyle.RED, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT);
			}
		}
	}
}
