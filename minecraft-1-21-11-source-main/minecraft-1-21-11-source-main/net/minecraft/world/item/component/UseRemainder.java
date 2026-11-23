package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record UseRemainder(ItemStack convertInto) {
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;

   public ItemStack convertIntoRemainder(final ItemStack usedStack, final int stackCountBeforeUsing, final boolean hasInfiniteMaterials, final OnExtraCreatedRemainder onExtraCreatedRemainder) {
      if (hasInfiniteMaterials) {
         return usedStack;
      } else if (usedStack.getCount() >= stackCountBeforeUsing) {
         return usedStack;
      } else {
         ItemStack remainderStack = this.convertInto.copy();
         if (usedStack.isEmpty()) {
            return remainderStack;
         } else {
            onExtraCreatedRemainder.apply(remainderStack);
            return usedStack;
         }
      }
   }

   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         UseRemainder that = (UseRemainder)o;
         return ItemStack.matches(this.convertInto, that.convertInto);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return ItemStack.hashItemAndComponents(this.convertInto);
   }

   static {
      CODEC = ItemStack.CODEC.xmap(UseRemainder::new, UseRemainder::convertInto);
      STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, UseRemainder::convertInto, UseRemainder::new);
   }

   @FunctionalInterface
   public interface OnExtraCreatedRemainder {
      void apply(final ItemStack extraCreatedRemainder);
   }
}
