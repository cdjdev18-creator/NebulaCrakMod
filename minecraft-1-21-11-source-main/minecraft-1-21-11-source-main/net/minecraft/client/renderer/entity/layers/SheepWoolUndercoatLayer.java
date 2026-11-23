package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public class SheepWoolUndercoatLayer extends RenderLayer {
   private static final Identifier SHEEP_WOOL_UNDERCOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_undercoat.png");
   private final EntityModel adultModel;
   private final EntityModel babyModel;

   public SheepWoolUndercoatLayer(final RenderLayerParent renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL_UNDERCOAT));
      this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL_UNDERCOAT));
   }

   public void submit(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final SheepRenderState state, final float yRot, final float xRot) {
      if (!state.isInvisible && (state.isJebSheep || state.woolColor != DyeColor.WHITE)) {
         EntityModel<SheepRenderState> model = state.isBaby ? this.babyModel : this.adultModel;
         coloredCutoutModelCopyLayerRender(model, SHEEP_WOOL_UNDERCOAT_LOCATION, poseStack, submitNodeCollector, lightCoords, state, state.getWoolColor(), 1);
      }
   }
}
