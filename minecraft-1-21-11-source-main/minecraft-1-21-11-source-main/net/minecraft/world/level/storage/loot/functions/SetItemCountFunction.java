package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetItemCountFunction extends LootItemConditionalFunction {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(i.group(NumberProviders.CODEC.fieldOf("count").forGetter((f) -> f.value), Codec.BOOL.fieldOf("add").orElse(false).forGetter((f) -> f.add))).apply(i, SetItemCountFunction::new));
   private final NumberProvider value;
   private final boolean add;

   private SetItemCountFunction(final List predicates, final NumberProvider value, final boolean add) {
      super(predicates);
      this.value = value;
      this.add = add;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_COUNT;
   }

   public Set getReferencedContextParams() {
      return this.value.getReferencedContextParams();
   }

   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      int base = this.add ? itemStack.getCount() : 0;
      itemStack.setCount(base + this.value.getInt(context));
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder setCount(final NumberProvider value) {
      return simpleBuilder((conditions) -> new SetItemCountFunction(conditions, value, false));
   }

   public static LootItemConditionalFunction.Builder setCount(final NumberProvider value, final boolean add) {
      return simpleBuilder((conditions) -> new SetItemCountFunction(conditions, value, add));
   }
}
