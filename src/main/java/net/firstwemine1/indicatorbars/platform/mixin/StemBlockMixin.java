package net.firstwemine1.indicatorbars.platform.mixin;

import net.firstwemine1.indicatorbars.meter.crops.FruitSupport;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Lends the crop meter the stem's fruit support tag, to tell whether its fruit has room. */
@Mixin(StemBlock.class)
abstract class StemBlockMixin implements FruitSupport {
	@Shadow
	@Final
	private TagKey<Block> fruitSupportBlocks;

	@Override
	public TagKey<Block> indicatorbars$fruitSupport() {
		return fruitSupportBlocks;
	}
}
