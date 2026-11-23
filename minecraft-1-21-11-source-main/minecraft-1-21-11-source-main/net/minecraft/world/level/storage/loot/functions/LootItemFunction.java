package net.minecraft.world.level.storage.loot.functions;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface LootItemFunction extends LootContextUser, BiFunction {
   LootItemFunctionType getType();

   static Consumer decorate(final BiFunction function, final Consumer output, final LootContext context) {
      return (drop) -> output.accept((ItemStack)function.apply(drop, context));
   }

   public interface Builder {
      LootItemFunction build();
   }
}
