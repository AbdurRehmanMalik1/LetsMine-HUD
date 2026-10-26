package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Sizes and centres the panel's portrait from the mob's model, not its hitbox: hitboxes are square seen
 * from above, but a pig's snout or a cow's head sticks out past them. Before render states the portrait
 * draws the live entity, so this measures the renderer's model posed on that entity. The members it reads
 * are opened in META-INF/accesstransformer.cfg.
 */
public final class PortraitFit {
	/**
	 * The inventory preview turns the mob by a mouse offset: body {@code atan(x / 40) * 20} degrees, head
	 * twice that, view pitch {@code atan(y / 40) * 20}. These give a three-quarter view (body 30 degrees)
	 * looking slightly down (10 degrees).
	 */
	public static final float MOUSE_X = (float) (40 * Math.tan(30.0 / 20));
	public static final float MOUSE_Y = (float) (40 * Math.tan(-10.0 / 20));
	private static final float BODY_YAW = (float) Math.atan(MOUSE_X / 40) * 20;
	private static final float VIEW_PITCH = (float) Math.atan(MOUSE_Y / 40) * 20;

	/** How much of the box the model's larger side fills. */
	private static final float FILL = 0.85f;
	/** Pixels per block, so tiny mobs (silverfish, endermites) don't fill the whole box. */
	private static final float MAX_SCALE = 40;
	/** Fallback only: how much longer than its hitbox a mob's model usually is (heads, tails). */
	private static final float HITBOX_LENGTH = 1.5f;
	/** Vanilla's offset from feet to model origin, from {@code LivingEntityRenderer.render}. */
	private static final float MODEL_OFFSET = -1.501f;

	/** Pixels per block, and where the entity's feet go so the model is centred in the box. */
	public record Fit(int scale, int x, int y) {
	}

	private static @Nullable LivingEntity fittedTarget;
	private static @Nullable Fit fit;

	private PortraitFit() {
	}

	public static void reset() {
		fittedTarget = null;
		fit = null;
	}

	/** Measured once per target. */
	public static Fit fit(LivingEntity target, int x0, int y0, int x1, int y1) {
		if (target != fittedTarget || fit == null) {
			float[] bounds = measure(target);
			fittedTarget = target;
			fit = bounds != null ? place(bounds, x0, y0, x1, y1) : hitbox(target, x0, y0, x1, y1);
		}
		return fit;
	}

	@SuppressWarnings("unchecked")
	private static float @Nullable [] measure(LivingEntity target) {
		EntityRenderer<? super LivingEntity> entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(target);
		if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> living)) {
			return null;
		}
		LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer = (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) living;
		EntityModel<LivingEntity> model = renderer.getModel();
		model.young = target.isBaby();
		// The head turns twice as far as the body in the preview, and looks down by the view pitch.
		model.setupAnim(target, 0, 0, target.tickCount, BODY_YAW, -VIEW_PITCH);

		// The GUI rotation, then the same steps as LivingEntityRenderer.render for a standing mob.
		Quaternion rotation = Vector3f.ZP.rotationDegrees(180);
		rotation.mul(Vector3f.XP.rotationDegrees(VIEW_PITCH));
		PoseStack poseStack = new PoseStack();
		poseStack.mulPose(rotation);
		poseStack.mulPose(Vector3f.YP.rotationDegrees(180 - (180 + BODY_YAW)));
		poseStack.scale(-1, -1, 1);
		renderer.scale(target, poseStack, 1);
		poseStack.translate(0, MODEL_OFFSET, 0);

		float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		if (model instanceof HierarchicalModel<?> hierarchical) {
			add(hierarchical.root(), poseStack, bounds);
		} else if (model instanceof AgeableListModel<?> ageable) {
			addAgeable(ageable, poseStack, bounds);
		} else {
			return null;
		}
		return bounds[2] > bounds[0] && bounds[3] > bounds[1] ? bounds : null;
	}

	/** The same head and body transforms as {@code AgeableListModel.renderToBuffer}. */
	private static void addAgeable(AgeableListModel<?> model, PoseStack poseStack, float[] bounds) {
		if (!model.young) {
			model.headParts().forEach(part -> add(part, poseStack, bounds));
			model.bodyParts().forEach(part -> add(part, poseStack, bounds));
			return;
		}
		poseStack.pushPose();
		if (model.scaleHead) {
			float scale = 1.5f / model.babyHeadScale;
			poseStack.scale(scale, scale, scale);
		}
		poseStack.translate(0, model.babyYHeadOffset / 16, model.babyZHeadOffset / 16);
		model.headParts().forEach(part -> add(part, poseStack, bounds));
		poseStack.popPose();
		poseStack.pushPose();
		float scale = 1 / model.babyBodyScale;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(0, model.bodyYOffset / 16, 0);
		model.bodyParts().forEach(part -> add(part, poseStack, bounds));
		poseStack.popPose();
	}

	/** Every corner of every drawn cube under {@code part}, as {x0, y0, x1, y1} after the pose. */
	private static void add(ModelPart part, PoseStack poseStack, float[] bounds) {
		if (!part.visible) {
			return;
		}
		part.visit(poseStack, (pose, path, cubeIndex, cube) -> {
			if (!drawn(part, path)) {
				return;
			}
			for (int corner = 0; corner < 8; corner++) {
				Vector4f point = new Vector4f(
						((corner & 1) == 0 ? cube.minX : cube.maxX) / 16,
						((corner & 2) == 0 ? cube.minY : cube.maxY) / 16,
						((corner & 4) == 0 ? cube.minZ : cube.maxZ) / 16, 1);
				point.transform(pose.pose());
				bounds[0] = Math.min(bounds[0], point.x());
				bounds[1] = Math.min(bounds[1], point.y());
				bounds[2] = Math.max(bounds[2], point.x());
				bounds[3] = Math.max(bounds[3], point.y());
			}
		});
	}

	/** Whether the part at {@code path} ("/body/head") under {@code root} is drawn: visible all the way down. */
	private static boolean drawn(ModelPart root, String path) {
		ModelPart part = root;
		for (String name : path.split("/")) {
			if (name.isEmpty()) {
				continue;
			}
			try {
				part = part.getChild(name);
			} catch (RuntimeException missing) {
				return true;
			}
			if (!part.visible) {
				return false;
			}
		}
		return !part.skipDraw;
	}

	private static Fit place(float[] bounds, int x0, int y0, int x1, int y1) {
		float size = Math.max(bounds[2] - bounds[0], bounds[3] - bounds[1]);
		int scale = Math.max(1, Math.round(Math.min((x1 - x0) * FILL / size, MAX_SCALE)));
		int x = Math.round((x0 + x1) / 2f - scale * (bounds[0] + bounds[2]) / 2);
		int y = Math.round((y0 + y1) / 2f - scale * (bounds[1] + bounds[3]) / 2);
		return new Fit(scale, x, y);
	}

	private static Fit hitbox(LivingEntity target, int x0, int y0, int x1, int y1) {
		float size = Math.max(target.getBbHeight(), target.getBbWidth() * HITBOX_LENGTH);
		int scale = Math.max(1, Math.round(Math.min((x1 - x0) * FILL / size, MAX_SCALE)));
		return new Fit(scale, (x0 + x1) / 2, (y0 + y1) / 2 + Math.round(target.getBbHeight() * scale / 2));
	}
}
