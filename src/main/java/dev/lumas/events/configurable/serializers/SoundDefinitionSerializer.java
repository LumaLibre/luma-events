package dev.lumas.events.configurable.serializers;

import dev.lumas.events.utility.effects.SoundDefinition;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class SoundDefinitionSerializer extends ScalarSerializer<SoundDefinition> {

    public SoundDefinitionSerializer() {
        super(SoundDefinition.class);
    }

    @Override
    public SoundDefinition deserialize(Type type, Object obj) throws SerializationException {
        String[] split = obj.toString().split(";");
        NamespacedKey soundKey = NamespacedKey.fromString(split[0]);
        if (soundKey == null || !(Registry.SOUNDS.get(soundKey) instanceof Sound sound)) {
            throw new SerializationException("Invalid namespaced key for sound definition");
        }
        try {
            float volume = split.length > 1 ? Float.parseFloat(split[1]) : 15f;
            float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;
            Long seed = split.length > 3 ? Long.parseLong(split[3]) : null;
            return new SoundDefinition(sound, volume, pitch, seed);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid sound definition: " + obj);
        }
    }

    @Override
    protected Object serialize(SoundDefinition item, Predicate<Class<?>> typeSupported) {
        Key key = item.sound().key();
        if (item.seed() != null) {
            return "%s;%f;%f;%d".formatted(key.asMinimalString(), item.volume(), item.pitch(), item.seed());
        }
        return "%s;%f;%f".formatted(key.asMinimalString(), item.volume(), item.pitch());
    }
}
