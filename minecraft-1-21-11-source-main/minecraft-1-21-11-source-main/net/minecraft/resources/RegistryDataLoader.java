package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.timeline.Timeline;
import org.slf4j.Logger;

public class RegistryDataLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Comparator ERROR_KEY_COMPARATOR = Comparator.comparing(ResourceKey::registry).thenComparing(ResourceKey::identifier);
   private static final RegistrationInfo NETWORK_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());
   private static final Function REGISTRATION_INFO_CACHE = Util.memoize((Function)((knownPack) -> {
      Lifecycle lifecycle = (Lifecycle)knownPack.map(KnownPack::isVanilla).map((info) -> Lifecycle.stable()).orElse(Lifecycle.experimental());
      return new RegistrationInfo(knownPack, lifecycle);
   }));
   public static final List WORLDGEN_REGISTRIES;
   public static final List DIMENSION_REGISTRIES;
   public static final List SYNCHRONIZED_REGISTRIES;

   public static RegistryAccess.Frozen load(final ResourceManager resourceManager, final List contextRegistries, final List registriesToLoad) {
      return load((LoadingFunction)((loader, context) -> loader.loadFromResources(resourceManager, context)), contextRegistries, registriesToLoad);
   }

   public static RegistryAccess.Frozen load(final Map entries, final ResourceProvider knownDataSource, final List contextRegistries, final List registriesToLoad) {
      return load((LoadingFunction)((loader, context) -> loader.loadFromNetwork(entries, knownDataSource, context)), contextRegistries, registriesToLoad);
   }

   private static RegistryAccess.Frozen load(final LoadingFunction loadingFunction, final List contextRegistries, final List registriesToLoad) {
      Map<ResourceKey<?>, Exception> loadingErrors = new HashMap();
      List<Loader<?>> newRegistriesAndLoaders = (List)registriesToLoad.stream().map((r) -> r.create(Lifecycle.stable(), loadingErrors)).collect(Collectors.toUnmodifiableList());
      RegistryOps.RegistryInfoLookup contextAndNewRegistries = createContext(contextRegistries, newRegistriesAndLoaders);
      newRegistriesAndLoaders.forEach((loader) -> loadingFunction.apply(loader, contextAndNewRegistries));
      newRegistriesAndLoaders.forEach((p) -> {
         Registry<?> registry = p.registry();

         try {
            registry.freeze();
         } catch (Exception e) {
            loadingErrors.put(registry.key(), e);
         }

         if (p.data.requiredNonEmpty && registry.size() == 0) {
            loadingErrors.put(registry.key(), new IllegalStateException("Registry must be non-empty: " + String.valueOf(registry.key().identifier())));
         }

      });
      if (!loadingErrors.isEmpty()) {
         throw logErrors(loadingErrors);
      } else {
         return (new RegistryAccess.ImmutableRegistryAccess(newRegistriesAndLoaders.stream().map(Loader::registry).toList())).freeze();
      }
   }

   private static RegistryOps.RegistryInfoLookup createContext(final List contextRegistries, final List newRegistriesAndLoaders) {
      final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> result = new HashMap();
      contextRegistries.forEach((e) -> result.put(e.key(), createInfoForContextRegistry(e)));
      newRegistriesAndLoaders.forEach((e) -> result.put(e.registry.key(), createInfoForNewRegistry(e.registry)));
      return new RegistryOps.RegistryInfoLookup() {
         public Optional lookup(final ResourceKey key) {
            return Optional.ofNullable((RegistryOps.RegistryInfo)result.get(key));
         }
      };
   }

   private static RegistryOps.RegistryInfo createInfoForNewRegistry(final WritableRegistry e) {
      return new RegistryOps.RegistryInfo(e, e.createRegistrationLookup(), e.registryLifecycle());
   }

   private static RegistryOps.RegistryInfo createInfoForContextRegistry(final HolderLookup.RegistryLookup lookup) {
      return new RegistryOps.RegistryInfo(lookup, lookup, lookup.registryLifecycle());
   }

   private static ReportedException logErrors(final Map loadingErrors) {
      printFullDetailsToLog(loadingErrors);
      return createReportWithBriefInfo(loadingErrors);
   }

   private static void printFullDetailsToLog(final Map loadingErrors) {
      StringWriter collectedErrors = new StringWriter();
      PrintWriter errorPrinter = new PrintWriter(collectedErrors);
      Map<Identifier, Map<Identifier, Exception>> errorsByRegistry = (Map)loadingErrors.entrySet().stream().collect(Collectors.groupingBy((e) -> ((ResourceKey)e.getKey()).registry(), Collectors.toMap((e) -> ((ResourceKey)e.getKey()).identifier(), Map.Entry::getValue)));
      errorsByRegistry.entrySet().stream().sorted(Entry.comparingByKey()).forEach((registryEntry) -> {
         errorPrinter.printf(Locale.ROOT, "> Errors in registry %s:%n", registryEntry.getKey());
         ((Map)registryEntry.getValue()).entrySet().stream().sorted(Entry.comparingByKey()).forEach((elementError) -> {
            errorPrinter.printf(Locale.ROOT, ">> Errors in element %s:%n", elementError.getKey());
            ((Exception)elementError.getValue()).printStackTrace(errorPrinter);
         });
      });
      errorPrinter.flush();
      LOGGER.error("Registry loading errors:\n{}", collectedErrors);
   }

   private static ReportedException createReportWithBriefInfo(final Map loadingErrors) {
      CrashReport report = CrashReport.forThrowable(new IllegalStateException("Failed to load registries due to errors"), "Registry Loading");
      CrashReportCategory errors = report.addCategory("Loading info");
      errors.setDetail("Errors", (CrashReportDetail)(() -> {
         StringBuilder briefDetails = new StringBuilder();
         loadingErrors.entrySet().stream().sorted(Entry.comparingByKey(ERROR_KEY_COMPARATOR)).forEach((e) -> briefDetails.append("\n\t\t").append(((ResourceKey)e.getKey()).registry()).append("/").append(((ResourceKey)e.getKey()).identifier()).append(": ").append(((Exception)e.getValue()).getMessage()));
         return briefDetails.toString();
      }));
      return new ReportedException(report);
   }

   private static void loadElementFromResource(final WritableRegistry output, final Decoder elementDecoder, final RegistryOps ops, final ResourceKey elementKey, final Resource thunk, final RegistrationInfo registrationInfo) throws IOException {
      Reader reader = thunk.openAsReader();

      try {
         JsonElement json = StrictJsonParser.parse(reader);
         DataResult<E> parseResult = elementDecoder.parse(ops, json);
         E parsedElement = (E)parseResult.getOrThrow();
         output.register(elementKey, parsedElement, registrationInfo);
      } catch (Throwable var11) {
         if (reader != null) {
            try {
               reader.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (reader != null) {
         reader.close();
      }

   }

   private static void loadContentsFromManager(final ResourceManager resourceManager, final RegistryOps.RegistryInfoLookup lookup, final WritableRegistry registry, final Decoder elementDecoder, final Map loadingErrors) {
      FileToIdConverter lister = FileToIdConverter.registry(registry.key());
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, (RegistryOps.RegistryInfoLookup)lookup);

      for(Map.Entry resourceEntry : lister.listMatchingResources(resourceManager).entrySet()) {
         Identifier resourceId = (Identifier)resourceEntry.getKey();
         ResourceKey<E> elementKey = ResourceKey.create(registry.key(), lister.fileToId(resourceId));
         Resource thunk = (Resource)resourceEntry.getValue();
         RegistrationInfo registrationInfo = (RegistrationInfo)REGISTRATION_INFO_CACHE.apply(thunk.knownPackInfo());

         try {
            loadElementFromResource(registry, elementDecoder, ops, elementKey, thunk, registrationInfo);
         } catch (Exception e) {
            loadingErrors.put(elementKey, new IllegalStateException(String.format(Locale.ROOT, "Failed to parse %s from pack %s", resourceId, thunk.sourcePackId()), e));
         }
      }

      TagLoader.loadTagsForRegistry(resourceManager, registry);
   }

   private static void loadContentsFromNetwork(final Map entries, final ResourceProvider knownDataSource, final RegistryOps.RegistryInfoLookup lookup, final WritableRegistry registry, final Decoder elementDecoder, final Map loadingErrors) {
      NetworkedRegistryData registryEntries = (NetworkedRegistryData)entries.get(registry.key());
      if (registryEntries != null) {
         RegistryOps<Tag> nbtOps = RegistryOps.create(NbtOps.INSTANCE, (RegistryOps.RegistryInfoLookup)lookup);
         RegistryOps<JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, (RegistryOps.RegistryInfoLookup)lookup);
         FileToIdConverter knownDataPathConverter = FileToIdConverter.registry(registry.key());

         for(RegistrySynchronization.PackedRegistryEntry entry : registryEntries.elements) {
            ResourceKey<E> elementKey = ResourceKey.create(registry.key(), entry.id());
            Optional<Tag> data = entry.data();
            if (data.isPresent()) {
               try {
                  DataResult<E> parseResult = elementDecoder.parse(nbtOps, (Tag)data.get());
                  E parsedElement = (E)parseResult.getOrThrow();
                  registry.register(elementKey, parsedElement, NETWORK_REGISTRATION_INFO);
               } catch (Exception e) {
                  loadingErrors.put(elementKey, new IllegalStateException(String.format(Locale.ROOT, "Failed to parse value %s from server", data.get()), e));
               }
            } else {
               Identifier knownDataPath = knownDataPathConverter.idToFile(entry.id());

               try {
                  Resource thunk = knownDataSource.getResourceOrThrow(knownDataPath);
                  loadElementFromResource(registry, elementDecoder, jsonOps, elementKey, thunk, NETWORK_REGISTRATION_INFO);
               } catch (Exception e) {
                  loadingErrors.put(elementKey, new IllegalStateException("Failed to parse local data", e));
               }
            }
         }

         TagLoader.loadTagsFromNetwork(registryEntries.tags, registry);
      }
   }

   static {
      WORLDGEN_REGISTRIES = List.of(new RegistryData(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC), new RegistryData(Registries.BIOME, Biome.DIRECT_CODEC), new RegistryData(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC), new RegistryData(Registries.CONFIGURED_CARVER, ConfiguredWorldCarver.DIRECT_CODEC), new RegistryData(Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC), new RegistryData(Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC), new RegistryData(Registries.STRUCTURE, Structure.DIRECT_CODEC), new RegistryData(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC), new RegistryData(Registries.PROCESSOR_LIST, StructureProcessorType.DIRECT_CODEC), new RegistryData(Registries.TEMPLATE_POOL, StructureTemplatePool.DIRECT_CODEC), new RegistryData(Registries.NOISE_SETTINGS, NoiseGeneratorSettings.DIRECT_CODEC), new RegistryData(Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC), new RegistryData(Registries.DENSITY_FUNCTION, DensityFunction.DIRECT_CODEC), new RegistryData(Registries.WORLD_PRESET, WorldPreset.DIRECT_CODEC), new RegistryData(Registries.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPreset.DIRECT_CODEC), new RegistryData(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC), new RegistryData(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC), new RegistryData(Registries.TRIAL_SPAWNER_CONFIG, TrialSpawnerConfig.DIRECT_CODEC), new RegistryData(Registries.WOLF_VARIANT, WolfVariant.DIRECT_CODEC, true), new RegistryData(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.DIRECT_CODEC, true), new RegistryData(Registries.PIG_VARIANT, PigVariant.DIRECT_CODEC, true), new RegistryData(Registries.FROG_VARIANT, FrogVariant.DIRECT_CODEC, true), new RegistryData(Registries.CAT_VARIANT, CatVariant.DIRECT_CODEC, true), new RegistryData(Registries.COW_VARIANT, CowVariant.DIRECT_CODEC, true), new RegistryData(Registries.CHICKEN_VARIANT, ChickenVariant.DIRECT_CODEC, true), new RegistryData(Registries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.DIRECT_CODEC, true), new RegistryData(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true), new RegistryData(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC), new RegistryData(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterList.DIRECT_CODEC), new RegistryData(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC), new RegistryData(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC), new RegistryData(Registries.ENCHANTMENT_PROVIDER, EnchantmentProvider.DIRECT_CODEC), new RegistryData(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC), new RegistryData(Registries.INSTRUMENT, Instrument.DIRECT_CODEC), new RegistryData(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC), new RegistryData(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC), new RegistryData(Registries.DIALOG, Dialog.DIRECT_CODEC), new RegistryData(Registries.TIMELINE, Timeline.DIRECT_CODEC));
      DIMENSION_REGISTRIES = List.of(new RegistryData(Registries.LEVEL_STEM, LevelStem.CODEC));
      SYNCHRONIZED_REGISTRIES = List.of(new RegistryData(Registries.BIOME, Biome.NETWORK_CODEC), new RegistryData(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC), new RegistryData(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC), new RegistryData(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC), new RegistryData(Registries.WOLF_VARIANT, WolfVariant.NETWORK_CODEC, true), new RegistryData(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.NETWORK_CODEC, true), new RegistryData(Registries.PIG_VARIANT, PigVariant.NETWORK_CODEC, true), new RegistryData(Registries.FROG_VARIANT, FrogVariant.NETWORK_CODEC, true), new RegistryData(Registries.CAT_VARIANT, CatVariant.NETWORK_CODEC, true), new RegistryData(Registries.COW_VARIANT, CowVariant.NETWORK_CODEC, true), new RegistryData(Registries.CHICKEN_VARIANT, ChickenVariant.NETWORK_CODEC, true), new RegistryData(Registries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.NETWORK_CODEC, true), new RegistryData(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true), new RegistryData(Registries.DIMENSION_TYPE, DimensionType.NETWORK_CODEC), new RegistryData(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC), new RegistryData(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC), new RegistryData(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC), new RegistryData(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC), new RegistryData(Registries.INSTRUMENT, Instrument.DIRECT_CODEC), new RegistryData(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC), new RegistryData(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC), new RegistryData(Registries.DIALOG, Dialog.DIRECT_CODEC), new RegistryData(Registries.TIMELINE, Timeline.NETWORK_CODEC));
   }

   public static record RegistryData(ResourceKey key, Codec elementCodec, boolean requiredNonEmpty) {
      private RegistryData(final ResourceKey key, final Codec elementCodec) {
         this(key, elementCodec, false);
      }

      private Loader create(final Lifecycle lifecycle, final Map loadingErrors) {
         WritableRegistry<T> result = new MappedRegistry(this.key, lifecycle);
         return new Loader(this, result, loadingErrors);
      }

      public void runWithArguments(final BiConsumer output) {
         output.accept(this.key, this.elementCodec);
      }
   }

   private static record Loader(RegistryData data, WritableRegistry registry, Map loadingErrors) {
      public void loadFromResources(final ResourceManager resourceManager, final RegistryOps.RegistryInfoLookup context) {
         RegistryDataLoader.loadContentsFromManager(resourceManager, context, this.registry, this.data.elementCodec, this.loadingErrors);
      }

      public void loadFromNetwork(final Map entries, final ResourceProvider knownDataSource, final RegistryOps.RegistryInfoLookup context) {
         RegistryDataLoader.loadContentsFromNetwork(entries, knownDataSource, context, this.registry, this.data.elementCodec, this.loadingErrors);
      }
   }

   public static record NetworkedRegistryData(List elements, TagNetworkSerialization.NetworkPayload tags) {
   }

   @FunctionalInterface
   private interface LoadingFunction {
      void apply(Loader loader, RegistryOps.RegistryInfoLookup context);
   }
}
