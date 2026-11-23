package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;

   protected AbstractPackResources(final PackLocationInfo location) {
      this.location = location;
   }

   public @Nullable Object getMetadataSection(final MetadataSectionType metadataSerializer) throws IOException {
      IoSupplier<InputStream> metadata = this.getRootResource(new String[]{"pack.mcmeta"});
      if (metadata == null) {
         return null;
      } else {
         InputStream resource = (InputStream)metadata.get();

         Object var4;
         try {
            var4 = getMetadataFromStream(metadataSerializer, resource, this.location);
         } catch (Throwable var7) {
            if (resource != null) {
               try {
                  resource.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (resource != null) {
            resource.close();
         }

         return var4;
      }
   }

   public static @Nullable Object getMetadataFromStream(final MetadataSectionType serializer, final InputStream stream, final PackLocationInfo location) {
      JsonObject metadata;
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

         try {
            metadata = GsonHelper.parse((Reader)reader);
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
      } catch (Exception e) {
         LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{location.id(), serializer.name(), e.getMessage()});
         return null;
      }

      return !metadata.has(serializer.name()) ? null : serializer.codec().parse(JsonOps.INSTANCE, metadata.get(serializer.name())).ifError((error) -> LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{location.id(), serializer.name(), error.message()})).result().orElse((Object)null);
   }

   public PackLocationInfo location() {
      return this.location;
   }
}
