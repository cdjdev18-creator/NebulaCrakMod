package net.minecraft.network.protocol.game;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundSetTimePacket(long gameTime, long dayTime, boolean tickDayTime) implements Packet {
   public static final StreamCodec STREAM_CODEC;

   public PacketType type() {
      return GamePacketTypes.CLIENTBOUND_SET_TIME;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetTime(this);
   }

   static {
      STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.LONG, ClientboundSetTimePacket::gameTime, ByteBufCodecs.LONG, ClientboundSetTimePacket::dayTime, ByteBufCodecs.BOOL, ClientboundSetTimePacket::tickDayTime, ClientboundSetTimePacket::new);
   }
}
