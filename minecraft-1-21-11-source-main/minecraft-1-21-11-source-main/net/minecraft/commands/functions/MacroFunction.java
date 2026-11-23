package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class MacroFunction implements CommandFunction {
   private static final DecimalFormat DECIMAL_FORMAT;
   private static final int MAX_CACHE_ENTRIES = 8;
   private final List parameters;
   private final Object2ObjectLinkedOpenHashMap cache = new Object2ObjectLinkedOpenHashMap(8, 0.25F);
   private final Identifier id;
   private final List entries;

   public MacroFunction(final Identifier id, final List entries, final List parameters) {
      this.id = id;
      this.entries = entries;
      this.parameters = parameters;
   }

   public Identifier id() {
      return this.id;
   }

   public InstantiatedFunction instantiate(final @Nullable CompoundTag arguments, final CommandDispatcher dispatcher) throws FunctionInstantiationException {
      if (arguments == null) {
         throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", Component.translationArg(this.id())));
      } else {
         List<String> parameterValues = new ArrayList(this.parameters.size());

         for(String argument : this.parameters) {
            Tag argumentValue = arguments.get(argument);
            if (argumentValue == null) {
               throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_argument", Component.translationArg(this.id()), argument));
            }

            parameterValues.add(stringify(argumentValue));
         }

         InstantiatedFunction<T> cachedFunction = (InstantiatedFunction)this.cache.getAndMoveToLast(parameterValues);
         if (cachedFunction != null) {
            return cachedFunction;
         } else {
            if (this.cache.size() >= 8) {
               this.cache.removeFirst();
            }

            InstantiatedFunction<T> function = this.substituteAndParse(this.parameters, parameterValues, dispatcher);
            this.cache.put(parameterValues, function);
            return function;
         }
      }
   }

   private static String stringify(final Tag tag) {
      Objects.requireNonNull(tag);
      byte var2 = 0;
      String var30;
      //$FF: var2->value
      //0->net/minecraft/nbt/FloatTag
      //1->net/minecraft/nbt/DoubleTag
      //2->net/minecraft/nbt/ByteTag
      //3->net/minecraft/nbt/ShortTag
      //4->net/minecraft/nbt/LongTag
      //5->net/minecraft/nbt/StringTag
      switch (tag.typeSwitch<invokedynamic>(tag, var2)) {
         case 0:
            FloatTag var3 = (FloatTag)tag;
            FloatTag var39 = var3;

            try {
               var40 = var39.value();
            } catch (Throwable var23) {
               throw new MatchException(var23.toString(), var23);
            }

            float value = var40;
            var30 = DECIMAL_FORMAT.format((double)value);
            break;
         case 1:
            DoubleTag value = (DoubleTag)tag;
            DoubleTag var37 = value;

            try {
               var38 = var37.value();
            } catch (Throwable var22) {
               throw new MatchException(var22.toString(), var22);
            }

            double value = var38;
            var30 = DECIMAL_FORMAT.format(value);
            break;
         case 2:
            ByteTag value = (ByteTag)tag;
            ByteTag var35 = value;

            try {
               var36 = var35.value();
            } catch (Throwable var21) {
               throw new MatchException(var21.toString(), var21);
            }

            byte value = var36;
            var30 = String.valueOf(value);
            break;
         case 3:
            ShortTag value = (ShortTag)tag;
            ShortTag var33 = value;

            try {
               var34 = var33.value();
            } catch (Throwable var20) {
               throw new MatchException(var20.toString(), var20);
            }

            short value = var34;
            var30 = String.valueOf(value);
            break;
         case 4:
            LongTag value = (LongTag)tag;
            LongTag var31 = value;

            try {
               var32 = var31.value();
            } catch (Throwable var19) {
               throw new MatchException(var19.toString(), var19);
            }

            long value = var32;
            var30 = String.valueOf(value);
            break;
         case 5:
            StringTag value = (StringTag)tag;
            StringTag var10000 = value;

            try {
               var29 = var10000.value();
            } catch (Throwable var18) {
               throw new MatchException(var18.toString(), var18);
            }

            String value = var29;
            var30 = value;
            break;
         default:
            var30 = tag.toString();
      }

      return var30;
   }

   private static void lookupValues(final List values, final IntList indicesToSelect, final List selectedValuesOutput) {
      selectedValuesOutput.clear();
      indicesToSelect.forEach((index) -> selectedValuesOutput.add((String)values.get(index)));
   }

   private InstantiatedFunction substituteAndParse(final List keys, final List values, final CommandDispatcher dispatcher) throws FunctionInstantiationException {
      List<UnboundEntryAction<T>> newEntries = new ArrayList(this.entries.size());
      List<String> entryArguments = new ArrayList(values.size());

      for(Entry entry : this.entries) {
         lookupValues(values, entry.parameters(), entryArguments);
         newEntries.add(entry.instantiate(entryArguments, dispatcher, this.id));
      }

      return new PlainTextFunction(this.id().withPath((UnaryOperator)((id) -> id + "/" + keys.hashCode())), newEntries);
   }

   static {
      DECIMAL_FORMAT = (DecimalFormat)Util.make(new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ROOT)), (format) -> format.setMaximumFractionDigits(15));
   }

   static class PlainTextEntry implements Entry {
      private final UnboundEntryAction compiledAction;

      public PlainTextEntry(final UnboundEntryAction compiledAction) {
         this.compiledAction = compiledAction;
      }

      public IntList parameters() {
         return IntLists.emptyList();
      }

      public UnboundEntryAction instantiate(final List substitutions, final CommandDispatcher dispatcher, final Identifier functionId) {
         return this.compiledAction;
      }
   }

   static class MacroEntry implements Entry {
      private final StringTemplate template;
      private final IntList parameters;
      private final ExecutionCommandSource compilationContext;

      public MacroEntry(final StringTemplate template, final IntList parameters, final ExecutionCommandSource compilationContext) {
         this.template = template;
         this.parameters = parameters;
         this.compilationContext = compilationContext;
      }

      public IntList parameters() {
         return this.parameters;
      }

      public UnboundEntryAction instantiate(final List substitutions, final CommandDispatcher dispatcher, final Identifier functionId) throws FunctionInstantiationException {
         String command = this.template.substitute(substitutions);

         try {
            return CommandFunction.parseCommand(dispatcher, this.compilationContext, new StringReader(command));
         } catch (CommandSyntaxException e) {
            throw new FunctionInstantiationException(Component.translatable("commands.function.error.parse", Component.translationArg(functionId), command, e.getMessage()));
         }
      }
   }

   interface Entry {
      IntList parameters();

      UnboundEntryAction instantiate(List substitutions, CommandDispatcher dispatcher, Identifier funtionId) throws FunctionInstantiationException;
   }
}
