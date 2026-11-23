package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class EnchantWithLevelsFunction extends LootItemConditionalFunction {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(i.group(NumberProviders.CODEC.fieldOf("levels").forGetter((f) -> f.levels), RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter((f) -> f.options))).apply(i, EnchantWithLevelsFunction::new));
   private final NumberProvider levels;
   private final Optional options;

   private EnchantWithLevelsFunction(final List predicates, final NumberProvider levels, final Optional options) {
      super(predicates);
      this.levels = levels;
      this.options = options;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.ENCHANT_WITH_LEVELS;
   }

   public Set getReferencedContextParams() {
      return this.levels.getReferencedContextParams();
   }

   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      RandomSource random = context.getRandom();
      RegistryAccess registryAccess = context.getLevel().registryAccess();
      return EnchantmentHelper.enchantItem(random, itemStack, this.levels.getInt(context), registryAccess, this.options);
   }

   public static Builder enchantWithLevels(final HolderLookup.Provider registries, final NumberProvider levels) {
      return (new Builder(levels)).fromOptions(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder {
      private final NumberProvider levels;
      private Optional options = Optional.empty();

      public Builder(final NumberProvider levels) {
         this.levels = levels;
      }

      protected Builder getThis() {
         return this;
      }

      public Builder fromOptions(final HolderSet tag) {
         this.options = Optional.of(tag);
         return this;
      }

      public LootItemFunction build() {
         return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.options);
      }
   }
}
