package net.minecraft.world.item.crafting;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class SingleItemRecipe implements Recipe {
   private final Ingredient input;
   private final ItemStack result;
   private final String group;
   private @Nullable PlacementInfo placementInfo;

   public SingleItemRecipe(final String group, final Ingredient input, final ItemStack result) {
      this.group = group;
      this.input = input;
      this.result = result;
   }

   public abstract RecipeSerializer getSerializer();

   public abstract RecipeType getType();

   public boolean matches(final SingleRecipeInput input, final Level level) {
      return this.input.test(input.item());
   }

   public String group() {
      return this.group;
   }

   public Ingredient input() {
      return this.input;
   }

   protected ItemStack result() {
      return this.result;
   }

   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(this.input);
      }

      return this.placementInfo;
   }

   public ItemStack assemble(final SingleRecipeInput input, final HolderLookup.Provider registries) {
      return this.result.copy();
   }

   public static class Serializer implements RecipeSerializer {
      private final MapCodec codec;
      private final StreamCodec streamCodec;

      protected Serializer(final Factory factory) {
         this.codec = RecordCodecBuilder.mapCodec((r) -> {
            Products.P3 var10000 = r.group(Codec.STRING.optionalFieldOf("group", "").forGetter(SingleItemRecipe::group), Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input), ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleItemRecipe::result));
            Objects.requireNonNull(factory);
            return var10000.apply(r, factory::create);
         });
         StreamCodec var10001 = ByteBufCodecs.STRING_UTF8;
         Function var10002 = SingleItemRecipe::group;
         StreamCodec var10003 = Ingredient.CONTENTS_STREAM_CODEC;
         Function var10004 = SingleItemRecipe::input;
         StreamCodec var10005 = ItemStack.STREAM_CODEC;
         Function var10006 = SingleItemRecipe::result;
         Objects.requireNonNull(factory);
         this.streamCodec = StreamCodec.composite(var10001, var10002, var10003, var10004, var10005, var10006, factory::create);
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
      SingleItemRecipe create(String group, Ingredient ingredient, ItemStack result);
   }
}
