package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
   ResourceMetadata EMPTY = new ResourceMetadata() {
      public Optional getSection(final MetadataSectionType serializer) {
         return Optional.empty();
      }
   };
   IoSupplier EMPTY_SUPPLIER = () -> EMPTY;

   static ResourceMetadata fromJsonStream(final InputStream inputStream) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

      ResourceMetadata var3;
      try {
         final JsonObject metadata = GsonHelper.parse((Reader)reader);
         var3 = new ResourceMetadata() {
            public Optional getSection(final MetadataSectionType serializer) {
               String name = serializer.name();
               if (metadata.has(name)) {
                  T section = (T)serializer.codec().parse(JsonOps.INSTANCE, metadata.get(name)).getOrThrow(JsonParseException::new);
                  return Optional.of(section);
               } else {
                  return Optional.empty();
               }
            }
         };
      } catch (Throwable var5) {
         try {
            reader.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      reader.close();
      return var3;
   }

   Optional getSection(MetadataSectionType serializer);

   default Optional getTypedSection(final MetadataSectionType type) {
      Optional var10000 = this.getSection(type);
      Objects.requireNonNull(type);
      return var10000.map(type::withValue);
   }

   default List getTypedSections(final Collection types) {
      return (List)types.stream().map(this::getTypedSection).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
   }
}
