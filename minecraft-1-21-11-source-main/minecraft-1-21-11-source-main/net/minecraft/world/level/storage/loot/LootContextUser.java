package net.minecraft.world.level.storage.loot;

import java.util.Set;

public interface LootContextUser {
   default Set getReferencedContextParams() {
      return Set.of();
   }

   default void validate(final ValidationContext context) {
      context.validateContextUsage(this);
   }
}
