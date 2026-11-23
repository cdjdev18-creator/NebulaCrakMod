package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger {
   public Codec codec() {
      return CuredZombieVillagerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final Zombie zombie, final Villager villager) {
      LootContext zombieContext = EntityPredicate.createContext(player, zombie);
      LootContext villagerContext = EntityPredicate.createContext(player, villager);
      this.trigger(player, (t) -> t.matches(zombieContext, villagerContext));
   }

   public static record TriggerInstance(Optional player, Optional zombie, Optional villager) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec CODEC = RecordCodecBuilder.create((i) -> i.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("zombie").forGetter(TriggerInstance::zombie), EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("villager").forGetter(TriggerInstance::villager)).apply(i, TriggerInstance::new));

      public static Criterion curedZombieVillager() {
         return CriteriaTriggers.CURED_ZOMBIE_VILLAGER.createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public boolean matches(final LootContext zombie, final LootContext villager) {
         if (this.zombie.isPresent() && !((ContextAwarePredicate)this.zombie.get()).matches(zombie)) {
            return false;
         } else {
            return !this.villager.isPresent() || ((ContextAwarePredicate)this.villager.get()).matches(villager);
         }
      }

      public void validate(final CriterionValidator validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         validator.validateEntity(this.zombie, "zombie");
         validator.validateEntity(this.villager, "villager");
      }
   }
}
