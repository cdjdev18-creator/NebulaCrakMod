package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class LootPoolEntries {
   public static final Codec CODEC;
   public static final LootPoolEntryType EMPTY;
   public static final LootPoolEntryType ITEM;
   public static final LootPoolEntryType LOOT_TABLE;
   public static final LootPoolEntryType DYNAMIC;
   public static final LootPoolEntryType TAG;
   public static final LootPoolEntryType SLOTS;
   public static final LootPoolEntryType ALTERNATIVES;
   public static final LootPoolEntryType SEQUENCE;
   public static final LootPoolEntryType GROUP;

   private static LootPoolEntryType register(final String name, final MapCodec codec) {
      return (LootPoolEntryType)Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, (Identifier)Identifier.withDefaultNamespace(name), new LootPoolEntryType(codec));
   }

   static {
      CODEC = BuiltInRegistries.LOOT_POOL_ENTRY_TYPE.byNameCodec().dispatch(LootPoolEntryContainer::getType, LootPoolEntryType::codec);
      EMPTY = register("empty", EmptyLootItem.CODEC);
      ITEM = register("item", LootItem.CODEC);
      LOOT_TABLE = register("loot_table", NestedLootTable.CODEC);
      DYNAMIC = register("dynamic", DynamicLoot.CODEC);
      TAG = register("tag", TagEntry.CODEC);
      SLOTS = register("slots", SlotLoot.CODEC);
      ALTERNATIVES = register("alternatives", AlternativesEntry.CODEC);
      SEQUENCE = register("sequence", SequentialEntry.CODEC);
      GROUP = register("group", EntryGroup.CODEC);
   }
}
