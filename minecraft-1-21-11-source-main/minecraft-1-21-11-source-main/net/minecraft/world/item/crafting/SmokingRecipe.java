package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SmokingRecipe extends AbstractCookingRecipe {
   public SmokingRecipe(final String group, final CookingBookCategory category, final Ingredient ingredient, final ItemStack result, final float experience, final int cookingTime) {
      super(group, category, ingredient, result, experience, cookingTime);
   }

   protected Item furnaceIcon() {
      return Items.SMOKER;
   }

   public RecipeType getType() {
      return RecipeType.SMOKING;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.SMOKING_RECIPE;
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.SMOKER_FOOD;
   }
}
