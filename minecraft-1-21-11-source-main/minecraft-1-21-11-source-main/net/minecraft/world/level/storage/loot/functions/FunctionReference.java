package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(ResourceKey.codec(Registries.ITEM_MODIFIER).fieldOf("name").forGetter((f) -> f.name)).apply(i, FunctionReference::new));
   private final ResourceKey name;

   private FunctionReference(final List predicates, final ResourceKey name) {
      super(predicates);
      this.name = name;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.REFERENCE;
   }

   public void validate(final ValidationContext context) {
      if (!context.allowsReferences()) {
         context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
      } else if (context.hasVisitedElement(this.name)) {
         context.reportProblem(new ValidationContext.RecursiveReferenceProblem(this.name));
      } else {
         super.validate(context);
         context.resolver().get(this.name).ifPresentOrElse((function) -> ((LootItemFunction)function.value()).validate(context.enterElement(new ProblemReporter.ElementReferencePathElement(this.name), this.name)), () -> context.reportProblem(new ValidationContext.MissingReferenceProblem(this.name)));
      }
   }

   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      LootItemFunction function = (LootItemFunction)context.getResolver().get(this.name).map(Holder::value).orElse((Object)null);
      if (function == null) {
         LOGGER.warn("Unknown function: {}", this.name.identifier());
         return itemStack;
      } else {
         LootContext.VisitedEntry<?> breadcrumb = LootContext.createVisitedEntry(function);
         if (context.pushVisitedElement(breadcrumb)) {
            ItemStack var5;
            try {
               var5 = (ItemStack)function.apply(itemStack, context);
            } finally {
               context.popVisitedElement(breadcrumb);
            }

            return var5;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return itemStack;
         }
      }
   }

   public static LootItemConditionalFunction.Builder functionReference(final ResourceKey name) {
      return simpleBuilder((conditions) -> new FunctionReference(conditions, name));
   }
}
