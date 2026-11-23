package net.minecraft.world.item.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CampfireCookingRecipe extends AbstractCookingRecipe {
   public CampfireCookingRecipe(final String group, final CookingBookCategory category, final Ingredient ingredient, final ItemStack result, final float experience, final int cookingTime) {
      super(group, category, ingredient, result, experience, cookingTime);
   }

   protected Item furnaceIcon() {
      return Items.CAMPFIRE;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.CAMPFIRE_COOKING_RECIPE;
   }

   public RecipeType getType() {
      return RecipeType.CAMPFIRE_COOKING;
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CAMPFIRE;
   }
}
