package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

public final class FluidTags {
   public static final TagKey WATER = create("water");
   public static final TagKey LAVA = create("lava");

   private FluidTags() {
   }

   private static TagKey create(final String name) {
      return TagKey.create(Registries.FLUID, Identifier.withDefaultNamespace(name));
   }
}
