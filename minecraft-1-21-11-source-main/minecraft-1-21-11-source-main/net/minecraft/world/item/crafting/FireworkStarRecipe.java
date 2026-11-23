package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class FireworkStarRecipe extends CustomRecipe {
   private static final Map SHAPE_BY_ITEM;
   private static final Ingredient TRAIL_INGREDIENT;
   private static final Ingredient TWINKLE_INGREDIENT;
   private static final Ingredient GUNPOWDER_INGREDIENT;

   public FireworkStarRecipe(final CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      } else {
         boolean gunPowder = false;
         boolean colors = false;
         boolean shape = false;
         boolean trail = false;
         boolean twinkle = false;

         for(int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (!itemStack.isEmpty()) {
               if (SHAPE_BY_ITEM.containsKey(itemStack.getItem())) {
                  if (shape) {
                     return false;
                  }

                  shape = true;
               } else if (TWINKLE_INGREDIENT.test(itemStack)) {
                  if (twinkle) {
                     return false;
                  }

                  twinkle = true;
               } else if (TRAIL_INGREDIENT.test(itemStack)) {
                  if (trail) {
                     return false;
                  }

                  trail = true;
               } else if (GUNPOWDER_INGREDIENT.test(itemStack)) {
                  if (gunPowder) {
                     return false;
                  }

                  gunPowder = true;
               } else {
                  if (!(itemStack.getItem() instanceof DyeItem)) {
                     return false;
                  }

                  colors = true;
               }
            }
         }

         return gunPowder && colors;
      }
   }

   public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
      FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
      boolean hasTwinkle = false;
      boolean hasTrail = false;
      IntList colors = new IntArrayList();

      for(int slot = 0; slot < input.size(); ++slot) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            FireworkExplosion.Shape maybeShape = (FireworkExplosion.Shape)SHAPE_BY_ITEM.get(itemStack.getItem());
            if (maybeShape != null) {
               shape = maybeShape;
            } else if (TWINKLE_INGREDIENT.test(itemStack)) {
               hasTwinkle = true;
            } else if (TRAIL_INGREDIENT.test(itemStack)) {
               hasTrail = true;
            } else {
               Item var11 = itemStack.getItem();
               if (var11 instanceof DyeItem) {
                  DyeItem dye = (DyeItem)var11;
                  colors.add(dye.getDyeColor().getFireworkColor());
               }
            }
         }
      }

      ItemStack star = new ItemStack(Items.FIREWORK_STAR);
      star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(shape, colors, IntList.of(), hasTrail, hasTwinkle));
      return star;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.FIREWORK_STAR;
   }

   static {
      SHAPE_BY_ITEM = Map.of(Items.FIRE_CHARGE, FireworkExplosion.Shape.LARGE_BALL, Items.FEATHER, FireworkExplosion.Shape.BURST, Items.GOLD_NUGGET, FireworkExplosion.Shape.STAR, Items.SKELETON_SKULL, FireworkExplosion.Shape.CREEPER, Items.WITHER_SKELETON_SKULL, FireworkExplosion.Shape.CREEPER, Items.CREEPER_HEAD, FireworkExplosion.Shape.CREEPER, Items.PLAYER_HEAD, FireworkExplosion.Shape.CREEPER, Items.DRAGON_HEAD, FireworkExplosion.Shape.CREEPER, Items.ZOMBIE_HEAD, FireworkExplosion.Shape.CREEPER, Items.PIGLIN_HEAD, FireworkExplosion.Shape.CREEPER);
      TRAIL_INGREDIENT = Ingredient.of((ItemLike)Items.DIAMOND);
      TWINKLE_INGREDIENT = Ingredient.of((ItemLike)Items.GLOWSTONE_DUST);
      GUNPOWDER_INGREDIENT = Ingredient.of((ItemLike)Items.GUNPOWDER);
   }
}
