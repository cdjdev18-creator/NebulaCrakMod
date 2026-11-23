package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MagmaBlock extends Block {
   public static final MapCodec CODEC = simpleCodec(MagmaBlock::new);
   private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

   public MapCodec codec() {
      return CODEC;
   }

   public MagmaBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   public void stepOn(final Level level, final BlockPos pos, final BlockState onState, final Entity entity) {
      if (!entity.isSteppingCarefully() && entity instanceof LivingEntity) {
         entity.hurt(level.damageSources().hotFloor(), 1.0F);
      }

      super.stepOn(level, pos, onState, entity);
   }

   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      BubbleColumnBlock.updateColumn(level, pos.above(), state);
   }

   protected BlockState updateShape(final BlockState state, final LevelReader level, final ScheduledTickAccess ticks, final BlockPos pos, final Direction directionToNeighbour, final BlockPos neighbourPos, final BlockState neighbourState, final RandomSource random) {
      if (directionToNeighbour == Direction.UP && neighbourState.is(Blocks.WATER)) {
         ticks.scheduleTick(pos, (Block)this, 20);
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      level.scheduleTick(pos, this, 20);
   }
}
