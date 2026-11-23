package net.minecraft.world.item.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

public class StonecutterRecipe extends SingleItemRecipe {
   public StonecutterRecipe(final String group, final Ingredient ingredient, final ItemStack result) {
      super(group, ingredient, result);
   }

   public RecipeType getType() {
      return RecipeType.STONECUTTING;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.STONECUTTER;
   }

   public List display() {
      return List.of(new StonecutterRecipeDisplay(this.input().display(), this.resultDisplay(), new SlotDisplay.ItemSlotDisplay(Items.STONECUTTER)));
   }

   public SlotDisplay resultDisplay() {
      return new SlotDisplay.ItemStackSlotDisplay(this.result());
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.STONECUTTER;
   }
}
