package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class LootItemFunctions {
   public static final BiFunction IDENTITY = (stack, context) -> stack;
   public static final Codec TYPED_CODEC;
   public static final Codec ROOT_CODEC;
   public static final Codec CODEC;
   public static final LootItemFunctionType SET_COUNT;
   public static final LootItemFunctionType SET_ITEM;
   public static final LootItemFunctionType ENCHANT_WITH_LEVELS;
   public static final LootItemFunctionType ENCHANT_RANDOMLY;
   public static final LootItemFunctionType SET_ENCHANTMENTS;
   public static final LootItemFunctionType SET_CUSTOM_DATA;
   public static final LootItemFunctionType SET_COMPONENTS;
   public static final LootItemFunctionType FURNACE_SMELT;
   public static final LootItemFunctionType ENCHANTED_COUNT_INCREASE;
   public static final LootItemFunctionType SET_DAMAGE;
   public static final LootItemFunctionType SET_ATTRIBUTES;
   public static final LootItemFunctionType SET_NAME;
   public static final LootItemFunctionType EXPLORATION_MAP;
   public static final LootItemFunctionType SET_STEW_EFFECT;
   public static final LootItemFunctionType COPY_NAME;
   public static final LootItemFunctionType SET_CONTENTS;
   public static final LootItemFunctionType MODIFY_CONTENTS;
   public static final LootItemFunctionType FILTERED;
   public static final LootItemFunctionType LIMIT_COUNT;
   public static final LootItemFunctionType APPLY_BONUS;
   public static final LootItemFunctionType SET_LOOT_TABLE;
   public static final LootItemFunctionType EXPLOSION_DECAY;
   public static final LootItemFunctionType SET_LORE;
   public static final LootItemFunctionType FILL_PLAYER_HEAD;
   public static final LootItemFunctionType COPY_CUSTOM_DATA;
   public static final LootItemFunctionType COPY_STATE;
   public static final LootItemFunctionType SET_BANNER_PATTERN;
   public static final LootItemFunctionType SET_POTION;
   public static final LootItemFunctionType SET_INSTRUMENT;
   public static final LootItemFunctionType REFERENCE;
   public static final LootItemFunctionType SEQUENCE;
   public static final LootItemFunctionType COPY_COMPONENTS;
   public static final LootItemFunctionType SET_FIREWORKS;
   public static final LootItemFunctionType SET_FIREWORK_EXPLOSION;
   public static final LootItemFunctionType SET_BOOK_COVER;
   public static final LootItemFunctionType SET_WRITTEN_BOOK_PAGES;
   public static final LootItemFunctionType SET_WRITABLE_BOOK_PAGES;
   public static final LootItemFunctionType TOGGLE_TOOLTIPS;
   public static final LootItemFunctionType SET_OMINOUS_BOTTLE_AMPLIFIER;
   public static final LootItemFunctionType SET_CUSTOM_MODEL_DATA;
   public static final LootItemFunctionType DISCARD;

   private static LootItemFunctionType register(final String name, final MapCodec codec) {
      return (LootItemFunctionType)Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, (Identifier)Identifier.withDefaultNamespace(name), new LootItemFunctionType(codec));
   }

   public static BiFunction compose(final List functions) {
      List<BiFunction<ItemStack, LootContext, ItemStack>> terms = List.copyOf(functions);
      BiFunction var10000;
      switch (terms.size()) {
         case 0:
            var10000 = IDENTITY;
            break;
         case 1:
            var10000 = (BiFunction)terms.get(0);
            break;
         case 2:
            BiFunction<ItemStack, LootContext, ItemStack> first = (BiFunction)terms.get(0);
            BiFunction<ItemStack, LootContext, ItemStack> second = (BiFunction)terms.get(1);
            var10000 = (itemStack, context) -> (ItemStack)second.apply((ItemStack)first.apply(itemStack, context), context);
            break;
         default:
            var10000 = (itemStack, context) -> {
               for(BiFunction function : terms) {
                  itemStack = (ItemStack)function.apply(itemStack, context);
               }

               return itemStack;
            };
      }

      return var10000;
   }

   static {
      TYPED_CODEC = BuiltInRegistries.LOOT_FUNCTION_TYPE.byNameCodec().dispatch("function", LootItemFunction::getType, LootItemFunctionType::codec);
      ROOT_CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, SequenceFunction.INLINE_CODEC));
      CODEC = RegistryFileCodec.create(Registries.ITEM_MODIFIER, ROOT_CODEC);
      SET_COUNT = register("set_count", SetItemCountFunction.CODEC);
      SET_ITEM = register("set_item", SetItemFunction.CODEC);
      ENCHANT_WITH_LEVELS = register("enchant_with_levels", EnchantWithLevelsFunction.CODEC);
      ENCHANT_RANDOMLY = register("enchant_randomly", EnchantRandomlyFunction.CODEC);
      SET_ENCHANTMENTS = register("set_enchantments", SetEnchantmentsFunction.CODEC);
      SET_CUSTOM_DATA = register("set_custom_data", SetCustomDataFunction.CODEC);
      SET_COMPONENTS = register("set_components", SetComponentsFunction.CODEC);
      FURNACE_SMELT = register("furnace_smelt", SmeltItemFunction.CODEC);
      ENCHANTED_COUNT_INCREASE = register("enchanted_count_increase", EnchantedCountIncreaseFunction.CODEC);
      SET_DAMAGE = register("set_damage", SetItemDamageFunction.CODEC);
      SET_ATTRIBUTES = register("set_attributes", SetAttributesFunction.CODEC);
      SET_NAME = register("set_name", SetNameFunction.CODEC);
      EXPLORATION_MAP = register("exploration_map", ExplorationMapFunction.CODEC);
      SET_STEW_EFFECT = register("set_stew_effect", SetStewEffectFunction.CODEC);
      COPY_NAME = register("copy_name", CopyNameFunction.CODEC);
      SET_CONTENTS = register("set_contents", SetContainerContents.CODEC);
      MODIFY_CONTENTS = register("modify_contents", ModifyContainerContents.CODEC);
      FILTERED = register("filtered", FilteredFunction.CODEC);
      LIMIT_COUNT = register("limit_count", LimitCount.CODEC);
      APPLY_BONUS = register("apply_bonus", ApplyBonusCount.CODEC);
      SET_LOOT_TABLE = register("set_loot_table", SetContainerLootTable.CODEC);
      EXPLOSION_DECAY = register("explosion_decay", ApplyExplosionDecay.CODEC);
      SET_LORE = register("set_lore", SetLoreFunction.CODEC);
      FILL_PLAYER_HEAD = register("fill_player_head", FillPlayerHead.CODEC);
      COPY_CUSTOM_DATA = register("copy_custom_data", CopyCustomDataFunction.CODEC);
      COPY_STATE = register("copy_state", CopyBlockState.CODEC);
      SET_BANNER_PATTERN = register("set_banner_pattern", SetBannerPatternFunction.CODEC);
      SET_POTION = register("set_potion", SetPotionFunction.CODEC);
      SET_INSTRUMENT = register("set_instrument", SetInstrumentFunction.CODEC);
      REFERENCE = register("reference", FunctionReference.CODEC);
      SEQUENCE = register("sequence", SequenceFunction.CODEC);
      COPY_COMPONENTS = register("copy_components", CopyComponentsFunction.CODEC);
      SET_FIREWORKS = register("set_fireworks", SetFireworksFunction.CODEC);
      SET_FIREWORK_EXPLOSION = register("set_firework_explosion", SetFireworkExplosionFunction.CODEC);
      SET_BOOK_COVER = register("set_book_cover", SetBookCoverFunction.CODEC);
      SET_WRITTEN_BOOK_PAGES = register("set_written_book_pages", SetWrittenBookPagesFunction.CODEC);
      SET_WRITABLE_BOOK_PAGES = register("set_writable_book_pages", SetWritableBookPagesFunction.CODEC);
      TOGGLE_TOOLTIPS = register("toggle_tooltips", ToggleTooltips.CODEC);
      SET_OMINOUS_BOTTLE_AMPLIFIER = register("set_ominous_bottle_amplifier", SetOminousBottleAmplifierFunction.CODEC);
      SET_CUSTOM_MODEL_DATA = register("set_custom_model_data", SetCustomModelDataFunction.CODEC);
      DISCARD = register("discard", DiscardItem.CODEC);
   }
}
