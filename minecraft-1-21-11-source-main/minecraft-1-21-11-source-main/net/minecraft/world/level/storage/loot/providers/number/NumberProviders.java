package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class NumberProviders {
   private static final Codec TYPED_CODEC;
   public static final Codec CODEC;
   public static final LootNumberProviderType CONSTANT;
   public static final LootNumberProviderType UNIFORM;
   public static final LootNumberProviderType BINOMIAL;
   public static final LootNumberProviderType SCORE;
   public static final LootNumberProviderType STORAGE;
   public static final LootNumberProviderType ENCHANTMENT_LEVEL;

   private static LootNumberProviderType register(final String name, final MapCodec codec) {
      return (LootNumberProviderType)Registry.register(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, (Identifier)Identifier.withDefaultNamespace(name), new LootNumberProviderType(codec));
   }

   static {
      TYPED_CODEC = BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE.byNameCodec().dispatch(NumberProvider::getType, LootNumberProviderType::codec);
      CODEC = Codec.lazyInitialized(() -> {
         Codec<NumberProvider> typedCodecWithFallback = Codec.withAlternative(TYPED_CODEC, UniformGenerator.CODEC.codec());
         return Codec.either(ConstantValue.INLINE_CODEC, typedCodecWithFallback).xmap(Either::unwrap, (provider) -> {
            Either var10000;
            if (provider instanceof ConstantValue constant) {
               var10000 = Either.left(constant);
            } else {
               var10000 = Either.right(provider);
            }

            return var10000;
         });
      });
      CONSTANT = register("constant", ConstantValue.CODEC);
      UNIFORM = register("uniform", UniformGenerator.CODEC);
      BINOMIAL = register("binomial", BinomialDistributionGenerator.CODEC);
      SCORE = register("score", ScoreboardValue.CODEC);
      STORAGE = register("storage", StorageValue.CODEC);
      ENCHANTMENT_LEVEL = register("enchantment_level", EnchantmentLevelProvider.CODEC);
   }
}
