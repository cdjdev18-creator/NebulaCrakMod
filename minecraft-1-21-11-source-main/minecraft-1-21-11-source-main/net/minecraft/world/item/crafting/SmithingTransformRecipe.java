package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import org.jspecify.annotations.Nullable;

public class SmithingTransformRecipe implements SmithingRecipe {
   private final Optional template;
   private final Ingredient base;
   private final Optional addition;
   private final TransmuteResult result;
   private @Nullable PlacementInfo placementInfo;

   public SmithingTransformRecipe(final Optional template, final Ingredient base, final Optional addition, final TransmuteResult result) {
      this.template = template;
      this.base = base;
      this.addition = addition;
      this.result = result;
   }

   public ItemStack assemble(final SmithingRecipeInput input, final HolderLookup.Provider registries) {
      return this.result.apply(input.base());
   }

   public Optional templateIngredient() {
      return this.template;
   }

   public Ingredient baseIngredient() {
      return this.base;
   }

   public Optional additionIngredient() {
      return this.addition;
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.SMITHING_TRANSFORM;
   }

   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.createFromOptionals(List.of(this.template, Optional.of(this.base), this.addition));
      }

      return this.placementInfo;
   }

   public List display() {
      return List.of(new SmithingRecipeDisplay(Ingredient.optionalIngredientToDisplay(this.template), this.base.display(), Ingredient.optionalIngredientToDisplay(this.addition), this.result.display(), new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
   }

   public static class Serializer implements RecipeSerializer {
      private static final MapCodec CODEC = RecordCodecBuilder.mapCodec((r) -> r.group(Ingredient.CODEC.optionalFieldOf("template").forGetter((o) -> o.template), Ingredient.CODEC.fieldOf("base").forGetter((o) -> o.base), Ingredient.CODEC.optionalFieldOf("addition").forGetter((o) -> o.addition), TransmuteResult.CODEC.fieldOf("result").forGetter((o) -> o.result)).apply(r, SmithingTransformRecipe::new));
      public static final StreamCodec STREAM_CODEC;

      public MapCodec codec() {
         return CODEC;
      }

      public StreamCodec streamCodec() {
         return STREAM_CODEC;
      }

      static {
         STREAM_CODEC = StreamCodec.composite(Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, (r) -> r.template, Ingredient.CONTENTS_STREAM_CODEC, (r) -> r.base, Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, (r) -> r.addition, TransmuteResult.STREAM_CODEC, (r) -> r.result, SmithingTransformRecipe::new);
      }
   }
}
