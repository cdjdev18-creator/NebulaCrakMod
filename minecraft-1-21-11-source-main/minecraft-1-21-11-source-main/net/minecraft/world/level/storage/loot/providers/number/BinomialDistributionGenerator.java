package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n), NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)).apply(i, BinomialDistributionGenerator::new));

   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   public int getInt(final LootContext context) {
      int n = this.n.getInt(context);
      float p = this.p.getFloat(context);
      RandomSource random = context.getRandom();
      int result = 0;

      for(int i = 0; i < n; ++i) {
         if (random.nextFloat() < p) {
            ++result;
         }
      }

      return result;
   }

   public float getFloat(final LootContext context) {
      return (float)this.getInt(context);
   }

   public static BinomialDistributionGenerator binomial(final int n, final float p) {
      return new BinomialDistributionGenerator(ConstantValue.exactly((float)n), ConstantValue.exactly(p));
   }

   public Set getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }
}
