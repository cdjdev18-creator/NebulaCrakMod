package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;

public record StorageNbtProvider(Identifier id) implements NbtProvider {
   public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Identifier.CODEC.fieldOf("source").forGetter(StorageNbtProvider::id)).apply(i, StorageNbtProvider::new));

   public LootNbtProviderType getType() {
      return NbtProviders.STORAGE;
   }

   public Tag get(final LootContext context) {
      return context.getLevel().getServer().getCommandStorage().get(this.id);
   }

   public Set getReferencedContextParams() {
      return Set.of();
   }
}
