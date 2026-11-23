package net.minecraft.world.entity.animal.wolf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.sounds.SoundEvent;

public record WolfSoundVariant(Holder ambientSound, Holder deathSound, Holder growlSound, Holder hurtSound, Holder pantSound, Holder whineSound) {
   public static final Codec DIRECT_CODEC = getWolfSoundVariantCodec();
   public static final Codec NETWORK_CODEC = getWolfSoundVariantCodec();
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;

   private static Codec getWolfSoundVariantCodec() {
      return RecordCodecBuilder.create((i) -> i.group(SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(WolfSoundVariant::ambientSound), SoundEvent.CODEC.fieldOf("death_sound").forGetter(WolfSoundVariant::deathSound), SoundEvent.CODEC.fieldOf("growl_sound").forGetter(WolfSoundVariant::growlSound), SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(WolfSoundVariant::hurtSound), SoundEvent.CODEC.fieldOf("pant_sound").forGetter(WolfSoundVariant::pantSound), SoundEvent.CODEC.fieldOf("whine_sound").forGetter(WolfSoundVariant::whineSound)).apply(i, WolfSoundVariant::new));
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.WOLF_SOUND_VARIANT);
      STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.WOLF_SOUND_VARIANT);
   }
}
