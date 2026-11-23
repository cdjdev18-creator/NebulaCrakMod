package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlastingRecipe extends AbstractCookingRecipe {
   public BlastingRecipe(final String group, final CookingBookCategory category, final Ingredient ingredient, final ItemStack result, final float experience, final int cookingTime) {
      super(group, category, ingredient, result, experience, cookingTime);
   }

   protected Item furnaceIcon() {
      return Items.BLAST_FURNACE;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.BLASTING_RECIPE;
   }

   public RecipeType getType() {
      return RecipeType.BLASTING;
   }

   public RecipeBookCategory recipeBookCategory() {
      RecipeBookCategory var10000;
      switch (this.category()) {
         case BLOCKS:
            var10000 = RecipeBookCategories.BLAST_FURNACE_BLOCKS;
            break;
         case FOOD:
         case MISC:
            var10000 = RecipeBookCategories.BLAST_FURNACE_MISC;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
