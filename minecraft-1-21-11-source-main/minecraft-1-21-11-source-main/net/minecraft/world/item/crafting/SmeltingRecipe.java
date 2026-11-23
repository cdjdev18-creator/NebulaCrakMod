package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SmeltingRecipe extends AbstractCookingRecipe {
   public SmeltingRecipe(final String group, final CookingBookCategory category, final Ingredient ingredient, final ItemStack result, final float experience, final int cookingTime) {
      super(group, category, ingredient, result, experience, cookingTime);
   }

   protected Item furnaceIcon() {
      return Items.FURNACE;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.SMELTING_RECIPE;
   }

   public RecipeType getType() {
      return RecipeType.SMELTING;
   }

   public RecipeBookCategory recipeBookCategory() {
      RecipeBookCategory var10000;
      switch (this.category()) {
         case BLOCKS -> var10000 = RecipeBookCategories.FURNACE_BLOCKS;
         case FOOD -> var10000 = RecipeBookCategories.FURNACE_FOOD;
         case MISC -> var10000 = RecipeBookCategories.FURNACE_MISC;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
