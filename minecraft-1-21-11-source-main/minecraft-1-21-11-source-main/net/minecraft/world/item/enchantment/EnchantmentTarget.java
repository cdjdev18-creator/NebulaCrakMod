package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EnchantmentTarget implements StringRepresentable {
   ATTACKER("attacker"),
   DAMAGING_ENTITY("damaging_entity"),
   VICTIM("victim");

   public static final Codec CODEC = StringRepresentable.fromEnum(EnchantmentTarget::values);
   private final String id;

   private EnchantmentTarget(final String id) {
      this.id = id;
   }

   public String getSerializedName() {
      return this.id;
   }

   // $FF: synthetic method
   private static EnchantmentTarget[] $values() {
      return new EnchantmentTarget[]{ATTACKER, DAMAGING_ENTITY, VICTIM};
   }
}
