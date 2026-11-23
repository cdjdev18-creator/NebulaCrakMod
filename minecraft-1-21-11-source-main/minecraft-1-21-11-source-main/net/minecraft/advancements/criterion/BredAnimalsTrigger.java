package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public class BredAnimalsTrigger extends SimpleCriterionTrigger {
   public Codec codec() {
      return BredAnimalsTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final Animal parent, final Animal partner, final @Nullable AgeableMob child) {
      LootContext parentContext = EntityPredicate.createContext(player, parent);
      LootContext partnerContext = EntityPredicate.createContext(player, partner);
      LootContext childContext = child != null ? EntityPredicate.createContext(player, child) : null;
      this.trigger(player, (t) -> t.matches(parentContext, partnerContext, childContext));
   }

   public static record TriggerInstance(Optional player, Optional parent, Optional partner, Optional child) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec CODEC = RecordCodecBuilder.create((i) -> i.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("parent").forGetter(TriggerInstance::parent), EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("partner").forGetter(TriggerInstance::partner), EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("child").forGetter(TriggerInstance::child)).apply(i, TriggerInstance::new));

      public static Criterion bredAnimals() {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion bredAnimals(final EntityPredicate.Builder child) {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(child))));
      }

      public static Criterion bredAnimals(final Optional parent1, final Optional parent2, final Optional child) {
         return CriteriaTriggers.BRED_ANIMALS.createCriterion(new TriggerInstance(Optional.empty(), EntityPredicate.wrap(parent1), EntityPredicate.wrap(parent2), EntityPredicate.wrap(child)));
      }

      public boolean matches(final LootContext parent, final LootContext partner, final @Nullable LootContext child) {
         if (!this.child.isPresent() || child != null && ((ContextAwarePredicate)this.child.get()).matches(child)) {
            return matches(this.parent, parent) && matches(this.partner, partner) || matches(this.parent, partner) && matches(this.partner, parent);
         } else {
            return false;
         }
      }

      private static boolean matches(final Optional predicate, final LootContext context) {
         return predicate.isEmpty() || ((ContextAwarePredicate)predicate.get()).matches(context);
      }

      public void validate(final CriterionValidator validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         validator.validateEntity(this.parent, "parent");
         validator.validateEntity(this.partner, "partner");
         validator.validateEntity(this.child, "child");
      }
   }
}
