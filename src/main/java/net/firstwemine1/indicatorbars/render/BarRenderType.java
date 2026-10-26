package net.firstwemine1.indicatorbars.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Coloured, translucent, unculled quads for the floating bars. Before 1.19.4 there is no text-background
 * render type to borrow, so this builds one; it extends RenderType only to reach the protected shards.
 */
public final class BarRenderType extends RenderType {
	public static final RenderType BAR = create("indicatorbars_bar", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
			VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
					.setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setCullState(NO_CULL)
					.createCompositeState(false));
	/** The same, drawn over everything, for the see-through option. */
	public static final RenderType BAR_SEE_THROUGH = create("indicatorbars_bar_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
			VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
					.setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setCullState(NO_CULL)
					.setDepthTestState(NO_DEPTH_TEST)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));

	private BarRenderType() {
		super("indicatorbars_unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {
		}, () -> {
		});
	}
}
