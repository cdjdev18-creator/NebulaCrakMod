package net.minecraft.world.item.crafting;

import com.mojang.datafixers.Products;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.codec.StreamCodec;

public abstract class CustomRecipe implements CraftingRecipe {
   private final CraftingBookCategory category;

   public CustomRecipe(final CraftingBookCategory category) {
      this.category = category;
   }

   public boolean isSpecial() {
      return true;
   }

   public CraftingBookCategory category() {
      return this.category;
   }

   public PlacementInfo placementInfo() {
      return PlacementInfo.NOT_PLACEABLE;
   }

   public abstract RecipeSerializer getSerializer();

   public static class Serializer implements RecipeSerializer {
      private final MapCodec codec;
      private final StreamCodec streamCodec;

      public Serializer(final Factory constructor) {
         this.codec = RecordCodecBuilder.mapCodec((r) -> {
            Products.P1 var10000 = r.group(CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CraftingRecipe::category));
            Objects.requireNonNull(constructor);
            return var10000.apply(r, constructor::create);
         });
         StreamCodec var10001 = CraftingBookCategory.STREAM_CODEC;
         Function var10002 = CraftingRecipe::category;
         Objects.requireNonNull(constructor);
         this.streamCodec = StreamCodec.composite(var10001, var10002, constructor::create);
      }

      public MapCodec codec() {
         return this.codec;
      }

      public StreamCodec streamCodec() {
         return this.streamCodec;
      }

      @FunctionalInterface
      public interface Factory {
         CraftingRecipe create(CraftingBookCategory category);
      }
   }
}
