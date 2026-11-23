package net.minecraft.world.entity.monster;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Zoglin extends Monster implements HoglinBase {
   private static final EntityDataAccessor DATA_BABY_ID;
   private static final int MAX_HEALTH = 40;
   private static final int ATTACK_KNOCKBACK = 1;
   private static final float KNOCKBACK_RESISTANCE = 0.6F;
   private static final int ATTACK_DAMAGE = 6;
   private static final float BABY_ATTACK_DAMAGE = 0.5F;
   private static final int ATTACK_INTERVAL = 40;
   private static final int BABY_ATTACK_INTERVAL = 15;
   private static final int ATTACK_DURATION = 200;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.4F;
   private static final boolean DEFAULT_BABY = false;
   private int attackAnimationRemainingTicks;
   protected static final ImmutableList SENSOR_TYPES;
   protected static final ImmutableList MEMORY_TYPES;

   public Zoglin(final EntityType type, final Level level) {
      super(type, level);
      this.xpReward = 5;
   }

   protected Brain.Provider brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected Brain makeBrain(final Dynamic input) {
      Brain<Zoglin> brain = this.brainProvider().makeBrain(input);
      initCoreActivity(brain);
      initIdleActivity(brain);
      initFightActivity(brain);
      brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
      brain.setDefaultActivity(Activity.IDLE);
      brain.useDefaultActivity();
      return brain;
   }

   private static void initCoreActivity(final Brain brain) {
      brain.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   private static void initIdleActivity(final Brain brain) {
      brain.addActivity(Activity.IDLE, 10, ImmutableList.of(StartAttacking.create((level, zoglin) -> zoglin.findNearestValidAttackTarget(level)), SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)), new RunOne(ImmutableList.of(Pair.of(RandomStroll.stroll(0.4F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.4F, 3), 2), Pair.of(new DoNothing(30, 60), 1)))));
   }

   private static void initFightActivity(final Brain brain) {
      brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F), BehaviorBuilder.triggerIf(Zoglin::isAdult, MeleeAttack.create(40)), BehaviorBuilder.triggerIf(Zoglin::isBaby, MeleeAttack.create(15)), StopAttackingIfTargetInvalid.create()), MemoryModuleType.ATTACK_TARGET);
   }

   private Optional findNearestValidAttackTarget(final ServerLevel level) {
      return ((NearestVisibleLivingEntities)this.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty())).findClosest((target) -> this.isTargetable(level, target));
   }

   private boolean isTargetable(final ServerLevel level, final LivingEntity livingEntity) {
      EntityType<?> type = livingEntity.getType();
      return type != EntityType.ZOGLIN && type != EntityType.CREEPER && Sensor.isEntityAttackable(level, this, livingEntity);
   }

   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_BABY_ID, false);
   }

   public void onSyncedDataUpdated(final EntityDataAccessor accessor) {
      super.onSyncedDataUpdated(accessor);
      if (DATA_BABY_ID.equals(accessor)) {
         this.refreshDimensions();
      }

   }

   public @Nullable SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData) {
      if (level.getRandom().nextFloat() < 0.2F) {
         this.setBaby(true);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, (double)40.0F).add(Attributes.MOVEMENT_SPEED, (double)0.3F).add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F).add(Attributes.ATTACK_KNOCKBACK, (double)1.0F).add(Attributes.ATTACK_DAMAGE, (double)6.0F);
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   public boolean doHurtTarget(final ServerLevel level, final Entity target) {
      if (target instanceof LivingEntity entity) {
         this.attackAnimationRemainingTicks = 10;
         level.broadcastEntityEvent(this, (byte)4);
         this.makeSound(SoundEvents.ZOGLIN_ATTACK);
         return HoglinBase.hurtAndThrowTarget(level, this, entity);
      } else {
         return false;
      }
   }

   public boolean canBeLeashed() {
      return true;
   }

   protected void blockedByItem(final LivingEntity defender) {
      if (!this.isBaby()) {
         HoglinBase.throwTarget(this, defender);
      }

   }

   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      boolean wasHurt = super.hurtServer(level, source, damage);
      if (wasHurt) {
         Entity var6 = source.getEntity();
         if (var6 instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity)var6;
            if (this.canAttack(attacker) && !BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(this, attacker, (double)4.0F)) {
               this.setAttackTarget(attacker);
            }

            return true;
         }
      }

      return wasHurt;
   }

   private void setAttackTarget(final LivingEntity target) {
      this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      this.brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
   }

   public Brain getBrain() {
      return super.getBrain();
   }

   protected void updateActivity() {
      Activity oldActivity = (Activity)this.brain.getActiveNonCoreActivity().orElse((Object)null);
      this.brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      Activity newActivity = (Activity)this.brain.getActiveNonCoreActivity().orElse((Object)null);
      if (newActivity == Activity.FIGHT && oldActivity != Activity.FIGHT) {
         this.playAngrySound();
      }

      this.setAggressive(this.brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
   }

   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("zoglinBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      this.updateActivity();
   }

   public void setBaby(final boolean baby) {
      this.getEntityData().set(DATA_BABY_ID, baby);
      if (!this.level().isClientSide() && baby) {
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((double)0.5F);
      }

   }

   public boolean isBaby() {
      return (Boolean)this.getEntityData().get(DATA_BABY_ID);
   }

   public void aiStep() {
      if (this.attackAnimationRemainingTicks > 0) {
         --this.attackAnimationRemainingTicks;
      }

      super.aiStep();
   }

   public void handleEntityEvent(final byte id) {
      if (id == 4) {
         this.attackAnimationRemainingTicks = 10;
         this.makeSound(SoundEvents.ZOGLIN_ATTACK);
      } else {
         super.handleEntityEvent(id);
      }

   }

   public int getAttackAnimationRemainingTicks() {
      return this.attackAnimationRemainingTicks;
   }

   protected SoundEvent getAmbientSound() {
      if (this.level().isClientSide()) {
         return null;
      } else {
         return this.brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) ? SoundEvents.ZOGLIN_ANGRY : SoundEvents.ZOGLIN_AMBIENT;
      }
   }

   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.ZOGLIN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOGLIN_DEATH;
   }

   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      this.playSound(SoundEvents.ZOGLIN_STEP, 0.15F, 1.0F);
   }

   protected void playAngrySound() {
      this.makeSound(SoundEvents.ZOGLIN_ANGRY);
   }

   public @Nullable LivingEntity getTarget() {
      return this.getTargetFromBrain();
   }

   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("IsBaby", this.isBaby());
   }

   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setBaby(input.getBooleanOr("IsBaby", false));
   }

   static {
      DATA_BABY_ID = SynchedEntityData.defineId(Zoglin.class, EntityDataSerializers.BOOLEAN);
      SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
      MEMORY_TYPES = ImmutableList.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN);
   }
}
