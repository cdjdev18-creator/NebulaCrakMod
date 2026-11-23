package net.minecraft.world.item.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ItemContainerContents implements TooltipProvider {
   private static final int NO_SLOT = -1;
   private static final int MAX_SIZE = 256;
   public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;
   private final NonNullList items;
   private final int hashCode;

   private ItemContainerContents(final NonNullList items) {
      if (items.size() > 256) {
         throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is 256");
      } else {
         this.items = items;
         this.hashCode = ItemStack.hashStackList(items);
      }
   }

   private ItemContainerContents(final int size) {
      this(NonNullList.withSize(size, ItemStack.EMPTY));
   }

   private ItemContainerContents(final List items) {
      this(items.size());

      for(int i = 0; i < items.size(); ++i) {
         this.items.set(i, (ItemStack)items.get(i));
      }

   }

   private static ItemContainerContents fromSlots(final List slots) {
      OptionalInt maxSlotIndex = slots.stream().mapToInt(Slot::index).max();
      if (maxSlotIndex.isEmpty()) {
         return EMPTY;
      } else {
         ItemContainerContents contents = new ItemContainerContents(maxSlotIndex.getAsInt() + 1);

         for(Slot slot : slots) {
            contents.items.set(slot.index(), slot.item());
         }

         return contents;
      }
   }

   public static ItemContainerContents fromItems(final List itemStacks) {
      int lastNonEmptySlot = findLastNonEmptySlot(itemStacks);
      if (lastNonEmptySlot == -1) {
         return EMPTY;
      } else {
         ItemContainerContents contents = new ItemContainerContents(lastNonEmptySlot + 1);

         for(int i = 0; i <= lastNonEmptySlot; ++i) {
            contents.items.set(i, ((ItemStack)itemStacks.get(i)).copy());
         }

         return contents;
      }
   }

   private static int findLastNonEmptySlot(final List itemStacks) {
      for(int i = itemStacks.size() - 1; i >= 0; --i) {
         if (!((ItemStack)itemStacks.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private List asSlots() {
      List<Slot> slots = new ArrayList();

      for(int i = 0; i < this.items.size(); ++i) {
         ItemStack item = (ItemStack)this.items.get(i);
         if (!item.isEmpty()) {
            slots.add(new Slot(i, item));
         }
      }

      return slots;
   }

   public void copyInto(final NonNullList destination) {
      for(int i = 0; i < destination.size(); ++i) {
         ItemStack item = i < this.items.size() ? (ItemStack)this.items.get(i) : ItemStack.EMPTY;
         destination.set(i, item.copy());
      }

   }

   public ItemStack copyOne() {
      return this.items.isEmpty() ? ItemStack.EMPTY : ((ItemStack)this.items.get(0)).copy();
   }

   public Stream stream() {
      return this.items.stream().map(ItemStack::copy);
   }

   public Stream nonEmptyStream() {
      return this.items.stream().filter((itemStack) -> !itemStack.isEmpty()).map(ItemStack::copy);
   }

   public Iterable nonEmptyItems() {
      return Iterables.filter(this.items, (itemStack) -> !itemStack.isEmpty());
   }

   public Iterable nonEmptyItemsCopy() {
      return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
   }

   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      } else {
         boolean var10000;
         if (obj instanceof ItemContainerContents) {
            ItemContainerContents contents = (ItemContainerContents)obj;
            if (ItemStack.listMatches(this.items, contents.items)) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public int hashCode() {
      return this.hashCode;
   }

   public void addToTooltip(final Item.TooltipContext context, final Consumer consumer, final TooltipFlag flag, final DataComponentGetter components) {
      int lineCount = 0;
      int itemCount = 0;

      for(ItemStack stack : this.nonEmptyItems()) {
         ++itemCount;
         if (lineCount <= 4) {
            ++lineCount;
            consumer.accept(Component.translatable("item.container.item_count", stack.getHoverName(), stack.getCount()));
         }
      }

      if (itemCount - lineCount > 0) {
         consumer.accept(Component.translatable("item.container.more_items", itemCount - lineCount).withStyle(ChatFormatting.ITALIC));
      }

   }

   static {
      CODEC = ItemContainerContents.Slot.CODEC.sizeLimitedListOf(256).xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
      STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(256)).map(ItemContainerContents::new, (c) -> c.items);
   }

   private static record Slot(int index, ItemStack item) {
      public static final Codec CODEC = RecordCodecBuilder.create((i) -> i.group(Codec.intRange(0, 255).fieldOf("slot").forGetter(Slot::index), ItemStack.CODEC.fieldOf("item").forGetter(Slot::item)).apply(i, Slot::new));
   }
}
