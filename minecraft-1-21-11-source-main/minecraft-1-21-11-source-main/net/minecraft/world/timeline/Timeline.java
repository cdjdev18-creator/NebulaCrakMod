package net.minecraft.world.timeline;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.Level;

public class Timeline {
   public static final Codec CODEC;
   private static final Codec TRACKS_CODEC;
   public static final Codec DIRECT_CODEC;
   public static final Codec NETWORK_CODEC;
   private final Optional periodTicks;
   private final Map tracks;

   private static Timeline filterSyncableTracks(final Timeline timeline) {
      Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> syncableTracks = Map.copyOf(Maps.filterKeys(timeline.tracks, EnvironmentAttribute::isSyncable));
      return new Timeline(timeline.periodTicks, syncableTracks);
   }

   private Timeline(final Optional periodTicks, final Map tracks) {
      this.periodTicks = periodTicks;
      this.tracks = tracks;
   }

   private static DataResult validateInternal(final Timeline timeline) {
      if (timeline.periodTicks.isEmpty()) {
         return DataResult.success(timeline);
      } else {
         int periodTicks = (Integer)timeline.periodTicks.get();
         DataResult<Timeline> result = DataResult.success(timeline);

         for(AttributeTrack track : timeline.tracks.values()) {
            result = result.apply2stable((t, $) -> t, AttributeTrack.validatePeriod(track, periodTicks));
         }

         return result;
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public long getCurrentTicks(final Level level) {
      long totalTicks = this.getTotalTicks(level);
      return this.periodTicks.isEmpty() ? totalTicks : totalTicks % (long)(Integer)this.periodTicks.get();
   }

   public long getTotalTicks(final Level level) {
      return level.getDayTime();
   }

   public Optional periodTicks() {
      return this.periodTicks;
   }

   public Set attributes() {
      return this.tracks.keySet();
   }

   public AttributeTrackSampler createTrackSampler(final EnvironmentAttribute attribute, final LongSupplier dayTimeGetter) {
      AttributeTrack<Value, ?> track = (AttributeTrack)this.tracks.get(attribute);
      if (track == null) {
         throw new IllegalStateException("Timeline has no track for " + String.valueOf(attribute));
      } else {
         return track.bakeSampler(attribute, this.periodTicks, dayTimeGetter);
      }
   }

   static {
      CODEC = RegistryFixedCodec.create(Registries.TIMELINE);
      TRACKS_CODEC = Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(AttributeTrack::createCodec));
      DIRECT_CODEC = RecordCodecBuilder.create((i) -> i.group(ExtraCodecs.POSITIVE_INT.optionalFieldOf("period_ticks").forGetter((t) -> t.periodTicks), TRACKS_CODEC.optionalFieldOf("tracks", Map.of()).forGetter((t) -> t.tracks)).apply(i, Timeline::new)).validate(Timeline::validateInternal);
      NETWORK_CODEC = DIRECT_CODEC.xmap(Timeline::filterSyncableTracks, Timeline::filterSyncableTracks);
   }

   public static class Builder {
      private Optional periodTicks = Optional.empty();
      private final ImmutableMap.Builder tracks = ImmutableMap.builder();

      private Builder() {
      }

      public Builder setPeriodTicks(final int periodTicks) {
         this.periodTicks = Optional.of(periodTicks);
         return this;
      }

      public Builder addModifierTrack(final EnvironmentAttribute attribute, final AttributeModifier modifier, final Consumer builder) {
         attribute.type().checkAllowedModifier(modifier);
         KeyframeTrack.Builder<Argument> argumentTrack = new KeyframeTrack.Builder();
         builder.accept(argumentTrack);
         this.tracks.put(attribute, new AttributeTrack(modifier, argumentTrack.build()));
         return this;
      }

      public Builder addTrack(final EnvironmentAttribute attribute, final Consumer builder) {
         return this.addModifierTrack(attribute, AttributeModifier.override(), builder);
      }

      public Timeline build() {
         return new Timeline(this.periodTicks, this.tracks.build());
      }
   }
}
