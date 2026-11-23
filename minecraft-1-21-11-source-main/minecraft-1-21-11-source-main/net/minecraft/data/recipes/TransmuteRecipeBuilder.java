package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.jspecify.annotations.Nullable;

public class TransmuteRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Holder result;
   private final Ingredient input;
   private final Ingredient material;
   private final Map criteria = new LinkedHashMap();
   private @Nullable String group;

   private TransmuteRecipeBuilder(final RecipeCategory category, final Holder result, final Ingredient input, final Ingredient material) {
      this.category = category;
      this.result = result;
      this.input = input;
      this.material = material;
   }

   public static TransmuteRecipeBuilder transmute(final RecipeCategory category, final Ingredient input, final Ingredient material, final Item result) {
      return new TransmuteRecipeBuilder(category, result.builtInRegistryHolder(), input, material);
   }

   public TransmuteRecipeBuilder unlockedBy(final String name, final Criterion criterion) {
      this.criteria.put(name, criterion);
      return this;
   }

   public TransmuteRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   public Item getResult() {
      return (Item)this.result.value();
   }

   public void save(final RecipeOutput output, final ResourceKey id) {
      this.ensureValid(id);
      Advancement.Builder advancement = output.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
      Map var10000 = this.criteria;
      Objects.requireNonNull(advancement);
      var10000.forEach(advancement::addCriterion);
      TransmuteRecipe recipe = new TransmuteRecipe((String)Objects.requireNonNullElse(this.group, ""), RecipeBuilder.determineBookCategory(this.category), this.input, this.material, new TransmuteResult((Item)this.result.value()));
      output.accept(id, recipe, advancement.build(id.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/")));
   }

   private void ensureValid(final ResourceKey id) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + String.valueOf(id.identifier()));
      }
   }
}
