package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ShapedRecipe implements CraftingRecipe {
   private final ShapedRecipePattern pattern;
   private final ItemStack result;
   private final String group;
   private final CraftingBookCategory category;
   private final boolean showNotification;
   private @Nullable PlacementInfo placementInfo;

   public ShapedRecipe(final String group, final CraftingBookCategory category, final ShapedRecipePattern pattern, final ItemStack result, final boolean showNotification) {
      this.group = group;
      this.category = category;
      this.pattern = pattern;
      this.result = result;
      this.showNotification = showNotification;
   }

   public ShapedRecipe(final String group, final CraftingBookCategory category, final ShapedRecipePattern pattern, final ItemStack result) {
      this(group, category, pattern, result, true);
   }

   public RecipeSerializer getSerializer() {
      return RecipeSerializer.SHAPED_RECIPE;
   }

   public String group() {
      return this.group;
   }

   public CraftingBookCategory category() {
      return this.category;
   }

   @VisibleForTesting
   public List getIngredients() {
      return this.pattern.ingredients();
   }

   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());
      }

      return this.placementInfo;
   }

   public boolean showNotification() {
      return this.showNotification;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      return this.pattern.matches(input);
   }

   public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
      return this.result.copy();
   }

   public int getWidth() {
      return this.pattern.width();
   }

   public int getHeight() {
      return this.pattern.height();
   }

   public List display() {
      return List.of(new ShapedCraftingRecipeDisplay(this.pattern.width(), this.pattern.height(), this.pattern.ingredients().stream().map((e) -> (SlotDisplay)e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(), new SlotDisplay.ItemStackSlotDisplay(this.result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
   }

   public static class Serializer implements RecipeSerializer {
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((r) -> r.group(Codec.STRING.optionalFieldOf("group", "").forGetter((o) -> o.group), CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((o) -> o.category), ShapedRecipePattern.MAP_CODEC.forGetter((o) -> o.pattern), ItemStack.STRICT_CODEC.fieldOf("result").forGetter((o) -> o.result), Codec.BOOL.optionalFieldOf("show_notification", true).forGetter((o) -> o.showNotification)).apply(r, ShapedRecipe::new));
      public static final StreamCodec STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

      public MapCodec codec() {
         return CODEC;
      }

      public StreamCodec streamCodec() {
         return STREAM_CODEC;
      }

      private static ShapedRecipe fromNetwork(final RegistryFriendlyByteBuf input) {
         String group = input.readUtf();
         CraftingBookCategory category = (CraftingBookCategory)input.readEnum(CraftingBookCategory.class);
         ShapedRecipePattern pattern = (ShapedRecipePattern)ShapedRecipePattern.STREAM_CODEC.decode(input);
         ItemStack result = (ItemStack)ItemStack.STREAM_CODEC.decode(input);
         boolean showNotification = input.readBoolean();
         return new ShapedRecipe(group, category, pattern, result, showNotification);
      }

      private static void toNetwork(final RegistryFriendlyByteBuf output, final ShapedRecipe recipe) {
         output.writeUtf(recipe.group);
         output.writeEnum(recipe.category);
         ShapedRecipePattern.STREAM_CODEC.encode(output, recipe.pattern);
         ItemStack.STREAM_CODEC.encode(output, recipe.result);
         output.writeBoolean(recipe.showNotification);
      }
   }
}
