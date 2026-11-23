package net.minecraft.core;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface WritableRegistry extends Registry {
   Holder.Reference register(ResourceKey key, Object value, RegistrationInfo registrationInfo);

   void bindTag(TagKey key, List values);

   boolean isEmpty();

   HolderGetter createRegistrationLookup();
}
