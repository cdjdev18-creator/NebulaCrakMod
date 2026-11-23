package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class NbtProviders {
   private static final Codec TYPED_CODEC;
   public static final Codec CODEC;
   public static final LootNbtProviderType STORAGE;
   public static final LootNbtProviderType CONTEXT;

   private static LootNbtProviderType register(final String name, final MapCodec codec) {
      return (LootNbtProviderType)Registry.register(BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE, (Identifier)Identifier.withDefaultNamespace(name), new LootNbtProviderType(codec));
   }

   static {
      TYPED_CODEC = BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE.byNameCodec().dispatch(NbtProvider::getType, LootNbtProviderType::codec);
      CODEC = Codec.lazyInitialized(() -> Codec.either(ContextNbtProvider.INLINE_CODEC, TYPED_CODEC).xmap(Either::unwrap, (provider) -> {
            Either var10000;
            if (provider instanceof ContextNbtProvider context) {
               var10000 = Either.left(context);
            } else {
               var10000 = Either.right(provider);
            }

            return var10000;
         }));
      STORAGE = register("storage", StorageNbtProvider.CODEC);
      CONTEXT = register("context", ContextNbtProvider.MAP_CODEC);
   }
}
