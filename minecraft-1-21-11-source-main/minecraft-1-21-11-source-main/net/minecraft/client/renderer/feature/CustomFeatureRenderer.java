package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;

public class CustomFeatureRenderer {
   public void render(final SubmitNodeCollection nodeCollection, final MultiBufferSource.BufferSource bufferSource) {
      Storage storage = nodeCollection.getCustomGeometrySubmits();

      for(Map.Entry entry : storage.customGeometrySubmits.entrySet()) {
         VertexConsumer buffer = bufferSource.getBuffer((RenderType)entry.getKey());

         for(SubmitNodeStorage.CustomGeometrySubmit customGeometrySubmit : (List)entry.getValue()) {
            customGeometrySubmit.customGeometryRenderer().render(customGeometrySubmit.pose(), buffer);
         }
      }

   }

   public static class Storage {
      private final Map customGeometrySubmits = new HashMap();
      private final Set customGeometrySubmitsUsage = new ObjectOpenHashSet();

      public void add(final PoseStack poseStack, final RenderType renderType, final SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
         List<SubmitNodeStorage.CustomGeometrySubmit> submits = (List)this.customGeometrySubmits.computeIfAbsent(renderType, (rt) -> new ArrayList());
         submits.add(new SubmitNodeStorage.CustomGeometrySubmit(poseStack.last().copy(), customGeometryRenderer));
      }

      public void clear() {
         for(Map.Entry entry : this.customGeometrySubmits.entrySet()) {
            if (!((List)entry.getValue()).isEmpty()) {
               this.customGeometrySubmitsUsage.add((RenderType)entry.getKey());
               ((List)entry.getValue()).clear();
            }
         }

      }

      public void endFrame() {
         this.customGeometrySubmits.keySet().removeIf((renderType) -> !this.customGeometrySubmitsUsage.contains(renderType));
         this.customGeometrySubmitsUsage.clear();
      }
   }
}
