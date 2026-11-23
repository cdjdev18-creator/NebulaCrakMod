package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.ScoreHolder;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name)).apply(i, FixedScoreboardNameProvider::new));

   public static ScoreboardNameProvider forName(final String name) {
      return new FixedScoreboardNameProvider(name);
   }

   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.FIXED;
   }

   public ScoreHolder getScoreHolder(final LootContext context) {
      return ScoreHolder.forNameOnly(this.name);
   }

   public Set getReferencedContextParams() {
      return Set.of();
   }
}
