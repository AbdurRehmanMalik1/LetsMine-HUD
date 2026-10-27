package net.firstwemine1.indicatorbars.render;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Sizes and centres the panel's portrait from the mob's model, not its hitbox: hitboxes are square seen
 * from above, but a pig's snout or a cow's head sticks out past them. Vanilla fits chests and banners into
 * item slots the same way ({@code ModelPart.getExtentsForGui}).
 */
public final class PortraitFit {
	/** How much of the box the model's larger side fills. */
	private static final float FILL = 0.85f;
	/** Pixels per block, so tiny mobs (silverfish, endermites) don't fill the whole box. */
	private static final float MAX_SCALE = 40;
	/** Fallback only: how much longer than its hitbox a mob's model usually is (heads, tails). */
	private static final float HITBOX_LENGTH = 1.5f;
	/** Vanilla's offset from feet to model origin, from {@code LivingEntityRenderer.submit}. */
	private static final float MODEL_OFFSET = -1.501f;

	/** Pixels per block, and the offset in blocks that centres the model in the box. */
	public record Fit(float scale, Vector3f translation) {
	}

	private static @Nullable PortraitRenderer capturedRenderer;
	private static int capturedEntityId = -1;
	private static @Nullable LivingEntity fittedTarget;
	private static @Nullable EntityModel<?> fittedModel;
	private static @Nullable Fit fit;

	private PortraitFit() {
	}

	/** The target was just submitted in the world, so its renderer holds the model it drew it with. */
	public static void capture(PortraitRenderer renderer, int entityId) {
		capturedRenderer = renderer;
		capturedEntityId = entityId;
	}

	public static void reset() {
		capturedRenderer = null;
		capturedEntityId = -1;
		fittedTarget = null;
		fittedModel = null;
		fit = null;
	}

	/** Measured once per target and model; the hitbox until the target has been drawn. */
	public static Fit fit(LivingEntity target, EntityRenderState state, Quaternionf rotation, int boxSize) {
		PortraitRenderer renderer = capturedEntityId == target.getId() ? capturedRenderer : null;
		if (renderer != null && state instanceof LivingEntityRenderState living) {
			EntityModel<?> model = renderer.indicatorbars$model();
			if (fit == null || fittedTarget != target || fittedModel != model) {
				Fit measured = measure(renderer, model, living, rotation, boxSize);
				if (measured != null) {
					fittedTarget = target;
					fittedModel = model;
					fit = measured;
				}
			}
			if (fittedTarget == target && fit != null) {
				return fit;
			}
		}
		return hitbox(state, boxSize);
	}

	@SuppressWarnings("unchecked")
	private static @Nullable Fit measure(PortraitRenderer renderer, EntityModel<?> model, LivingEntityRenderState state,
			Quaternionf rotation, int boxSize) {
		((EntityModel<LivingEntityRenderState>) model).setupAnim(state);

		// The GUI rotation, then the same steps as LivingEntityRenderer.submit for a standing mob.
		PoseStack poseStack = new PoseStack();
		poseStack.mulPose(rotation);
		poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.bodyRot));
		poseStack.scale(-1, -1, 1);
		renderer.indicatorbars$scale(state, poseStack);
		poseStack.translate(0, MODEL_OFFSET, 0);

		ModelPart root = model.root();
		Map<String, Boolean> shown = new HashMap<>();
		float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		root.visit(poseStack, (pose, partPath, cubeIndex, cube) -> {
			if (!shown.computeIfAbsent(partPath, path -> drawn(root, path))) {
				return;
			}
			for (ModelPart.Polygon polygon : cube.polygons) {
				for (ModelPart.Vertex vertex : polygon.vertices()) {
					Vector3f point = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
					bounds[0] = Math.min(bounds[0], point.x);
					bounds[1] = Math.min(bounds[1], point.y);
					bounds[2] = Math.max(bounds[2], point.x);
					bounds[3] = Math.max(bounds[3], point.y);
				}
			}
		});
		float width = bounds[2] - bounds[0];
		float height = bounds[3] - bounds[1];
		if (!(width > 0) || !(height > 0)) {
			return null;
		}
		float scale = Math.min(boxSize * FILL / Math.max(width, height), MAX_SCALE);
		return new Fit(scale, new Vector3f(-(bounds[0] + bounds[2]) / 2, -(bounds[1] + bounds[3]) / 2, 0));
	}

	/** Whether the part at {@code path} ("/body/head") is drawn: it and every parent visible, not skipped. */
	private static boolean drawn(ModelPart root, String path) {
		ModelPart part = root;
		for (String name : path.split("/")) {
			if (!name.isEmpty()) {
				if (!part.hasChild(name)) {
					return true;
				}
				part = part.getChild(name);
			}
			if (!part.visible) {
				return false;
			}
		}
		return !part.skipDraw;
	}

	private static Fit hitbox(EntityRenderState state, int boxSize) {
		float size = Math.max(state.boundingBoxHeight, state.boundingBoxWidth * HITBOX_LENGTH);
		float scale = Math.min(boxSize * FILL / size, MAX_SCALE);
		return new Fit(scale, new Vector3f(0, state.boundingBoxHeight / 2, 0));
	}
}
