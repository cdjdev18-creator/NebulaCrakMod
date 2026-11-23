package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClientboundSetEntityMotionPacket implements Packet {
   public static final StreamCodec STREAM_CODEC = Packet.codec(ClientboundSetEntityMotionPacket::write, ClientboundSetEntityMotionPacket::new);
   private final int id;
   private final Vec3 movement;

   public ClientboundSetEntityMotionPacket(final Entity entity) {
      this(entity.getId(), entity.getDeltaMovement());
   }

   public ClientboundSetEntityMotionPacket(final int id, final Vec3 movement) {
      this.id = id;
      this.movement = movement;
   }

   private ClientboundSetEntityMotionPacket(final FriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.movement = input.readLpVec3();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeLpVec3(this.movement);
   }

   public PacketType type() {
      return GamePacketTypes.CLIENTBOUND_SET_ENTITY_MOTION;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetEntityMotion(this);
   }

   public int getId() {
      return this.id;
   }

   public Vec3 getMovement() {
      return this.movement;
   }
}
