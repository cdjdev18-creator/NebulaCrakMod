package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public interface EnchantmentEffectComponents {
   Codec COMPONENT_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.byNameCodec());
   Codec CODEC = DataComponentMap.makeCodec(COMPONENT_CODEC);
   DataComponentType DAMAGE_PROTECTION = register("damage_protection", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType DAMAGE_IMMUNITY = register("damage_immunity", (b) -> b.persistent(ConditionalEffect.codec(DamageImmunity.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType DAMAGE = register("damage", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType SMASH_DAMAGE_PER_FALLEN_BLOCK = register("smash_damage_per_fallen_block", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType KNOCKBACK = register("knockback", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType ARMOR_EFFECTIVENESS = register("armor_effectiveness", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType POST_ATTACK = register("post_attack", (b) -> b.persistent(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType POST_PIERCING_ATTACK = register("post_piercing_attack", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType HIT_BLOCK = register("hit_block", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.HIT_BLOCK).listOf()));
   DataComponentType ITEM_DAMAGE = register("item_damage", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));
   DataComponentType ATTRIBUTES = register("attributes", (b) -> b.persistent(EnchantmentAttributeEffect.CODEC.codec().listOf()));
   DataComponentType EQUIPMENT_DROPS = register("equipment_drops", (b) -> b.persistent(TargetedConditionalEffect.equipmentDropsCodec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));
   DataComponentType LOCATION_CHANGED = register("location_changed", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC, LootContextParamSets.ENCHANTED_LOCATION).listOf()));
   DataComponentType TICK = register("tick", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType AMMO_USE = register("ammo_use", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));
   DataComponentType PROJECTILE_PIERCING = register("projectile_piercing", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));
   DataComponentType PROJECTILE_SPAWNED = register("projectile_spawned", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType PROJECTILE_SPREAD = register("projectile_spread", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType PROJECTILE_COUNT = register("projectile_count", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType TRIDENT_RETURN_ACCELERATION = register("trident_return_acceleration", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType FISHING_TIME_REDUCTION = register("fishing_time_reduction", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType FISHING_LUCK_BONUS = register("fishing_luck_bonus", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType BLOCK_EXPERIENCE = register("block_experience", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));
   DataComponentType MOB_EXPERIENCE = register("mob_experience", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf()));
   DataComponentType REPAIR_WITH_XP = register("repair_with_xp", (b) -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));
   DataComponentType CROSSBOW_CHARGE_TIME = register("crossbow_charge_time", (b) -> b.persistent(EnchantmentValueEffect.CODEC));
   DataComponentType CROSSBOW_CHARGING_SOUNDS = register("crossbow_charging_sounds", (b) -> b.persistent(CrossbowItem.ChargingSounds.CODEC.listOf()));
   DataComponentType TRIDENT_SOUND = register("trident_sound", (b) -> b.persistent(SoundEvent.CODEC.listOf()));
   DataComponentType PREVENT_EQUIPMENT_DROP = register("prevent_equipment_drop", (b) -> b.persistent(Unit.CODEC));
   DataComponentType PREVENT_ARMOR_CHANGE = register("prevent_armor_change", (b) -> b.persistent(Unit.CODEC));
   DataComponentType TRIDENT_SPIN_ATTACK_STRENGTH = register("trident_spin_attack_strength", (b) -> b.persistent(EnchantmentValueEffect.CODEC));

   static DataComponentType bootstrap(final Registry registry) {
      return DAMAGE_PROTECTION;
   }

   private static DataComponentType register(final String id, final UnaryOperator builder) {
      return (DataComponentType)Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, (String)id, ((DataComponentType.Builder)builder.apply(DataComponentType.builder())).build());
   }
}
