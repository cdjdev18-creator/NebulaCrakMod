package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeTrack(AttributeModifier modifier, KeyframeTrack argumentTrack) {
   public static Codec createCodec(final EnvironmentAttribute attribute) {
      MapCodec<AttributeModifier<Value, ?>> modifierCodec = attribute.type().modifierCodec().optionalFieldOf("modifier", AttributeModifier.override());
      return modifierCodec.dispatch(AttributeTrack::modifier, Util.memoize((Function)((modifier) -> createCodecWithModifier(attribute, modifier))));
   }

   private static MapCodec createCodecWithModifier(final EnvironmentAttribute attribute, final AttributeModifier modifier) {
      return KeyframeTrack.mapCodec(modifier.argumentCodec(attribute)).xmap((track) -> new AttributeTrack(modifier, track), AttributeTrack::argumentTrack);
   }

   public AttributeTrackSampler bakeSampler(final EnvironmentAttribute attribute, final Optional periodTicks, final LongSupplier dayTimeGetter) {
      return new AttributeTrackSampler(periodTicks, this.modifier, this.argumentTrack, this.modifier.argumentKeyframeLerp(attribute), dayTimeGetter);
   }

   public static DataResult validatePeriod(final AttributeTrack track, final int periodTicks) {
      return KeyframeTrack.validatePeriod(track.argumentTrack(), periodTicks).map((ignored) -> track);
   }
}
