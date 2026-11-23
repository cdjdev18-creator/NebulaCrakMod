package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record LootDataType(ResourceKey registryKey, Codec codec, Validator validator) {
   public static final LootDataType PREDICATE;
   public static final LootDataType MODIFIER;
   public static final LootDataType TABLE;

   public void runValidation(final ValidationContext rootContext, final ResourceKey key, final Object value) {
      this.validator.run(rootContext, key, value);
   }

   public static Stream values() {
      return Stream.of(PREDICATE, MODIFIER, TABLE);
   }

   private static Validator createSimpleValidator() {
      return (rootContext, key, value) -> value.validate(rootContext.enterElement(new ProblemReporter.RootElementPathElement(key), key));
   }

   private static Validator createLootTableValidator() {
      return (rootContext, key, value) -> value.validate(rootContext.setContextKeySet(value.getParamSet()).enterElement(new ProblemReporter.RootElementPathElement(key), key));
   }

   static {
      PREDICATE = new LootDataType(Registries.PREDICATE, LootItemCondition.DIRECT_CODEC, createSimpleValidator());
      MODIFIER = new LootDataType(Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC, createSimpleValidator());
      TABLE = new LootDataType(Registries.LOOT_TABLE, LootTable.DIRECT_CODEC, createLootTableValidator());
   }

   @FunctionalInterface
   public interface Validator {
      void run(ValidationContext rootContext, ResourceKey id, Object value);
   }
}
