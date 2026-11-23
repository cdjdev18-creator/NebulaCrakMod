package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder(Either contents) {
   public EitherHolder(final Holder holder) {
      this(Either.left(holder));
   }

   public EitherHolder(final ResourceKey key) {
      this(Either.right(key));
   }

   public static Codec codec(final ResourceKey registry, final Codec holderCodec) {
      return Codec.either(holderCodec, ResourceKey.codec(registry).comapFlatMap((key) -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity())).xmap(EitherHolder::new, EitherHolder::contents);
   }

   public static StreamCodec streamCodec(final ResourceKey registry, final StreamCodec streamHolderCodec) {
      return StreamCodec.composite(ByteBufCodecs.either(streamHolderCodec, ResourceKey.streamCodec(registry)), EitherHolder::contents, EitherHolder::new);
   }

   public Optional unwrap(final Registry registry) {
      Either var10000 = this.contents;
      Function var10001 = (holder) -> Optional.of(holder.value());
      Objects.requireNonNull(registry);
      return (Optional)var10000.map(var10001, registry::getOptional);
   }

   public Optional unwrap(final HolderLookup.Provider provider) {
      return (Optional)this.contents.map(Optional::of, (key) -> provider.get(key).map((e) -> e));
   }

   public Optional key() {
      return (Optional)this.contents.map(Holder::unwrapKey, Optional::of);
   }
}
