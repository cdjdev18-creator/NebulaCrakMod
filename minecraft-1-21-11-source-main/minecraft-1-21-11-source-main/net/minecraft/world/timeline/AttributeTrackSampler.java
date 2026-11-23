package net.minecraft.world.timeline;

import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.KeyframeTrackSampler;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public class AttributeTrackSampler implements EnvironmentAttributeLayer.TimeBased {
   private final AttributeModifier modifier;
   private final KeyframeTrackSampler argumentSampler;
   private final LongSupplier dayTimeGetter;
   private int cachedTickId;
   private @Nullable Object cachedArgument;

   public AttributeTrackSampler(final Optional periodTicks, final AttributeModifier modifier, final KeyframeTrack argumentTrack, final LerpFunction argumentLerp, final LongSupplier dayTimeGetter) {
      this.modifier = modifier;
      this.dayTimeGetter = dayTimeGetter;
      this.argumentSampler = argumentTrack.bakeSampler(periodTicks, argumentLerp);
   }

   public Object applyTimeBased(final Object baseValue, final int cacheTickId) {
      if (this.cachedArgument == null || cacheTickId != this.cachedTickId) {
         this.cachedTickId = cacheTickId;
         this.cachedArgument = this.argumentSampler.sample(this.dayTimeGetter.getAsLong());
      }

      return this.modifier.apply(baseValue, this.cachedArgument);
   }
}
