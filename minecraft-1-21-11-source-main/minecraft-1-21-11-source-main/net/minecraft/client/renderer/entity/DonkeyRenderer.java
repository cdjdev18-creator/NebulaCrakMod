package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class DonkeyRenderer extends AbstractHorseRenderer {
   private final Identifier texture;

   public DonkeyRenderer(final EntityRendererProvider.Context context, final Type type) {
      super(context, new DonkeyModel(context.bakeLayer(type.model)), new DonkeyModel(context.bakeLayer(type.babyModel)));
      this.texture = type.texture;
      this.addLayer(new SimpleEquipmentLayer(this, context.getEquipmentRenderer(), type.saddleLayer, (state) -> state.saddle, new EquineSaddleModel(context.bakeLayer(type.saddleModel)), new EquineSaddleModel(context.bakeLayer(type.babySaddleModel))));
   }

   public Identifier getTextureLocation(final DonkeyRenderState state) {
      return this.texture;
   }

   public DonkeyRenderState createRenderState() {
      return new DonkeyRenderState();
   }

   public void extractRenderState(final AbstractChestedHorse entity, final DonkeyRenderState state, final float partialTicks) {
      super.extractRenderState((AbstractHorse)entity, (EquineRenderState)state, partialTicks);
      state.hasChest = entity.hasChest();
   }

   public static enum Type {
      DONKEY(Identifier.withDefaultNamespace("textures/entity/horse/donkey.png"), ModelLayers.DONKEY, ModelLayers.DONKEY_BABY, EquipmentClientInfo.LayerType.DONKEY_SADDLE, ModelLayers.DONKEY_SADDLE, ModelLayers.DONKEY_BABY_SADDLE),
      MULE(Identifier.withDefaultNamespace("textures/entity/horse/mule.png"), ModelLayers.MULE, ModelLayers.MULE_BABY, EquipmentClientInfo.LayerType.MULE_SADDLE, ModelLayers.MULE_SADDLE, ModelLayers.MULE_BABY_SADDLE);

      private final Identifier texture;
      private final ModelLayerLocation model;
      private final ModelLayerLocation babyModel;
      private final EquipmentClientInfo.LayerType saddleLayer;
      private final ModelLayerLocation saddleModel;
      private final ModelLayerLocation babySaddleModel;

      private Type(final Identifier texture, final ModelLayerLocation model, final ModelLayerLocation babyModel, final EquipmentClientInfo.LayerType saddleLayer, final ModelLayerLocation saddleModel, final ModelLayerLocation babySaddleModel) {
         this.texture = texture;
         this.model = model;
         this.babyModel = babyModel;
         this.saddleLayer = saddleLayer;
         this.saddleModel = saddleModel;
         this.babySaddleModel = babySaddleModel;
      }

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{DONKEY, MULE};
      }
   }
}
