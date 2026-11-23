package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetOminousBottleAmplifierFunction extends LootItemConditionalFunction {
   static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> commonFields(i).and(NumberProviders.CODEC.fieldOf("amplifier").forGetter((f) -> f.amplifierGenerator)).apply(i, SetOminousBottleAmplifierFunction::new));
   private final NumberProvider amplifierGenerator;

   private SetOminousBottleAmplifierFunction(final List predicates, final NumberProvider amplifierGenerator) {
      super(predicates);
      this.amplifierGenerator = amplifierGenerator;
   }

   public Set getReferencedContextParams() {
      return this.amplifierGenerator.getReferencedContextParams();
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_OMINOUS_BOTTLE_AMPLIFIER;
   }

   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      int amplifierValue = Mth.clamp(this.amplifierGenerator.getInt(context), 0, 4);
      itemStack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(amplifierValue));
      return itemStack;
   }

   public NumberProvider amplifier() {
      return this.amplifierGenerator;
   }

   public static LootItemConditionalFunction.Builder setAmplifier(final NumberProvider generator) {
      return simpleBuilder((conditions) -> new SetOminousBottleAmplifierFunction(conditions, generator));
   }
}
