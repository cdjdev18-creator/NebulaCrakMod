package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Brain {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Supplier codec;
   private static final int SCHEDULE_UPDATE_DELAY = 20;
   private final Map memories = Maps.newHashMap();
   private final Map sensors = Maps.newLinkedHashMap();
   private final Map availableBehaviorsByPriority = Maps.newTreeMap();
   private @Nullable EnvironmentAttribute schedule;
   private final Map activityRequirements = Maps.newHashMap();
   private final Map activityMemoriesToEraseWhenStopped = Maps.newHashMap();
   private Set coreActivities = Sets.newHashSet();
   private final Set activeActivities = Sets.newHashSet();
   private Activity defaultActivity;
   private long lastScheduleUpdate;

   public static Provider provider(final Collection memoryTypes, final Collection sensorTypes) {
      return new Provider(memoryTypes, sensorTypes);
   }

   public static Codec codec(final Collection memoryTypes, final Collection sensorTypes) {
      final MutableObject<Codec<Brain<E>>> codecReference = new MutableObject();
      codecReference.setValue((new MapCodec() {
         public Stream keys(final DynamicOps ops) {
            return memoryTypes.stream().flatMap((t) -> t.getCodec().map((c) -> BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(t)).stream()).map((l) -> ops.createString(l.toString()));
         }

         public DataResult decode(final DynamicOps ops, final MapLike input) {
            MutableObject<DataResult<ImmutableList.Builder<MemoryValue<?>>>> result = new MutableObject(DataResult.success(ImmutableList.builder()));
            input.entries().forEach((pair) -> {
               DataResult<MemoryModuleType<?>> typeResult = BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().parse(ops, pair.getFirst());
               DataResult<? extends MemoryValue<?>> entryResult = typeResult.flatMap((type) -> this.captureRead(type, ops, pair.getSecond()));
               result.setValue(((DataResult)result.get()).apply2(ImmutableList.Builder::add, entryResult));
            });
            DataResult var10000 = (DataResult)result.get();
            Logger var10001 = Brain.LOGGER;
            Objects.requireNonNull(var10001);
            ImmutableList<MemoryValue<?>> memories = (ImmutableList)var10000.resultOrPartial(var10001::error).map(ImmutableList.Builder::build).orElseGet(ImmutableList::of);
            return DataResult.success(new Brain(memoryTypes, sensorTypes, memories, codecReference));
         }

         private DataResult captureRead(final MemoryModuleType type, final DynamicOps ops, final Object input) {
            return ((DataResult)type.getCodec().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "No codec for memory: " + String.valueOf(type)))).flatMap((c) -> c.parse(ops, input)).map((v) -> new MemoryValue(type, Optional.of(v)));
         }

         public RecordBuilder encode(final Brain input, final DynamicOps ops, final RecordBuilder prefix) {
            input.memories().forEach((m) -> m.serialize(ops, prefix));
            return prefix;
         }
      }).fieldOf("memories").codec());
      return (Codec)codecReference.get();
   }

   public Brain(final Collection memoryTypes, final Collection sensorTypes, final ImmutableList memories, final Supplier codec) {
      this.defaultActivity = Activity.IDLE;
      this.lastScheduleUpdate = -9999L;
      this.codec = codec;

      for(MemoryModuleType memoryType : memoryTypes) {
         this.memories.put(memoryType, Optional.empty());
      }

      for(SensorType sensorType : sensorTypes) {
         this.sensors.put(sensorType, sensorType.create());
      }

      for(Sensor sensor : this.sensors.values()) {
         for(MemoryModuleType type : sensor.requires()) {
            this.memories.put(type, Optional.empty());
         }
      }

      UnmodifiableIterator var11 = memories.iterator();

      while(var11.hasNext()) {
         MemoryValue<?> memory = (MemoryValue)var11.next();
         memory.setMemoryInternal(this);
      }

   }

   public DataResult serializeStart(final DynamicOps ops) {
      return ((Codec)this.codec.get()).encodeStart(ops, this);
   }

   private Stream memories() {
      return this.memories.entrySet().stream().map((e) -> Brain.MemoryValue.createUnchecked((MemoryModuleType)e.getKey(), (Optional)e.getValue()));
   }

   public boolean hasMemoryValue(final MemoryModuleType type) {
      return this.checkMemory(type, MemoryStatus.VALUE_PRESENT);
   }

   public void clearMemories() {
      this.memories.keySet().forEach((key) -> this.memories.put(key, Optional.empty()));
   }

   public void eraseMemory(final MemoryModuleType type) {
      this.setMemory(type, Optional.empty());
   }

   public void setMemory(final MemoryModuleType type, final @Nullable Object value) {
      this.setMemory(type, Optional.ofNullable(value));
   }

   public void setMemoryWithExpiry(final MemoryModuleType type, final Object value, final long timeToLive) {
      this.setMemoryInternal(type, Optional.of(ExpirableValue.of(value, timeToLive)));
   }

   public void setMemory(final MemoryModuleType type, final Optional optionalValue) {
      this.setMemoryInternal(type, optionalValue.map(ExpirableValue::of));
   }

   private void setMemoryInternal(final MemoryModuleType type, final Optional optionalExpirableValue) {
      if (this.memories.containsKey(type)) {
         if (optionalExpirableValue.isPresent() && this.isEmptyCollection(((ExpirableValue)optionalExpirableValue.get()).getValue())) {
            this.eraseMemory(type);
         } else {
            this.memories.put(type, optionalExpirableValue);
         }
      }

   }

   public Optional getMemory(final MemoryModuleType type) {
      Optional<? extends ExpirableValue<?>> expirableValue = (Optional)this.memories.get(type);
      if (expirableValue == null) {
         throw new IllegalStateException("Unregistered memory fetched: " + String.valueOf(type));
      } else {
         return expirableValue.map(ExpirableValue::getValue);
      }
   }

   public @Nullable Optional getMemoryInternal(final MemoryModuleType type) {
      Optional<? extends ExpirableValue<?>> expirableValue = (Optional)this.memories.get(type);
      return expirableValue == null ? null : expirableValue.map(ExpirableValue::getValue);
   }

   public long getTimeUntilExpiry(final MemoryModuleType type) {
      Optional<? extends ExpirableValue<?>> memory = (Optional)this.memories.get(type);
      return (Long)memory.map(ExpirableValue::getTimeToLive).orElse(0L);
   }

   /** @deprecated */
   @Deprecated
   @VisibleForDebug
   public Map getMemories() {
      return this.memories;
   }

   public boolean isMemoryValue(final MemoryModuleType memoryType, final Object value) {
      return !this.hasMemoryValue(memoryType) ? false : this.getMemory(memoryType).filter((memory) -> memory.equals(value)).isPresent();
   }

   public boolean checkMemory(final MemoryModuleType type, final MemoryStatus status) {
      Optional<? extends ExpirableValue<?>> optionalExpirableValue = (Optional)this.memories.get(type);
      if (optionalExpirableValue == null) {
         return false;
      } else {
         return status == MemoryStatus.REGISTERED || status == MemoryStatus.VALUE_PRESENT && optionalExpirableValue.isPresent() || status == MemoryStatus.VALUE_ABSENT && optionalExpirableValue.isEmpty();
      }
   }

   public void setSchedule(final EnvironmentAttribute schedule) {
      this.schedule = schedule;
   }

   public void setCoreActivities(final Set activities) {
      this.coreActivities = activities;
   }

   /** @deprecated */
   @Deprecated
   @VisibleForDebug
   public Set getActiveActivities() {
      return this.activeActivities;
   }

   /** @deprecated */
   @Deprecated
   @VisibleForDebug
   public List getRunningBehaviors() {
      List<BehaviorControl<? super E>> runningBehaviours = new ObjectArrayList();

      for(Map behavioursByActivities : this.availableBehaviorsByPriority.values()) {
         for(Set behaviors : behavioursByActivities.values()) {
            for(BehaviorControl behavior : behaviors) {
               if (behavior.getStatus() == Behavior.Status.RUNNING) {
                  runningBehaviours.add(behavior);
               }
            }
         }
      }

      return runningBehaviours;
   }

   public void useDefaultActivity() {
      this.setActiveActivity(this.defaultActivity);
   }

   public Optional getActiveNonCoreActivity() {
      for(Activity activity : this.activeActivities) {
         if (!this.coreActivities.contains(activity)) {
            return Optional.of(activity);
         }
      }

      return Optional.empty();
   }

   public void setActiveActivityIfPossible(final Activity activity) {
      if (this.activityRequirementsAreMet(activity)) {
         this.setActiveActivity(activity);
      } else {
         this.useDefaultActivity();
      }

   }

   private void setActiveActivity(final Activity activity) {
      if (!this.isActive(activity)) {
         this.eraseMemoriesForOtherActivitesThan(activity);
         this.activeActivities.clear();
         this.activeActivities.addAll(this.coreActivities);
         this.activeActivities.add(activity);
      }
   }

   private void eraseMemoriesForOtherActivitesThan(final Activity activity) {
      for(Activity oldActivity : this.activeActivities) {
         if (oldActivity != activity) {
            Set<MemoryModuleType<?>> memoryModuleTypes = (Set)this.activityMemoriesToEraseWhenStopped.get(oldActivity);
            if (memoryModuleTypes != null) {
               for(MemoryModuleType memoryModuleType : memoryModuleTypes) {
                  this.eraseMemory(memoryModuleType);
               }
            }
         }
      }

   }

   public void updateActivityFromSchedule(final EnvironmentAttributeSystem environmentAttributes, final long gameTime, final Vec3 pos) {
      if (gameTime - this.lastScheduleUpdate > 20L) {
         this.lastScheduleUpdate = gameTime;
         Activity scheduledActivity = this.schedule != null ? (Activity)environmentAttributes.getValue(this.schedule, pos) : Activity.IDLE;
         if (!this.activeActivities.contains(scheduledActivity)) {
            this.setActiveActivityIfPossible(scheduledActivity);
         }
      }

   }

   public void setActiveActivityToFirstValid(final List activities) {
      for(Activity activity : activities) {
         if (this.activityRequirementsAreMet(activity)) {
            this.setActiveActivity(activity);
            break;
         }
      }

   }

   public void setDefaultActivity(final Activity activity) {
      this.defaultActivity = activity;
   }

   public void addActivity(final Activity activity, final int priorityOfFirstBehavior, final ImmutableList behaviorList) {
      this.addActivity(activity, this.createPriorityPairs(priorityOfFirstBehavior, behaviorList));
   }

   public void addActivityAndRemoveMemoryWhenStopped(final Activity activity, final int priorityOfFirstBehavior, final ImmutableList behaviorList, final MemoryModuleType memoryThatMustHaveValueAndWillBeErasedAfter) {
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions = ImmutableSet.of(Pair.of(memoryThatMustHaveValueAndWillBeErasedAfter, MemoryStatus.VALUE_PRESENT));
      Set<MemoryModuleType<?>> memoriesToEraseWhenStopped = ImmutableSet.of(memoryThatMustHaveValueAndWillBeErasedAfter);
      this.addActivityAndRemoveMemoriesWhenStopped(activity, this.createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions, memoriesToEraseWhenStopped);
   }

   public void addActivity(final Activity activity, final ImmutableList behaviorPriorityPairs) {
      this.addActivityAndRemoveMemoriesWhenStopped(activity, behaviorPriorityPairs, ImmutableSet.of(), Sets.newHashSet());
   }

   public void addActivityWithConditions(final Activity activity, final int priorityOfFirstBehavior, final ImmutableList behaviorList, final Set conditions) {
      this.addActivityWithConditions(activity, this.createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions);
   }

   public void addActivityWithConditions(final Activity activity, final ImmutableList behaviorPriorityPairs, final Set conditions) {
      this.addActivityAndRemoveMemoriesWhenStopped(activity, behaviorPriorityPairs, conditions, Sets.newHashSet());
   }

   public void addActivityAndRemoveMemoriesWhenStopped(final Activity activity, final ImmutableList behaviorPriorityPairs, final Set conditions, final Set memoriesToEraseWhenStopped) {
      this.activityRequirements.put(activity, conditions);
      if (!memoriesToEraseWhenStopped.isEmpty()) {
         this.activityMemoriesToEraseWhenStopped.put(activity, memoriesToEraseWhenStopped);
      }

      UnmodifiableIterator var5 = behaviorPriorityPairs.iterator();

      while(var5.hasNext()) {
         Pair<Integer, ? extends BehaviorControl<? super E>> pair = (Pair)var5.next();
         ((Set)((Map)this.availableBehaviorsByPriority.computeIfAbsent((Integer)pair.getFirst(), (key) -> Maps.newHashMap())).computeIfAbsent(activity, (key) -> Sets.newLinkedHashSet())).add((BehaviorControl)pair.getSecond());
      }

   }

   @VisibleForTesting
   public void removeAllBehaviors() {
      this.availableBehaviorsByPriority.clear();
   }

   public boolean isActive(final Activity activity) {
      return this.activeActivities.contains(activity);
   }

   public Brain copyWithoutBehaviors() {
      Brain<E> brain = new Brain(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codec);

      for(Map.Entry memoryEntry : this.memories.entrySet()) {
         MemoryModuleType<?> memoryModuleType = (MemoryModuleType)memoryEntry.getKey();
         if (((Optional)memoryEntry.getValue()).isPresent()) {
            brain.memories.put(memoryModuleType, (Optional)memoryEntry.getValue());
         }
      }

      return brain;
   }

   public void tick(final ServerLevel level, final LivingEntity body) {
      this.forgetOutdatedMemories();
      this.tickSensors(level, body);
      this.startEachNonRunningBehavior(level, body);
      this.tickEachRunningBehavior(level, body);
   }

   private void tickSensors(final ServerLevel level, final LivingEntity body) {
      for(Sensor sensor : this.sensors.values()) {
         sensor.tick(level, body);
      }

   }

   private void forgetOutdatedMemories() {
      for(Map.Entry entry : this.memories.entrySet()) {
         if (((Optional)entry.getValue()).isPresent()) {
            ExpirableValue<?> memory = (ExpirableValue)((Optional)entry.getValue()).get();
            if (memory.hasExpired()) {
               this.eraseMemory((MemoryModuleType)entry.getKey());
            }

            memory.tick();
         }
      }

   }

   public void stopAll(final ServerLevel level, final LivingEntity body) {
      long timestamp = body.level().getGameTime();

      for(BehaviorControl behavior : this.getRunningBehaviors()) {
         behavior.doStop(level, body, timestamp);
      }

   }

   private void startEachNonRunningBehavior(final ServerLevel level, final LivingEntity body) {
      long time = level.getGameTime();

      for(Map behavioursByActivities : this.availableBehaviorsByPriority.values()) {
         for(Map.Entry behavioursForActivity : behavioursByActivities.entrySet()) {
            Activity activity = (Activity)behavioursForActivity.getKey();
            if (this.activeActivities.contains(activity)) {
               for(BehaviorControl behavior : (Set)behavioursForActivity.getValue()) {
                  if (behavior.getStatus() == Behavior.Status.STOPPED) {
                     behavior.tryStart(level, body, time);
                  }
               }
            }
         }
      }

   }

   private void tickEachRunningBehavior(final ServerLevel level, final LivingEntity body) {
      long timestamp = level.getGameTime();

      for(BehaviorControl behavior : this.getRunningBehaviors()) {
         behavior.tickOrStop(level, body, timestamp);
      }

   }

   private boolean activityRequirementsAreMet(final Activity activity) {
      if (!this.activityRequirements.containsKey(activity)) {
         return false;
      } else {
         for(Pair memoryRequirement : (Set)this.activityRequirements.get(activity)) {
            MemoryModuleType<?> memoryType = (MemoryModuleType)memoryRequirement.getFirst();
            MemoryStatus memoryStatus = (MemoryStatus)memoryRequirement.getSecond();
            if (!this.checkMemory(memoryType, memoryStatus)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isEmptyCollection(final Object object) {
      return object instanceof Collection && ((Collection)object).isEmpty();
   }

   ImmutableList createPriorityPairs(final int priorityOfFirstBehavior, final ImmutableList behaviorList) {
      int nextPrio = priorityOfFirstBehavior;
      ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super E>>> listBuilder = ImmutableList.builder();
      UnmodifiableIterator var5 = behaviorList.iterator();

      while(var5.hasNext()) {
         BehaviorControl<? super E> behavior = (BehaviorControl)var5.next();
         listBuilder.add(Pair.of(nextPrio++, behavior));
      }

      return listBuilder.build();
   }

   public boolean isBrainDead() {
      return this.memories.isEmpty() && this.sensors.isEmpty() && this.availableBehaviorsByPriority.isEmpty();
   }

   public static final class Provider {
      private final Collection memoryTypes;
      private final Collection sensorTypes;
      private final Codec codec;

      private Provider(final Collection memoryTypes, final Collection sensorTypes) {
         this.memoryTypes = memoryTypes;
         this.sensorTypes = sensorTypes;
         this.codec = Brain.codec(memoryTypes, sensorTypes);
      }

      public Brain makeBrain(final Dynamic input) {
         DataResult var10000 = this.codec.parse(input);
         Logger var10001 = Brain.LOGGER;
         Objects.requireNonNull(var10001);
         return (Brain)var10000.resultOrPartial(var10001::error).orElseGet(() -> new Brain(this.memoryTypes, this.sensorTypes, ImmutableList.of(), () -> this.codec));
      }
   }

   private static final class MemoryValue {
      private final MemoryModuleType type;
      private final Optional value;

      private static MemoryValue createUnchecked(final MemoryModuleType type, final Optional value) {
         return new MemoryValue(type, value);
      }

      private MemoryValue(final MemoryModuleType type, final Optional value) {
         this.type = type;
         this.value = value;
      }

      private void setMemoryInternal(final Brain brain) {
         brain.setMemoryInternal(this.type, this.value);
      }

      public void serialize(final DynamicOps ops, final RecordBuilder builder) {
         this.type.getCodec().ifPresent((codec) -> this.value.ifPresent((v) -> builder.add(BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().encodeStart(ops, this.type), codec.encodeStart(ops, v))));
      }
   }
}
