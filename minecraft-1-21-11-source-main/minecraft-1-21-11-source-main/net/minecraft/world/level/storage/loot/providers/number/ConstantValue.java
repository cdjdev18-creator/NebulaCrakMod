package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;

public record ConstantValue(float value) implements NumberProvider {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantValue::value)).apply(i, ConstantValue::new));
   public static final Codec INLINE_CODEC;

   public LootNumberProviderType getType() {
      return NumberProviders.CONSTANT;
   }

   public float getFloat(final LootContext random) {
      return this.value;
   }

   public static ConstantValue exactly(final float value) {
      return new ConstantValue(value);
   }

   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         return Float.compare(((ConstantValue)o).value, this.value) == 0;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
   }

   static {
      INLINE_CODEC = Codec.FLOAT.xmap(ConstantValue::new, ConstantValue::value);
   }
}
