package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

public class RabbitRenderer extends AgeableMobRenderer {
   private static final Identifier RABBIT_BROWN_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/brown.png");
   private static final Identifier RABBIT_WHITE_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white.png");
   private static final Identifier RABBIT_BLACK_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/black.png");
   private static final Identifier RABBIT_GOLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/gold.png");
   private static final Identifier RABBIT_SALT_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/salt.png");
   private static final Identifier RABBIT_WHITE_SPLOTCHED_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white_splotched.png");
   private static final Identifier RABBIT_TOAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/toast.png");
   private static final Identifier RABBIT_EVIL_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/caerbannog.png");

   public RabbitRenderer(final EntityRendererProvider.Context context) {
      super(context, new RabbitModel(context.bakeLayer(ModelLayers.RABBIT)), new RabbitModel(context.bakeLayer(ModelLayers.RABBIT_BABY)), 0.3F);
   }

   public Identifier getTextureLocation(final RabbitRenderState state) {
      if (state.isToast) {
         return RABBIT_TOAST_LOCATION;
      } else {
         Identifier var10000;
         switch (state.variant) {
            case BROWN -> var10000 = RABBIT_BROWN_LOCATION;
            case WHITE -> var10000 = RABBIT_WHITE_LOCATION;
            case BLACK -> var10000 = RABBIT_BLACK_LOCATION;
            case GOLD -> var10000 = RABBIT_GOLD_LOCATION;
            case SALT -> var10000 = RABBIT_SALT_LOCATION;
            case WHITE_SPLOTCHED -> var10000 = RABBIT_WHITE_SPLOTCHED_LOCATION;
            case EVIL -> var10000 = RABBIT_EVIL_LOCATION;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }
   }

   public RabbitRenderState createRenderState() {
      return new RabbitRenderState();
   }

   public void extractRenderState(final Rabbit entity, final RabbitRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.jumpCompletion = entity.getJumpCompletion(partialTicks);
      state.isToast = checkMagicName(entity, "Toast");
      state.variant = entity.getVariant();
   }
}
