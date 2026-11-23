package net.minecraft.world.entity.animal.feline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

public record CatVariant(ClientAsset.ResourceTexture assetInfo, SpawnPrioritySelectors spawnConditions) implements PriorityProvider {
   public static final Codec DIRECT_CODEC = RecordCodecBuilder.create((i) -> i.group(ClientAsset.ResourceTexture.DEFAULT_FIELD_CODEC.forGetter(CatVariant::assetInfo), SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(CatVariant::spawnConditions)).apply(i, CatVariant::new));
   public static final Codec NETWORK_CODEC = RecordCodecBuilder.create((i) -> i.group(ClientAsset.ResourceTexture.DEFAULT_FIELD_CODEC.forGetter(CatVariant::assetInfo)).apply(i, CatVariant::new));
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;

   private CatVariant(final ClientAsset.ResourceTexture assetInfo) {
      this(assetInfo, SpawnPrioritySelectors.EMPTY);
   }

   public List selectors() {
      return this.spawnConditions.selectors();
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.CAT_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.CAT_VARIANT);
   }
}
