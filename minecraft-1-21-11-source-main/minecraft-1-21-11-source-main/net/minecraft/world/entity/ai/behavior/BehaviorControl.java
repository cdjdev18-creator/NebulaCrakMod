package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface BehaviorControl {
   Behavior.Status getStatus();

   boolean tryStart(ServerLevel level, LivingEntity body, long timestamp);

   void tickOrStop(ServerLevel level, LivingEntity body, long timestamp);

   void doStop(ServerLevel level, LivingEntity body, long timestamp);

   String debugString();
}
