package net.minecraft.network.protocol.game;

import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ServerboundInteractPacket implements Packet {
   public static final StreamCodec STREAM_CODEC = Packet.codec(ServerboundInteractPacket::write, ServerboundInteractPacket::new);
   private final int entityId;
   private final Action action;
   private final boolean usingSecondaryAction;
   private static final Action ATTACK_ACTION = new Action() {
      public ActionType getType() {
         return ServerboundInteractPacket.ActionType.ATTACK;
      }

      public void dispatch(final Handler handler) {
         handler.onAttack();
      }

      public void write(final FriendlyByteBuf output) {
      }
   };

   private ServerboundInteractPacket(final int entityId, final boolean usingSecondaryAction, final Action action) {
      this.entityId = entityId;
      this.action = action;
      this.usingSecondaryAction = usingSecondaryAction;
   }

   public static ServerboundInteractPacket createAttackPacket(final Entity entity, final boolean usingSecondaryAction) {
      return new ServerboundInteractPacket(entity.getId(), usingSecondaryAction, ATTACK_ACTION);
   }

   public static ServerboundInteractPacket createInteractionPacket(final Entity entity, final boolean usingSecondaryAction, final InteractionHand hand) {
      return new ServerboundInteractPacket(entity.getId(), usingSecondaryAction, new InteractionAction(hand));
   }

   public static ServerboundInteractPacket createInteractionPacket(final Entity entity, final boolean usingSecondaryAction, final InteractionHand hand, final Vec3 location) {
      return new ServerboundInteractPacket(entity.getId(), usingSecondaryAction, new InteractionAtLocationAction(hand, location));
   }

   private ServerboundInteractPacket(final FriendlyByteBuf input) {
      this.entityId = input.readVarInt();
      ActionType type = (ActionType)input.readEnum(ActionType.class);
      this.action = (Action)type.reader.apply(input);
      this.usingSecondaryAction = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.entityId);
      output.writeEnum(this.action.getType());
      this.action.write(output);
      output.writeBoolean(this.usingSecondaryAction);
   }

   public PacketType type() {
      return GamePacketTypes.SERVERBOUND_INTERACT;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleInteract(this);
   }

   public @Nullable Entity getTarget(final ServerLevel level) {
      return level.getEntityOrPart(this.entityId);
   }

   public boolean isUsingSecondaryAction() {
      return this.usingSecondaryAction;
   }

   public boolean isWithinRange(final ServerPlayer player, final AABB aabb, final double buffer) {
      return this.action.getType() == ServerboundInteractPacket.ActionType.ATTACK ? player.isWithinAttackRange(aabb, buffer) : player.isWithinEntityInteractionRange(aabb, buffer);
   }

   public void dispatch(final Handler handler) {
      this.action.dispatch(handler);
   }

   private static enum ActionType {
      INTERACT(InteractionAction::new),
      ATTACK((input) -> ServerboundInteractPacket.ATTACK_ACTION),
      INTERACT_AT(InteractionAtLocationAction::new);

      private final Function reader;

      private ActionType(final Function reader) {
         this.reader = reader;
      }

      // $FF: synthetic method
      private static ActionType[] $values() {
         return new ActionType[]{INTERACT, ATTACK, INTERACT_AT};
      }
   }

   private static class InteractionAction implements Action {
      private final InteractionHand hand;

      private InteractionAction(final InteractionHand hand) {
         this.hand = hand;
      }

      private InteractionAction(final FriendlyByteBuf input) {
         this.hand = (InteractionHand)input.readEnum(InteractionHand.class);
      }

      public ActionType getType() {
         return ServerboundInteractPacket.ActionType.INTERACT;
      }

      public void dispatch(final Handler handler) {
         handler.onInteraction(this.hand);
      }

      public void write(final FriendlyByteBuf output) {
         output.writeEnum(this.hand);
      }
   }

   private static class InteractionAtLocationAction implements Action {
      private final InteractionHand hand;
      private final Vec3 location;

      private InteractionAtLocationAction(final InteractionHand hand, final Vec3 location) {
         this.hand = hand;
         this.location = location;
      }

      private InteractionAtLocationAction(final FriendlyByteBuf input) {
         this.location = new Vec3((double)input.readFloat(), (double)input.readFloat(), (double)input.readFloat());
         this.hand = (InteractionHand)input.readEnum(InteractionHand.class);
      }

      public ActionType getType() {
         return ServerboundInteractPacket.ActionType.INTERACT_AT;
      }

      public void dispatch(final Handler handler) {
         handler.onInteraction(this.hand, this.location);
      }

      public void write(final FriendlyByteBuf output) {
         output.writeFloat((float)this.location.x);
         output.writeFloat((float)this.location.y);
         output.writeFloat((float)this.location.z);
         output.writeEnum(this.hand);
      }
   }

   private interface Action {
      ActionType getType();

      void dispatch(Handler handler);

      void write(FriendlyByteBuf output);
   }

   public interface Handler {
      void onInteraction(final InteractionHand hand);

      void onInteraction(final InteractionHand hand, final Vec3 location);

      void onAttack();
   }
}
