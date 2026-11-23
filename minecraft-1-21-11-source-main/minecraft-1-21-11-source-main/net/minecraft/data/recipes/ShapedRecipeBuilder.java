package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapedRecipeBuilder implements RecipeBuilder {
   private final HolderGetter items;
   private final RecipeCategory category;
   private final Item result;
   private final int count;
   private final List rows = Lists.newArrayList();
   private final Map key = Maps.newLinkedHashMap();
   private final Map criteria = new LinkedHashMap();
   private @Nullable String group;
   private boolean showNotification = true;

   private ShapedRecipeBuilder(final HolderGetter items, final RecipeCategory category, final ItemLike result, final int count) {
      this.items = items;
      this.category = category;
      this.result = result.asItem();
      this.count = count;
   }

   public static ShapedRecipeBuilder shaped(final HolderGetter items, final RecipeCategory category, final ItemLike item) {
      return shaped(items, category, item, 1);
   }

   public static ShapedRecipeBuilder shaped(final HolderGetter items, final RecipeCategory category, final ItemLike item, final int count) {
      return new ShapedRecipeBuilder(items, category, item, count);
   }

   public ShapedRecipeBuilder define(final Character symbol, final TagKey tag) {
      return this.define(symbol, Ingredient.of((HolderSet)this.items.getOrThrow(tag)));
   }

   public ShapedRecipeBuilder define(final Character symbol, final ItemLike item) {
      return this.define(symbol, Ingredient.of(item));
   }

   public ShapedRecipeBuilder define(final Character symbol, final Ingredient ingredient) {
      if (this.key.containsKey(symbol)) {
         throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
      } else if (symbol == ' ') {
         throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
      } else {
         this.key.put(symbol, ingredient);
         return this;
      }
   }

   public ShapedRecipeBuilder pattern(final String row) {
      if (!this.rows.isEmpty() && row.length() != ((String)this.rows.get(0)).length()) {
         throw new IllegalArgumentException("Pattern must be the same width on every line!");
      } else {
         this.rows.add(row);
         return this;
      }
   }

   public ShapedRecipeBuilder unlockedBy(final String name, final Criterion criterion) {
      this.criteria.put(name, criterion);
      return this;
   }

   public ShapedRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   public ShapedRecipeBuilder showNotification(final boolean showNotification) {
      this.showNotification = showNotification;
      return this;
   }

   public Item getResult() {
      return this.result;
   }

   public void save(final RecipeOutput output, final ResourceKey id) {
      ShapedRecipePattern pattern = this.ensureValid(id);
      Advancement.Builder advancement = output.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
      Map var10000 = this.criteria;
      Objects.requireNonNull(advancement);
      var10000.forEach(advancement::addCriterion);
      ShapedRecipe recipe = new ShapedRecipe((String)Objects.requireNonNullElse(this.group, ""), RecipeBuilder.determineBookCategory(this.category), pattern, new ItemStack(this.result, this.count), this.showNotification);
      output.accept(id, recipe, advancement.build(id.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/")));
   }

   private ShapedRecipePattern ensureValid(final ResourceKey id) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + String.valueOf(id.identifier()));
      } else {
         return ShapedRecipePattern.of(this.key, this.rows);
      }
   }
}
