package net.minecraft.world.item.crafting.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.FuelValues;

public interface SlotDisplay {
   Codec CODEC = BuiltInRegistries.SLOT_DISPLAY.byNameCodec().dispatch(SlotDisplay::type, Type::codec);
   StreamCodec STREAM_CODEC = ByteBufCodecs.registry(Registries.SLOT_DISPLAY).dispatch(SlotDisplay::type, Type::streamCodec);

   Stream resolve(ContextMap context, DisplayContentsFactory builder);

   Type type();

   default boolean isEnabled(final FeatureFlagSet enabledFeatures) {
      return true;
   }

   default List resolveForStacks(final ContextMap context) {
      return this.resolve(context, SlotDisplay.ItemStackContentsFactory.INSTANCE).toList();
   }

   default ItemStack resolveForFirstStack(final ContextMap context) {
      return (ItemStack)this.resolve(context, SlotDisplay.ItemStackContentsFactory.INSTANCE).findFirst().orElse(ItemStack.EMPTY);
   }

   public static record Type(MapCodec codec, StreamCodec streamCodec) {
   }

   public static class ItemStackContentsFactory implements DisplayContentsFactory.ForStacks {
      public static final ItemStackContentsFactory INSTANCE = new ItemStackContentsFactory();

      public ItemStack forStack(final ItemStack stack) {
         return stack;
      }
   }

   public static class Empty implements SlotDisplay {
      public static final Empty INSTANCE = new Empty();
      public static final MapCodec MAP_CODEC;
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      private Empty() {
      }

      public Type type() {
         return TYPE;
      }

      public String toString() {
         return "<empty>";
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         return Stream.empty();
      }

      static {
         MAP_CODEC = MapCodec.unit(INSTANCE);
         STREAM_CODEC = StreamCodec.unit(INSTANCE);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static class AnyFuel implements SlotDisplay {
      public static final AnyFuel INSTANCE = new AnyFuel();
      public static final MapCodec MAP_CODEC;
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      private AnyFuel() {
      }

      public Type type() {
         return TYPE;
      }

      public String toString() {
         return "<any fuel>";
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks stacks) {
            FuelValues fuelValues = (FuelValues)context.getOptional(SlotDisplayContext.FUEL_VALUES);
            if (fuelValues != null) {
               Stream var10000 = fuelValues.fuelItems().stream();
               Objects.requireNonNull(stacks);
               return var10000.map(stacks::forStack);
            }
         }

         return Stream.empty();
      }

      static {
         MAP_CODEC = MapCodec.unit(INSTANCE);
         STREAM_CODEC = StreamCodec.unit(INSTANCE);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Holder pattern) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(SlotDisplay.CODEC.fieldOf("base").forGetter(SmithingTrimDemoSlotDisplay::base), SlotDisplay.CODEC.fieldOf("material").forGetter(SmithingTrimDemoSlotDisplay::material), TrimPattern.CODEC.fieldOf("pattern").forGetter(SmithingTrimDemoSlotDisplay::pattern)).apply(i, SmithingTrimDemoSlotDisplay::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks stacks) {
            HolderLookup.Provider registries = (HolderLookup.Provider)context.getOptional(SlotDisplayContext.REGISTRIES);
            if (registries != null) {
               RandomSource randomSource = RandomSource.create((long)System.identityHashCode(this));
               List<ItemStack> bases = this.base.resolveForStacks(context);
               if (bases.isEmpty()) {
                  return Stream.empty();
               }

               List<ItemStack> materials = this.material.resolveForStacks(context);
               if (materials.isEmpty()) {
                  return Stream.empty();
               }

               Stream var10000 = Stream.generate(() -> {
                  ItemStack base = (ItemStack)Util.getRandom(bases, randomSource);
                  ItemStack material = (ItemStack)Util.getRandom(materials, randomSource);
                  return SmithingTrimRecipe.applyTrim(registries, base, material, this.pattern);
               }).limit(256L).filter((s) -> !s.isEmpty()).limit(16L);
               Objects.requireNonNull(stacks);
               return var10000.map(stacks::forStack);
            }
         }

         return Stream.empty();
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC, SmithingTrimDemoSlotDisplay::base, SlotDisplay.STREAM_CODEC, SmithingTrimDemoSlotDisplay::material, TrimPattern.STREAM_CODEC, SmithingTrimDemoSlotDisplay::pattern, SmithingTrimDemoSlotDisplay::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record ItemSlotDisplay(Holder item) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Item.CODEC.fieldOf("item").forGetter(ItemSlotDisplay::item)).apply(i, ItemSlotDisplay::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public ItemSlotDisplay(final Item item) {
         this((Holder)item.builtInRegistryHolder());
      }

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks stacks) {
            return Stream.of(stacks.forStack(this.item));
         } else {
            return Stream.empty();
         }
      }

      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return ((Item)this.item.value()).isEnabled(enabledFeatures);
      }

      static {
         STREAM_CODEC = StreamCodec.composite(Item.STREAM_CODEC, ItemSlotDisplay::item, ItemSlotDisplay::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record ItemStackSlotDisplay(ItemStack stack) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(ItemStack.STRICT_CODEC.fieldOf("item").forGetter(ItemStackSlotDisplay::stack)).apply(i, ItemStackSlotDisplay::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks stacks) {
            return Stream.of(stacks.forStack(this.stack));
         } else {
            return Stream.empty();
         }
      }

      public boolean equals(final Object o) {
         boolean var10000;
         if (this != o) {
            label26: {
               if (o instanceof ItemStackSlotDisplay) {
                  ItemStackSlotDisplay that = (ItemStackSlotDisplay)o;
                  if (ItemStack.matches(this.stack, that.stack)) {
                     break label26;
                  }
               }

               var10000 = false;
               return var10000;
            }
         }

         var10000 = true;
         return var10000;
      }

      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.stack.getItem().isEnabled(enabledFeatures);
      }

      static {
         STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, ItemStackSlotDisplay::stack, ItemStackSlotDisplay::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record TagSlotDisplay(TagKey tag) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TagSlotDisplay::tag)).apply(i, TagSlotDisplay::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks stacks) {
            HolderLookup.Provider registries = (HolderLookup.Provider)context.getOptional(SlotDisplayContext.REGISTRIES);
            if (registries != null) {
               return registries.lookupOrThrow(Registries.ITEM).get(this.tag).map((t) -> {
                  Stream var10000 = t.stream();
                  Objects.requireNonNull(stacks);
                  return var10000.map(stacks::forStack);
               }).stream().flatMap((s) -> s);
            }
         }

         return Stream.empty();
      }

      static {
         STREAM_CODEC = StreamCodec.composite(TagKey.streamCodec(Registries.ITEM), TagSlotDisplay::tag, TagSlotDisplay::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record Composite(List contents) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(SlotDisplay.CODEC.listOf().fieldOf("contents").forGetter(Composite::contents)).apply(i, Composite::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         return this.contents.stream().flatMap((d) -> d.resolve(context, factory));
      }

      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.contents.stream().allMatch((c) -> c.isEnabled(enabledFeatures));
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC.apply(ByteBufCodecs.list()), Composite::contents, Composite::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }

   public static record WithRemainder(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay {
      public static final MapCodec MAP_CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(SlotDisplay.CODEC.fieldOf("input").forGetter(WithRemainder::input), SlotDisplay.CODEC.fieldOf("remainder").forGetter(WithRemainder::remainder)).apply(i, WithRemainder::new));
      public static final StreamCodec STREAM_CODEC;
      public static final Type TYPE;

      public Type type() {
         return TYPE;
      }

      public Stream resolve(final ContextMap context, final DisplayContentsFactory factory) {
         if (factory instanceof DisplayContentsFactory.ForRemainders remainders) {
            List<T> resolvedRemainders = this.remainder.resolve(context, factory).toList();
            return this.input.resolve(context, factory).map((input) -> remainders.addRemainder(input, resolvedRemainders));
         } else {
            return this.input.resolve(context, factory);
         }
      }

      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.input.isEnabled(enabledFeatures) && this.remainder.isEnabled(enabledFeatures);
      }

      static {
         STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC, WithRemainder::input, SlotDisplay.STREAM_CODEC, WithRemainder::remainder, WithRemainder::new);
         TYPE = new Type(MAP_CODEC, STREAM_CODEC);
      }
   }
}
