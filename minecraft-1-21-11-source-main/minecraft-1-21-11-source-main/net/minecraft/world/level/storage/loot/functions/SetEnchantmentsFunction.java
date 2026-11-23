package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(i.group(Codec.unboundedMap(Enchantment.CODEC, NumberProviders.CODEC).optionalFieldOf("enchantments", Map.of()).forGetter((f) -> f.enchantments), Codec.BOOL.fieldOf("add").orElse(false).forGetter((f) -> f.add))).apply(i, SetEnchantmentsFunction::new));
   private final Map enchantments;
   private final boolean add;

   private SetEnchantmentsFunction(final List predicates, final Map enchantments, final boolean add) {
      super(predicates);
      this.enchantments = Map.copyOf(enchantments);
      this.add = add;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ENCHANTMENTS;
   }

   public Set getReferencedContextParams() {
      return (Set)this.enchantments.values().stream().flatMap((m) -> m.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack run(ItemStack itemStack, final LootContext context) {
      if (itemStack.is(Items.BOOK)) {
         itemStack = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
      }

      EnchantmentHelper.updateEnchantments(itemStack, (enchantments) -> {
         if (this.add) {
            this.enchantments.forEach((enchantment, levelProvider) -> enchantments.set(enchantment, Mth.clamp(enchantments.getLevel(enchantment) + levelProvider.getInt(context), 0, 255)));
         } else {
            this.enchantments.forEach((enchantment, levelProvider) -> enchantments.set(enchantment, Mth.clamp(levelProvider.getInt(context), 0, 255)));
         }

      });
      return itemStack;
   }

   public static class Builder extends LootItemConditionalFunction.Builder {
      private final ImmutableMap.Builder enchantments;
      private final boolean add;

      public Builder() {
         this(false);
      }

      public Builder(final boolean add) {
         this.enchantments = ImmutableMap.builder();
         this.add = add;
      }

      protected Builder getThis() {
         return this;
      }

      public Builder withEnchantment(final Holder enchantment, final NumberProvider levelProvider) {
         this.enchantments.put(enchantment, levelProvider);
         return this;
      }

      public LootItemFunction build() {
         return new SetEnchantmentsFunction(this.getConditions(), this.enchantments.build(), this.add);
      }
   }
}
