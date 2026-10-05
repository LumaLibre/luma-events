package dev.lumas.events.configurable.serializers;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class KeyedSerializer<T extends Keyed> extends ScalarSerializer<T> {

    private final Registry<T> backing;

    public KeyedSerializer(Class<T> type, Registry<T> backing) {
        super(type);
        this.backing = backing;
    }

    @Override
    public T deserialize(Type type, Object obj) throws SerializationException {
        NamespacedKey key = NamespacedKey.fromString(obj.toString());
        if (key == null) {
            throw new SerializationException("Expected a namespaced key");
        }
        T t = backing.get(key);
        if (t == null) {
            throw new SerializationException("Could not find keyed registry value '%s'"
                    .formatted(key.asMinimalString())
            );
        }
        return t;
    }

    @Override
    protected Object serialize(T item, Predicate<Class<?>> typeSupported) {
        return item.getKey().asString();
    }
}
