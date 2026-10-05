package dev.lumas.events.configurable.serializers;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class NamespacedKeySerializer extends ScalarSerializer<NamespacedKey> {

    public NamespacedKeySerializer() {
        super(NamespacedKey.class);
    }

    @Override
    public NamespacedKey deserialize(Type type, Object obj) throws SerializationException {
        NamespacedKey key = NamespacedKey.fromString(obj.toString());
        if (key == null) {
            throw new SerializationException("Invalid namespaced key: " + obj);
        }
        return key;
    }

    @Override
    protected Object serialize(NamespacedKey item, Predicate<Class<?>> typeSupported) {
        return item.asMinimalString();
    }
}
