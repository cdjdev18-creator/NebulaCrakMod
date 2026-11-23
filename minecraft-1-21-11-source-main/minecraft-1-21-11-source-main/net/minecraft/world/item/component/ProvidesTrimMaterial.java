package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public record ProvidesTrimMaterial(EitherHolder material) {
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;

   public ProvidesTrimMaterial(final Holder material) {
      this(new EitherHolder(material));
   }

   /** @deprecated */
   @Deprecated
   public ProvidesTrimMaterial(final ResourceKey material) {
      this(new EitherHolder(material));
   }

   public Optional unwrap(final HolderLookup.Provider registries) {
      return this.material.unwrap(registries);
   }

   static {
      CODEC = EitherHolder.codec(Registries.TRIM_MATERIAL, TrimMaterial.CODEC).xmap(ProvidesTrimMaterial::new, ProvidesTrimMaterial::material);
      STREAM_CODEC = EitherHolder.streamCodec(Registries.TRIM_MATERIAL, TrimMaterial.STREAM_CODEC).map(ProvidesTrimMaterial::new, ProvidesTrimMaterial::material);
   }
}
