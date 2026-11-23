package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record EnchantmentActiveCheck(boolean active) implements LootItemCondition {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Codec.BOOL.fieldOf("active").forGetter(EnchantmentActiveCheck::active)).apply(i, EnchantmentActiveCheck::new));

   public boolean test(final LootContext lootContext) {
      return (Boolean)lootContext.getParameter(LootContextParams.ENCHANTMENT_ACTIVE) == this.active;
   }

   public LootItemConditionType getType() {
      return LootItemConditions.ENCHANTMENT_ACTIVE_CHECK;
   }

   public Set getReferencedContextParams() {
      return Set.of(LootContextParams.ENCHANTMENT_ACTIVE);
   }

   public static LootItemCondition.Builder enchantmentActiveCheck() {
      return () -> new EnchantmentActiveCheck(true);
   }

   public static LootItemCondition.Builder enchantmentInactiveCheck() {
      return () -> new EnchantmentActiveCheck(false);
   }
}
