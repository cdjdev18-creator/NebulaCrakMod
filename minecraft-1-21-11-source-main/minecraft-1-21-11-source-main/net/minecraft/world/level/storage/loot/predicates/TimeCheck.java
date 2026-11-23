package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;

public record TimeCheck(Optional period, IntRange value) implements LootItemCondition {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Codec.LONG.optionalFieldOf("period").forGetter(TimeCheck::period), IntRange.CODEC.fieldOf("value").forGetter(TimeCheck::value)).apply(i, TimeCheck::new));

   public LootItemConditionType getType() {
      return LootItemConditions.TIME_CHECK;
   }

   public Set getReferencedContextParams() {
      return this.value.getReferencedContextParams();
   }

   public boolean test(final LootContext context) {
      ServerLevel level = context.getLevel();
      long time = level.getDayTime();
      if (this.period.isPresent()) {
         time %= (Long)this.period.get();
      }

      return this.value.test(context, (int)time);
   }

   public static Builder time(final IntRange period) {
      return new Builder(period);
   }

   public static class Builder implements LootItemCondition.Builder {
      private Optional period = Optional.empty();
      private final IntRange value;

      public Builder(final IntRange value) {
         this.value = value;
      }

      public Builder setPeriod(final long period) {
         this.period = Optional.of(period);
         return this;
      }

      public TimeCheck build() {
         return new TimeCheck(this.period, this.value);
      }
   }
}
