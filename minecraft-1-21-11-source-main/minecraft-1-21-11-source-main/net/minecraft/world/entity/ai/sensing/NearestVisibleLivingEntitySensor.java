package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public abstract class NearestVisibleLivingEntitySensor extends Sensor {
   protected abstract boolean isMatchingEntity(final ServerLevel level, LivingEntity body, LivingEntity mob);

   protected abstract MemoryModuleType getMemory();

   public Set requires() {
      return ImmutableSet.of(this.getMemory());
   }

   protected void doTick(final ServerLevel level, final LivingEntity body) {
      body.getBrain().setMemory(this.getMemory(), this.getNearestEntity(level, body));
   }

   private Optional getNearestEntity(final ServerLevel level, final LivingEntity body) {
      return this.getVisibleEntities(body).flatMap((livingEntities) -> livingEntities.findClosest((mob) -> this.isMatchingEntity(level, body, mob)));
   }

   protected Optional getVisibleEntities(final LivingEntity body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
