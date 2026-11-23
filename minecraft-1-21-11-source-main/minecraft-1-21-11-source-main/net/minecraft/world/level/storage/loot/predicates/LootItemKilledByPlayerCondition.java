package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootItemKilledByPlayerCondition implements LootItemCondition {
   private static final LootItemKilledByPlayerCondition INSTANCE = new LootItemKilledByPlayerCondition();
   public static final MapCodec CODEC;

   private LootItemKilledByPlayerCondition() {
   }

   public LootItemConditionType getType() {
      return LootItemConditions.KILLED_BY_PLAYER;
   }

   public Set getReferencedContextParams() {
      return Set.of(LootContextParams.LAST_DAMAGE_PLAYER);
   }

   public boolean test(final LootContext context) {
      return context.hasParameter(LootContextParams.LAST_DAMAGE_PLAYER);
   }

   public static LootItemCondition.Builder killedByPlayer() {
      return () -> INSTANCE;
   }

   static {
      CODEC = MapCodec.unit(INSTANCE);
   }
}
