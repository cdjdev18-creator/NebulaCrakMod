package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record ConditionReference(ResourceKey name) implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(ResourceKey.codec(Registries.PREDICATE).fieldOf("name").forGetter(ConditionReference::name)).apply(i, ConditionReference::new));

   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   public void validate(final ValidationContext context) {
      if (!context.allowsReferences()) {
         context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
      } else if (context.hasVisitedElement(this.name)) {
         context.reportProblem(new ValidationContext.RecursiveReferenceProblem(this.name));
      } else {
         LootItemCondition.super.validate(context);
         context.resolver().get(this.name).ifPresentOrElse((condition) -> ((LootItemCondition)condition.value()).validate(context.enterElement(new ProblemReporter.ElementReferencePathElement(this.name), this.name)), () -> context.reportProblem(new ValidationContext.MissingReferenceProblem(this.name)));
      }
   }

   public boolean test(final LootContext lootContext) {
      LootItemCondition condition = (LootItemCondition)lootContext.getResolver().get(this.name).map(Holder.Reference::value).orElse((Object)null);
      if (condition == null) {
         LOGGER.warn("Tried using unknown condition table called {}", this.name.identifier());
         return false;
      } else {
         LootContext.VisitedEntry<?> breadcrumb = LootContext.createVisitedEntry(condition);
         if (lootContext.pushVisitedElement(breadcrumb)) {
            boolean var4;
            try {
               var4 = condition.test(lootContext);
            } finally {
               lootContext.popVisitedElement(breadcrumb);
            }

            return var4;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(final ResourceKey name) {
      return () -> new ConditionReference(name);
   }
}
