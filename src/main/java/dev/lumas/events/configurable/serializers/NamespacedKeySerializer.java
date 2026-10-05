package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NamespacedKeySerializer implements ObjectSerializer<NamespacedKey> {
    @Override
    public boolean supports(Class<? super NamespacedKey> type) {
        return NamespacedKey.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(NamespacedKey object, SerializationData data, GenericsDeclaration generics) {
        data.setValue(object.asMinimalString());
    }

    @Override
    public NamespacedKey deserialize(DeserializationData data, GenericsDeclaration generics) {
        if (data.getValue(String.class) instanceof String string) {
            return NamespacedKey.fromString(string);
        }
        throw new IllegalArgumentException("Undefined element for namespaced key");
    }
}
