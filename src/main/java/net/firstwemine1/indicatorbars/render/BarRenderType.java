package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

/**
 * Coloured, translucent, unculled quads for the floating bars. Before 1.19.4 there is no text-background
 * render type to borrow, so this builds one; it extends RenderType only to reach the protected states.
 */
public final class BarRenderType extends RenderType {
	public static final RenderType BAR = create("indicatorbars_bar", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
			GL11.GL_QUADS, 256, false, true, CompositeState.builder()
					.setTextureState(NO_TEXTURE)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setCullState(NO_CULL)
					.createCompositeState(false));
	/** The same, drawn over everything, for the see-through option. */
	public static final RenderType BAR_SEE_THROUGH = create("indicatorbars_bar_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
			GL11.GL_QUADS, 256, false, true, CompositeState.builder()
					.setTextureState(NO_TEXTURE)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setCullState(NO_CULL)
					.setDepthTestState(NO_DEPTH_TEST)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));

	private BarRenderType() {
		super("indicatorbars_unused", DefaultVertexFormat.POSITION_COLOR, GL11.GL_QUADS, 256, false, false, () -> {
		}, () -> {
		});
	}
}
