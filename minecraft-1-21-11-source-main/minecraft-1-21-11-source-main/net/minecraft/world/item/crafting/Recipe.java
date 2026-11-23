package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface Recipe {
   Codec CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec().dispatch(Recipe::getSerializer, RecipeSerializer::codec);
   Codec KEY_CODEC = ResourceKey.codec(Registries.RECIPE);
   StreamCodec STREAM_CODEC = ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER).dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);

   boolean matches(RecipeInput input, Level level);

   ItemStack assemble(RecipeInput input, final HolderLookup.Provider registries);

   default boolean isSpecial() {
      return false;
   }

   default boolean showNotification() {
      return true;
   }

   default String group() {
      return "";
   }

   RecipeSerializer getSerializer();

   RecipeType getType();

   PlacementInfo placementInfo();

   default List display() {
      return List.of();
   }

   RecipeBookCategory recipeBookCategory();
}
