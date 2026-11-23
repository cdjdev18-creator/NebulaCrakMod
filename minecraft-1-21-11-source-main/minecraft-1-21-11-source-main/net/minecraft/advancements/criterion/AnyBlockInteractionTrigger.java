package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger {
   public Codec codec() {
      return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final BlockPos pos, final ItemStack itemStack) {
      ServerLevel level = player.level();
      BlockState state = level.getBlockState(pos);
      LootParams params = (new LootParams.Builder(level)).withParameter(LootContextParams.ORIGIN, pos.getCenter()).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.BLOCK_STATE, state).withParameter(LootContextParams.TOOL, itemStack).create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext context = (new LootContext.Builder(params)).create(Optional.empty());
      this.trigger(player, (t) -> t.matches(context));
   }

   public static record TriggerInstance(Optional player, Optional location) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec CODEC = RecordCodecBuilder.create((i) -> i.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(TriggerInstance::location)).apply(i, TriggerInstance::new));

      public boolean matches(final LootContext locationContext) {
         return this.location.isEmpty() || ((ContextAwarePredicate)this.location.get()).matches(locationContext);
      }

      public void validate(final CriterionValidator validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         this.location.ifPresent((predicate) -> validator.validate(predicate, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
      }
   }
}
