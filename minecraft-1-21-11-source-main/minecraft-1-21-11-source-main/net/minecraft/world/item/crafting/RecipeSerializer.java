package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;

public interface RecipeSerializer {
   RecipeSerializer SHAPED_RECIPE = register("crafting_shaped", new ShapedRecipe.Serializer());
   RecipeSerializer SHAPELESS_RECIPE = register("crafting_shapeless", new ShapelessRecipe.Serializer());
   RecipeSerializer ARMOR_DYE = register("crafting_special_armordye", new CustomRecipe.Serializer(ArmorDyeRecipe::new));
   RecipeSerializer BOOK_CLONING = register("crafting_special_bookcloning", new CustomRecipe.Serializer(BookCloningRecipe::new));
   RecipeSerializer MAP_CLONING = register("crafting_special_mapcloning", new CustomRecipe.Serializer(MapCloningRecipe::new));
   RecipeSerializer MAP_EXTENDING = register("crafting_special_mapextending", new CustomRecipe.Serializer(MapExtendingRecipe::new));
   RecipeSerializer FIREWORK_ROCKET = register("crafting_special_firework_rocket", new CustomRecipe.Serializer(FireworkRocketRecipe::new));
   RecipeSerializer FIREWORK_STAR = register("crafting_special_firework_star", new CustomRecipe.Serializer(FireworkStarRecipe::new));
   RecipeSerializer FIREWORK_STAR_FADE = register("crafting_special_firework_star_fade", new CustomRecipe.Serializer(FireworkStarFadeRecipe::new));
   RecipeSerializer TIPPED_ARROW = register("crafting_special_tippedarrow", new CustomRecipe.Serializer(TippedArrowRecipe::new));
   RecipeSerializer BANNER_DUPLICATE = register("crafting_special_bannerduplicate", new CustomRecipe.Serializer(BannerDuplicateRecipe::new));
   RecipeSerializer SHIELD_DECORATION = register("crafting_special_shielddecoration", new CustomRecipe.Serializer(ShieldDecorationRecipe::new));
   RecipeSerializer TRANSMUTE = register("crafting_transmute", new TransmuteRecipe.Serializer());
   RecipeSerializer REPAIR_ITEM = register("crafting_special_repairitem", new CustomRecipe.Serializer(RepairItemRecipe::new));
   RecipeSerializer SMELTING_RECIPE = register("smelting", new AbstractCookingRecipe.Serializer(SmeltingRecipe::new, 200));
   RecipeSerializer BLASTING_RECIPE = register("blasting", new AbstractCookingRecipe.Serializer(BlastingRecipe::new, 100));
   RecipeSerializer SMOKING_RECIPE = register("smoking", new AbstractCookingRecipe.Serializer(SmokingRecipe::new, 100));
   RecipeSerializer CAMPFIRE_COOKING_RECIPE = register("campfire_cooking", new AbstractCookingRecipe.Serializer(CampfireCookingRecipe::new, 100));
   RecipeSerializer STONECUTTER = register("stonecutting", new SingleItemRecipe.Serializer(StonecutterRecipe::new));
   RecipeSerializer SMITHING_TRANSFORM = register("smithing_transform", new SmithingTransformRecipe.Serializer());
   RecipeSerializer SMITHING_TRIM = register("smithing_trim", new SmithingTrimRecipe.Serializer());
   RecipeSerializer DECORATED_POT_RECIPE = register("crafting_decorated_pot", new CustomRecipe.Serializer(DecoratedPotRecipe::new));

   MapCodec codec();

   /** @deprecated */
   @Deprecated
   StreamCodec streamCodec();

   static RecipeSerializer register(final String name, final RecipeSerializer serializer) {
      return (RecipeSerializer)Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, (String)name, serializer);
   }
}
