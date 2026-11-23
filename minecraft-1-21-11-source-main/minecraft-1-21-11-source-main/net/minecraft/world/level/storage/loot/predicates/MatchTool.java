package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record MatchTool(Optional predicate) implements LootItemCondition {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(ItemPredicate.CODEC.optionalFieldOf("predicate").forGetter(MatchTool::predicate)).apply(i, MatchTool::new));

   public LootItemConditionType getType() {
      return LootItemConditions.MATCH_TOOL;
   }

   public Set getReferencedContextParams() {
      return Set.of(LootContextParams.TOOL);
   }

   public boolean test(final LootContext context) {
      ItemStack tool = (ItemStack)context.getOptionalParameter(LootContextParams.TOOL);
      return tool != null && (this.predicate.isEmpty() || ((ItemPredicate)this.predicate.get()).test(tool));
   }

   public static LootItemCondition.Builder toolMatches(final ItemPredicate.Builder predicate) {
      return () -> new MatchTool(Optional.of(predicate.build()));
   }
}
