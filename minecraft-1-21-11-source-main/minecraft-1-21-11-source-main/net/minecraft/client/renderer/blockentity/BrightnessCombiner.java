package net.minecraft.client.renderer.blockentity;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BrightnessCombiner implements DoubleBlockCombiner.Combiner {
   public Int2IntFunction acceptDouble(final BlockEntity first, final BlockEntity second) {
      return (i) -> {
         int firstCoords = LevelRenderer.getLightColor(first.getLevel(), first.getBlockPos());
         int secondCoords = LevelRenderer.getLightColor(second.getLevel(), second.getBlockPos());
         int firstBlock = LightTexture.block(firstCoords);
         int secondBlock = LightTexture.block(secondCoords);
         int firstSky = LightTexture.sky(firstCoords);
         int secondSky = LightTexture.sky(secondCoords);
         return LightTexture.pack(Math.max(firstBlock, secondBlock), Math.max(firstSky, secondSky));
      };
   }

   public Int2IntFunction acceptSingle(final BlockEntity single) {
      return (i) -> i;
   }

   public Int2IntFunction acceptNone() {
      return (i) -> i;
   }
}
