package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class DebugEntryLookingAtFluid implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("looking_at_fluid");

   public void display(final DebugScreenDisplayer displayer, final @Nullable Level serverOrClientLevel, final @Nullable LevelChunk clientChunk, final @Nullable LevelChunk serverChunk) {
      Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
      Level clientOrServerLevel = (Level)(SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES ? serverOrClientLevel : Minecraft.getInstance().level);
      if (cameraEntity != null && clientOrServerLevel != null) {
         HitResult liquid = cameraEntity.pick((double)20.0F, 0.0F, true);
         List<String> result = new ArrayList();
         if (liquid.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult)liquid).getBlockPos();
            FluidState fluidState = clientOrServerLevel.getFluidState(pos);
            String var10001 = String.valueOf(ChatFormatting.UNDERLINE);
            result.add(var10001 + "Targeted Fluid: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            result.add(String.valueOf(BuiltInRegistries.FLUID.getKey(fluidState.getType())));

            for(Map.Entry entry : fluidState.getValues().entrySet()) {
               result.add(this.getPropertyValueString(entry));
            }

            Stream var10000 = fluidState.getTags().map((e) -> "#" + String.valueOf(e.location()));
            Objects.requireNonNull(result);
            var10000.forEach(result::add);
         }

         displayer.addToGroup(GROUP, (Collection)result);
      }
   }

   private String getPropertyValueString(final Map.Entry entry) {
      Property<?> property = (Property)entry.getKey();
      Comparable<?> value = (Comparable)entry.getValue();
      String valueString = Util.getPropertyName(property, value);
      if (Boolean.TRUE.equals(value)) {
         String var10000 = String.valueOf(ChatFormatting.GREEN);
         valueString = var10000 + valueString;
      } else if (Boolean.FALSE.equals(value)) {
         String var5 = String.valueOf(ChatFormatting.RED);
         valueString = var5 + valueString;
      }

      String var6 = property.getName();
      return var6 + ": " + valueString;
   }
}
