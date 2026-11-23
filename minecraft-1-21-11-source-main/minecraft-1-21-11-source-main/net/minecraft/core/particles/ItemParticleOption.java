package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemParticleOption implements ParticleOptions {
   private static final Codec ITEM_CODEC;
   private final ParticleType type;
   private final ItemStack itemStack;

   public static MapCodec codec(final ParticleType type) {
      return ITEM_CODEC.xmap((stack) -> new ItemParticleOption(type, stack), (o) -> o.itemStack).fieldOf("item");
   }

   public static StreamCodec streamCodec(final ParticleType type) {
      return ItemStack.STREAM_CODEC.map((stack) -> new ItemParticleOption(type, stack), (o) -> o.itemStack);
   }

   public ItemParticleOption(final ParticleType type, final ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         throw new IllegalArgumentException("Empty stacks are not allowed");
      } else {
         this.type = type;
         this.itemStack = itemStack;
      }
   }

   public ParticleType getType() {
      return this.type;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }

   static {
      ITEM_CODEC = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, Item.CODEC, ItemStack::new);
   }
}
