package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FollowBoatGoal extends Goal {
   private int timeToRecalcPath;
   private final PathfinderMob mob;
   private @Nullable Player following;
   private BoatGoals currentGoal;

   public FollowBoatGoal(final PathfinderMob mob) {
      this.mob = mob;
   }

   public boolean canUse() {
      if (this.following != null && this.following.hasMovedHorizontallyRecently()) {
         return true;
      } else {
         for(AbstractBoat boat : this.mob.level().getEntitiesOfClass(AbstractBoat.class, this.mob.getBoundingBox().inflate((double)5.0F))) {
            LivingEntity var5 = boat.getControllingPassenger();
            if (var5 instanceof Player) {
               Player controllingPlayer = (Player)var5;
               if (controllingPlayer.hasMovedHorizontallyRecently()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public boolean isInterruptable() {
      return true;
   }

   public boolean canContinueToUse() {
      return this.following != null && this.following.isPassenger() && this.following.hasMovedHorizontallyRecently();
   }

   public void start() {
      for(AbstractBoat boat : this.mob.level().getEntitiesOfClass(AbstractBoat.class, this.mob.getBoundingBox().inflate((double)5.0F))) {
         LivingEntity var5 = boat.getControllingPassenger();
         if (var5 instanceof Player player) {
            this.following = player;
            break;
         }
      }

      this.timeToRecalcPath = 0;
      this.currentGoal = BoatGoals.GO_TO_BOAT;
   }

   public void stop() {
      this.following = null;
   }

   public void tick() {
      float speed = this.currentGoal == BoatGoals.GO_IN_BOAT_DIRECTION ? 0.01F : 0.015F;
      this.mob.moveRelative(speed, new Vec3((double)this.mob.xxa, (double)this.mob.yya, (double)this.mob.zza));
      this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         if (this.currentGoal == BoatGoals.GO_TO_BOAT) {
            BlockPos behindBoatPos = this.following.blockPosition().relative(this.following.getDirection().getOpposite());
            behindBoatPos = behindBoatPos.offset(0, -1, 0);
            this.mob.getNavigation().moveTo((double)behindBoatPos.getX(), (double)behindBoatPos.getY(), (double)behindBoatPos.getZ(), (double)1.0F);
            if (this.mob.distanceTo(this.following) < 4.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = BoatGoals.GO_IN_BOAT_DIRECTION;
            }
         } else if (this.currentGoal == BoatGoals.GO_IN_BOAT_DIRECTION) {
            Direction direction = this.following.getMotionDirection();
            BlockPos goTo = this.following.blockPosition().relative((Direction)direction, 10);
            this.mob.getNavigation().moveTo((double)goTo.getX(), (double)(goTo.getY() - 1), (double)goTo.getZ(), (double)1.0F);
            if (this.mob.distanceTo(this.following) > 12.0F) {
               this.timeToRecalcPath = 0;
               this.currentGoal = BoatGoals.GO_TO_BOAT;
            }
         }

      }
   }
}
