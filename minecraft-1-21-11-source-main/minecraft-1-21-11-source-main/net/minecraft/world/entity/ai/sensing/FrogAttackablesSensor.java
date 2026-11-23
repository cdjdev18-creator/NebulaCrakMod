package net.minecraft.world.entity.ai.sensing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogAttackablesSensor extends NearestVisibleLivingEntitySensor {
   public static final float TARGET_DETECTION_DISTANCE = 10.0F;

   protected boolean isMatchingEntity(final ServerLevel level, final LivingEntity body, final LivingEntity mob) {
      return !body.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN) && Sensor.isEntityAttackable(level, body, mob) && Frog.canEat(mob) && !this.isUnreachableAttackTarget(body, mob) ? mob.closerThan(body, (double)10.0F) : false;
   }

   private boolean isUnreachableAttackTarget(final LivingEntity body, final LivingEntity mob) {
      List<UUID> unreachableAttackTargets = (List)body.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
      return unreachableAttackTargets.contains(mob.getUUID());
   }

   protected MemoryModuleType getMemory() {
      return MemoryModuleType.NEAREST_ATTACKABLE;
   }
}
