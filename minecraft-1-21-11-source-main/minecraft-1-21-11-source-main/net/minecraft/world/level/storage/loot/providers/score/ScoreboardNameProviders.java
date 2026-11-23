package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ScoreboardNameProviders {
   private static final Codec TYPED_CODEC;
   public static final Codec CODEC;
   public static final LootScoreProviderType FIXED;
   public static final LootScoreProviderType CONTEXT;

   private static LootScoreProviderType register(final String name, final MapCodec codec) {
      return (LootScoreProviderType)Registry.register(BuiltInRegistries.LOOT_SCORE_PROVIDER_TYPE, (Identifier)Identifier.withDefaultNamespace(name), new LootScoreProviderType(codec));
   }

   static {
      TYPED_CODEC = BuiltInRegistries.LOOT_SCORE_PROVIDER_TYPE.byNameCodec().dispatch(ScoreboardNameProvider::getType, LootScoreProviderType::codec);
      CODEC = Codec.lazyInitialized(() -> Codec.either(ContextScoreboardNameProvider.INLINE_CODEC, TYPED_CODEC).xmap(Either::unwrap, (provider) -> {
            Either var10000;
            if (provider instanceof ContextScoreboardNameProvider context) {
               var10000 = Either.left(context);
            } else {
               var10000 = Either.right(provider);
            }

            return var10000;
         }));
      FIXED = register("fixed", FixedScoreboardNameProvider.CODEC);
      CONTEXT = register("context", ContextScoreboardNameProvider.CODEC);
   }
}
