package net.firstwemine1.indicatorbars.meter.crops;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Mixed into {@code StemBlock}: the blocks its pumpkin or melon may sit on are a private field, and a
 * modded stem may use its own tag.
 */
public interface FruitSupport {
	TagKey<Block> indicatorbars$fruitSupport();
}
