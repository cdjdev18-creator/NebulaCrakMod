package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public record TransmuteResult(Holder item, int count, DataComponentPatch components) {
   private static final Codec FULL_CODEC = RecordCodecBuilder.create((i) -> i.group(Item.CODEC.fieldOf("id").forGetter(TransmuteResult::item), ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(TransmuteResult::count), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(TransmuteResult::components)).apply(i, TransmuteResult::new));
   public static final Codec CODEC;
   public static final StreamCodec STREAM_CODEC;

   public TransmuteResult(final Item item) {
      this(item.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
   }

   private static DataResult validate(final TransmuteResult result) {
      return ItemStack.validateStrict(new ItemStack(result.item, result.count, result.components)).map((ignored) -> result);
   }

   public ItemStack apply(final ItemStack input) {
      ItemStack result = input.transmuteCopy((ItemLike)this.item.value(), this.count);
      result.applyComponents(this.components);
      return result;
   }

   public boolean isResultUnchanged(final ItemStack input) {
      ItemStack result = this.apply(input);
      return result.getCount() == 1 && ItemStack.isSameItemSameComponents(input, result);
   }

   public SlotDisplay display() {
      return new SlotDisplay.ItemStackSlotDisplay(new ItemStack(this.item, this.count, this.components));
   }

   static {
      CODEC = Codec.withAlternative(FULL_CODEC, Item.CODEC, (item) -> new TransmuteResult((Item)item.value())).validate(TransmuteResult::validate);
      STREAM_CODEC = StreamCodec.composite(Item.STREAM_CODEC, TransmuteResult::item, ByteBufCodecs.VAR_INT, TransmuteResult::count, DataComponentPatch.STREAM_CODEC, TransmuteResult::components, TransmuteResult::new);
   }
}
