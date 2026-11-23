package net.minecraft.data.recipes;

import java.util.function.Function;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

public class SpecialRecipeBuilder {
   private final Function factory;

   public SpecialRecipeBuilder(final Function factory) {
      this.factory = factory;
   }

   public static SpecialRecipeBuilder special(final Function factory) {
      return new SpecialRecipeBuilder(factory);
   }

   public void save(final RecipeOutput output, final String name) {
      this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
   }

   public void save(final RecipeOutput output, final ResourceKey id) {
      output.accept(id, (Recipe)this.factory.apply(CraftingBookCategory.MISC), (AdvancementHolder)null);
   }
}
