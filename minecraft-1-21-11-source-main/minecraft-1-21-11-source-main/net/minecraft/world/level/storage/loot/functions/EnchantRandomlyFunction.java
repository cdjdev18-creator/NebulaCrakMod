package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(i.group(RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter((f) -> f.options), Codec.BOOL.optionalFieldOf("only_compatible", true).forGetter((f) -> f.onlyCompatible))).apply(i, EnchantRandomlyFunction::new));
   private final Optional options;
   private final boolean onlyCompatible;

   private EnchantRandomlyFunction(final List predicates, final Optional options, final boolean onlyCompatible) {
      super(predicates);
      this.options = options;
      this.onlyCompatible = onlyCompatible;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.ENCHANT_RANDOMLY;
   }

   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      RandomSource random = context.getRandom();
      boolean targetIsBook = itemStack.is(Items.BOOK);
      boolean shouldCheckCompatibility = !targetIsBook && this.onlyCompatible;
      Stream<Holder<Enchantment>> compatibleEnchantmentsStream = ((Stream)this.options.map(HolderSet::stream).orElseGet(() -> context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity()))).filter((candidate) -> !shouldCheckCompatibility || ((Enchantment)candidate.value()).canEnchant(itemStack));
      List<Holder<Enchantment>> compatibleEnchantments = compatibleEnchantmentsStream.toList();
      Optional<Holder<Enchantment>> enchantment = Util.getRandomSafe(compatibleEnchantments, random);
      if (enchantment.isEmpty()) {
         LOGGER.warn("Couldn't find a compatible enchantment for {}", itemStack);
         return itemStack;
      } else {
         return enchantItem(itemStack, (Holder)enchantment.get(), random);
      }
   }

   private static ItemStack enchantItem(ItemStack itemStack, final Holder enchantment, final RandomSource random) {
      int level = Mth.nextInt(random, ((Enchantment)enchantment.value()).getMinLevel(), ((Enchantment)enchantment.value()).getMaxLevel());
      if (itemStack.is(Items.BOOK)) {
         itemStack = new ItemStack(Items.ENCHANTED_BOOK);
      }

      itemStack.enchant(enchantment, level);
      return itemStack;
   }

   public static Builder randomEnchantment() {
      return new Builder();
   }

   public static Builder randomApplicableEnchantment(final HolderLookup.Provider registries) {
      return randomEnchantment().withOneOf(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder {
      private Optional options = Optional.empty();
      private boolean onlyCompatible = true;

      protected Builder getThis() {
         return this;
      }

      public Builder withEnchantment(final Holder enchantment) {
         this.options = Optional.of(HolderSet.direct(enchantment));
         return this;
      }

      public Builder withOneOf(final HolderSet enchantments) {
         this.options = Optional.of(enchantments);
         return this;
      }

      public Builder allowingIncompatibleEnchantments() {
         this.onlyCompatible = false;
         return this;
      }

      public LootItemFunction build() {
         return new EnchantRandomlyFunction(this.getConditions(), this.options, this.onlyCompatible);
      }
   }
}
