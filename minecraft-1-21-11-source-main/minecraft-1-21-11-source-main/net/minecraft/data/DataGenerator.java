package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public class DataGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path rootOutputFolder;
   private final PackOutput vanillaPackOutput;
   private final Set allProviderIds = new HashSet();
   private final Map providersToRun = new LinkedHashMap();
   private final WorldVersion version;
   private final boolean alwaysGenerate;

   public DataGenerator(final Path output, final WorldVersion version, final boolean alwaysGenerate) {
      this.rootOutputFolder = output;
      this.vanillaPackOutput = new PackOutput(this.rootOutputFolder);
      this.version = version;
      this.alwaysGenerate = alwaysGenerate;
   }

   public void run() throws IOException {
      HashCache cache = new HashCache(this.rootOutputFolder, this.allProviderIds, this.version);
      Stopwatch totalTime = Stopwatch.createStarted();
      Stopwatch stopwatch = Stopwatch.createUnstarted();
      this.providersToRun.forEach((providerId, provider) -> {
         if (!this.alwaysGenerate && !cache.shouldRunInThisVersion(providerId)) {
            LOGGER.debug("Generator {} already run for version {}", providerId, this.version.name());
         } else {
            LOGGER.info("Starting provider: {}", providerId);
            stopwatch.start();
            Objects.requireNonNull(provider);
            cache.applyUpdate((HashCache.UpdateResult)cache.generateUpdate(providerId, provider::run).join());
            stopwatch.stop();
            LOGGER.info("{} finished after {} ms", providerId, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();
         }
      });
      LOGGER.info("All providers took: {} ms", totalTime.elapsed(TimeUnit.MILLISECONDS));
      cache.purgeStaleAndWrite();
   }

   public PackGenerator getVanillaPack(final boolean toRun) {
      return new PackGenerator(toRun, "vanilla", this.vanillaPackOutput);
   }

   public PackGenerator getBuiltinDatapack(final boolean toRun, final String packId) {
      Path packOutputDir = this.vanillaPackOutput.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft").resolve("datapacks").resolve(packId);
      return new PackGenerator(toRun, packId, new PackOutput(packOutputDir));
   }

   static {
      Bootstrap.bootStrap();
   }

   public class PackGenerator {
      private final boolean toRun;
      private final String providerPrefix;
      private final PackOutput output;

      private PackGenerator(final boolean toRun, final String providerPrefix, final PackOutput output) {
         this.toRun = toRun;
         this.providerPrefix = providerPrefix;
         this.output = output;
      }

      public DataProvider addProvider(final DataProvider.Factory factory) {
         T provider = (T)factory.create(this.output);
         String var10000 = this.providerPrefix;
         String providerId = var10000 + "/" + provider.getName();
         if (!DataGenerator.this.allProviderIds.add(providerId)) {
            throw new IllegalStateException("Duplicate provider: " + providerId);
         } else {
            if (this.toRun) {
               DataGenerator.this.providersToRun.put(providerId, provider);
            }

            return provider;
         }
      }
   }
}
