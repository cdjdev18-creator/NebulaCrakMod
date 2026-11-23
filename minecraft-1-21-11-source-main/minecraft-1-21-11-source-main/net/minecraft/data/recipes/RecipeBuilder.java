package net.minecraft.data.recipes;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public interface RecipeBuilder {
   Identifier ROOT_RECIPE_ADVANCEMENT = Identifier.withDefaultNamespace("recipes/root");

   RecipeBuilder unlockedBy(String name, Criterion criterion);

   RecipeBuilder group(@Nullable String group);

   Item getResult();

   void save(RecipeOutput output, ResourceKey location);

   default void save(final RecipeOutput output) {
      this.save(output, ResourceKey.create(Registries.RECIPE, getDefaultRecipeId(this.getResult())));
   }

   default void save(final RecipeOutput output, final String id) {
      Identifier key = getDefaultRecipeId(this.getResult());
      Identifier resourceId = Identifier.parse(id);
      if (resourceId.equals(key)) {
         throw new IllegalStateException("Recipe " + id + " should remove its 'save' argument as it is equal to default one");
      } else {
         this.save(output, ResourceKey.create(Registries.RECIPE, resourceId));
      }
   }

   static Identifier getDefaultRecipeId(final ItemLike itemLike) {
      return BuiltInRegistries.ITEM.getKey(itemLike.asItem());
   }

   static CraftingBookCategory determineBookCategory(final RecipeCategory category) {
      CraftingBookCategory var10000;
      switch (category) {
         case BUILDING_BLOCKS:
            var10000 = CraftingBookCategory.BUILDING;
            break;
         case TOOLS:
         case COMBAT:
            var10000 = CraftingBookCategory.EQUIPMENT;
            break;
         case REDSTONE:
            var10000 = CraftingBookCategory.REDSTONE;
            break;
         default:
            var10000 = CraftingBookCategory.MISC;
      }

      return var10000;
   }
}
