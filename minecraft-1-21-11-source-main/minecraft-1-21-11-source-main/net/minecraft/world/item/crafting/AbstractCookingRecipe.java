package net.minecraft.world.item.crafting;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public abstract class AbstractCookingRecipe extends SingleItemRecipe {
   private final CookingBookCategory category;
   private final float experience;
   private final int cookingTime;

   public AbstractCookingRecipe(final String group, final CookingBookCategory category, final Ingredient ingredient, final ItemStack result, final float experience, final int cookingTime) {
      super(group, ingredient, result);
      this.category = category;
      this.experience = experience;
      this.cookingTime = cookingTime;
   }

   public abstract RecipeSerializer getSerializer();

   public abstract RecipeType getType();

   public float experience() {
      return this.experience;
   }

   public int cookingTime() {
      return this.cookingTime;
   }

   public CookingBookCategory category() {
      return this.category;
   }

   protected abstract Item furnaceIcon();

   public List display() {
      return List.of(new FurnaceRecipeDisplay(this.input().display(), SlotDisplay.AnyFuel.INSTANCE, new SlotDisplay.ItemStackSlotDisplay(this.result()), new SlotDisplay.ItemSlotDisplay(this.furnaceIcon()), this.cookingTime, this.experience));
   }

   public static class Serializer implements RecipeSerializer {
      private final MapCodec codec;
      private final StreamCodec streamCodec;

      public Serializer(final Factory factory, final int defaultCookingTime) {
         this.codec = RecordCodecBuilder.mapCodec((r) -> {
            Products.P6 var10000 = r.group(Codec.STRING.optionalFieldOf("group", "").forGetter(SingleItemRecipe::group), CookingBookCategory.CODEC.fieldOf("category").orElse(CookingBookCategory.MISC).forGetter(AbstractCookingRecipe::category), Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input), ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("result").forGetter(SingleItemRecipe::result), Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter(AbstractCookingRecipe::experience), Codec.INT.fieldOf("cookingtime").orElse(defaultCookingTime).forGetter(AbstractCookingRecipe::cookingTime));
            Objects.requireNonNull(factory);
            return var10000.apply(r, factory::create);
         });
         StreamCodec var10001 = ByteBufCodecs.STRING_UTF8;
         Function var10002 = SingleItemRecipe::group;
         StreamCodec var10003 = CookingBookCategory.STREAM_CODEC;
         Function var10004 = AbstractCookingRecipe::category;
         StreamCodec var10005 = Ingredient.CONTENTS_STREAM_CODEC;
         Function var10006 = SingleItemRecipe::input;
         StreamCodec var10007 = ItemStack.STREAM_CODEC;
         Function var10008 = SingleItemRecipe::result;
         StreamCodec var10009 = ByteBufCodecs.FLOAT;
         Function var10010 = AbstractCookingRecipe::experience;
         StreamCodec var10011 = ByteBufCodecs.INT;
         Function var10012 = AbstractCookingRecipe::cookingTime;
         Objects.requireNonNull(factory);
         this.streamCodec = StreamCodec.composite(var10001, var10002, var10003, var10004, var10005, var10006, var10007, var10008, var10009, var10010, var10011, var10012, factory::create);
      }

      public MapCodec codec() {
         return this.codec;
      }

      public StreamCodec streamCodec() {
         return this.streamCodec;
      }
   }

   @FunctionalInterface
   public interface Factory {
      AbstractCookingRecipe create(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime);
   }
}
