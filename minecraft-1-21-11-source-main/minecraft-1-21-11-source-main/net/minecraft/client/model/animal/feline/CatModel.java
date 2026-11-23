package net.minecraft.client.model.animal.feline;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.MeshTransformer;

public class CatModel extends FelineModel {
   public static final MeshTransformer CAT_TRANSFORMER = MeshTransformer.scaling(0.8F);

   public CatModel(final ModelPart root) {
      super(root);
   }
}
