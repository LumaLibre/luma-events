package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record KeyedSerializer<T extends Keyed>(Class<T> tClass,
                                               Registry<T> backing) implements ObjectSerializer<T> {
    @Override
    public boolean supports(Class<? super T> type) {
        return tClass.isAssignableFrom(type);
    }

    @Override
    public void serialize(T object, SerializationData data, GenericsDeclaration generics) {
        data.setValue(object.getKey());
    }

    @Override
    public T deserialize(DeserializationData data, GenericsDeclaration generics) {
        NamespacedKey key = data.getValue(NamespacedKey.class);
        if (key == null) {
            throw new IllegalArgumentException("Expected a namespaced key");
        }
        T t = backing.get(key);
        if (t == null) {
            throw new IllegalArgumentException("Could not find keyed registry value '%s'"
                    .formatted(key.asMinimalString())
            );
        }
        return t;
    }
}
