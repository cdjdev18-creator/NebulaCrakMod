package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

public class ArmorDyeRecipe extends CustomRecipe {
   public ArmorDyeRecipe(final CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      } else {
         boolean hasArmor = false;
         boolean hasDyes = false;

         for(int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (!itemStack.isEmpty()) {
               if (itemStack.is(ItemTags.DYEABLE)) {
                  if (hasArmor) {
                     return false;
                  }

                  hasArmor = true;
               } else {
                  if (!(itemStack.getItem() instanceof DyeItem)) {
                     return false;
                  }

                  hasDyes = true;
               }
            }
         }

         return hasDyes && hasArmor;
      }
   }

   public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
      List<DyeItem> dyes = new ArrayList();
      ItemStack armorItemStack = ItemStack.EMPTY;

      for(int slot = 0; slot < input.size(); ++slot) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (itemStack.is(ItemTags.DYEABLE)) {
               if (!armorItemStack.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               armorItemStack = itemStack.copy();
            } else {
               Item var8 = itemStack.getItem();
               if (!(var8 instanceof DyeItem)) {
                  return ItemStack.EMPTY;
               }

               DyeItem dye = (DyeItem)var8;
               dyes.add(dye);
            }
         }
      }

      if (!armorItemStack.isEmpty() && !dyes.isEmpty()) {
         return DyedItemColor.applyDyes(armorItemStack, dyes);
      } else {
         return ItemStack.EMPTY;
      }
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.ARMOR_DYE;
   }
}
