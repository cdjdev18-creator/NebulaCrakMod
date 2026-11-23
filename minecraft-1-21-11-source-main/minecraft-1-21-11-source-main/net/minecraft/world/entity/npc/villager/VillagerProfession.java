package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public record VillagerProfession(Component name, Predicate heldJobSite, Predicate acquirableJobSite, ImmutableSet requestedItems, ImmutableSet secondaryPoi, @Nullable SoundEvent workSound) {
   public static final Predicate ALL_ACQUIRABLE_JOBS = (holder) -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
   public static final ResourceKey NONE = createKey("none");
   public static final ResourceKey ARMORER = createKey("armorer");
   public static final ResourceKey BUTCHER = createKey("butcher");
   public static final ResourceKey CARTOGRAPHER = createKey("cartographer");
   public static final ResourceKey CLERIC = createKey("cleric");
   public static final ResourceKey FARMER = createKey("farmer");
   public static final ResourceKey FISHERMAN = createKey("fisherman");
   public static final ResourceKey FLETCHER = createKey("fletcher");
   public static final ResourceKey LEATHERWORKER = createKey("leatherworker");
   public static final ResourceKey LIBRARIAN = createKey("librarian");
   public static final ResourceKey MASON = createKey("mason");
   public static final ResourceKey NITWIT = createKey("nitwit");
   public static final ResourceKey SHEPHERD = createKey("shepherd");
   public static final ResourceKey TOOLSMITH = createKey("toolsmith");
   public static final ResourceKey WEAPONSMITH = createKey("weaponsmith");

   private static ResourceKey createKey(final String name) {
      return ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace(name));
   }

   private static VillagerProfession register(final Registry registry, final ResourceKey name, final ResourceKey jobSite, final @Nullable SoundEvent workSound) {
      return register(registry, name, (poiType) -> poiType.is(jobSite), (poiType) -> poiType.is(jobSite), workSound);
   }

   private static VillagerProfession register(final Registry registry, final ResourceKey name, final Predicate heldJobSite, final Predicate acquirableJobSite, final @Nullable SoundEvent workSound) {
      return register(registry, name, heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound);
   }

   private static VillagerProfession register(final Registry registry, final ResourceKey name, final ResourceKey jobSite, final ImmutableSet requestedItems, final ImmutableSet secondaryPoi, final @Nullable SoundEvent workSound) {
      return register(registry, name, (poiType) -> poiType.is(jobSite), (poiType) -> poiType.is(jobSite), requestedItems, secondaryPoi, workSound);
   }

   private static VillagerProfession register(final Registry registry, final ResourceKey name, final Predicate heldJobSite, final Predicate acquirableJobSite, final ImmutableSet requestedItems, final ImmutableSet secondaryPoi, final @Nullable SoundEvent workSound) {
      String var10004 = name.identifier().getNamespace();
      return (VillagerProfession)Registry.register(registry, (ResourceKey)name, new VillagerProfession(Component.translatable("entity." + var10004 + ".villager." + name.identifier().getPath()), heldJobSite, acquirableJobSite, requestedItems, secondaryPoi, workSound));
   }

   public static VillagerProfession bootstrap(final Registry registry) {
      register(registry, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, (SoundEvent)null);
      register(registry, ARMORER, PoiTypes.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER);
      register(registry, BUTCHER, PoiTypes.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER);
      register(registry, CARTOGRAPHER, PoiTypes.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
      register(registry, CLERIC, PoiTypes.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC);
      register(registry, FARMER, PoiTypes.FARMER, ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL), ImmutableSet.of(Blocks.FARMLAND), SoundEvents.VILLAGER_WORK_FARMER);
      register(registry, FISHERMAN, PoiTypes.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN);
      register(registry, FLETCHER, PoiTypes.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER);
      register(registry, LEATHERWORKER, PoiTypes.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
      register(registry, LIBRARIAN, PoiTypes.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN);
      register(registry, MASON, PoiTypes.MASON, SoundEvents.VILLAGER_WORK_MASON);
      register(registry, NITWIT, PoiType.NONE, PoiType.NONE, (SoundEvent)null);
      register(registry, SHEPHERD, PoiTypes.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD);
      register(registry, TOOLSMITH, PoiTypes.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH);
      return register(registry, WEAPONSMITH, PoiTypes.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
   }
}
