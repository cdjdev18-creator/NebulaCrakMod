package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;

public record ContextScoreboardNameProvider(LootContext.EntityTarget target) implements ScoreboardNameProvider {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(LootContext.EntityTarget.CODEC.fieldOf("target").forGetter(ContextScoreboardNameProvider::target)).apply(i, ContextScoreboardNameProvider::new));
   public static final Codec INLINE_CODEC;

   public static ScoreboardNameProvider forTarget(final LootContext.EntityTarget target) {
      return new ContextScoreboardNameProvider(target);
   }

   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.CONTEXT;
   }

   public @Nullable ScoreHolder getScoreHolder(final LootContext context) {
      return (ScoreHolder)context.getOptionalParameter(this.target.contextParam());
   }

   public Set getReferencedContextParams() {
      return Set.of(this.target.contextParam());
   }

   static {
      INLINE_CODEC = LootContext.EntityTarget.CODEC.xmap(ContextScoreboardNameProvider::new, ContextScoreboardNameProvider::target);
   }
}
