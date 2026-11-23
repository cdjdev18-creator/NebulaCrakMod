package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface CraftingRecipe extends Recipe {
   default RecipeType getType() {
      return RecipeType.CRAFTING;
   }

   RecipeSerializer getSerializer();

   CraftingBookCategory category();

   default NonNullList getRemainingItems(final CraftingInput input) {
      return defaultCraftingReminder(input);
   }

   static NonNullList defaultCraftingReminder(final CraftingInput input) {
      NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

      for(int slot = 0; slot < result.size(); ++slot) {
         Item item = input.getItem(slot).getItem();
         result.set(slot, item.getCraftingRemainder());
      }

      return result;
   }

   default RecipeBookCategory recipeBookCategory() {
      RecipeBookCategory var10000;
      switch (this.category()) {
         case BUILDING -> var10000 = RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
         case EQUIPMENT -> var10000 = RecipeBookCategories.CRAFTING_EQUIPMENT;
         case REDSTONE -> var10000 = RecipeBookCategories.CRAFTING_REDSTONE;
         case MISC -> var10000 = RecipeBookCategories.CRAFTING_MISC;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
