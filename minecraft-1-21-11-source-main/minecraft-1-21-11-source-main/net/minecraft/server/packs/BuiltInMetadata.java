package net.minecraft.server.packs;

import java.util.Map;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public class BuiltInMetadata {
   private static final BuiltInMetadata EMPTY = new BuiltInMetadata(Map.of());
   private final Map values;

   private BuiltInMetadata(final Map values) {
      this.values = values;
   }

   public Object get(final MetadataSectionType section) {
      return this.values.get(section);
   }

   public static BuiltInMetadata of() {
      return EMPTY;
   }

   public static BuiltInMetadata of(final MetadataSectionType k, final Object v) {
      return new BuiltInMetadata(Map.of(k, v));
   }

   public static BuiltInMetadata of(final MetadataSectionType k1, final Object v1, final MetadataSectionType k2, final Object v2) {
      return new BuiltInMetadata(Map.of(k1, v1, k2, v2));
   }
}
