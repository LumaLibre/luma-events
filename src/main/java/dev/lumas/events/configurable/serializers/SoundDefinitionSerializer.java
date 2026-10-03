package dev.lumas.events.configurable.serializers;

import dev.lumas.events.utility.effects.SoundDefinition;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SoundDefinitionSerializer implements ObjectSerializer<SoundDefinition> {
    @Override
    public boolean supports(Class<? super SoundDefinition> type) {
        return SoundDefinition.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(SoundDefinition object, SerializationData data, GenericsDeclaration generics) {
        Key key = object.sound().key();
        if (object.seed() != null) {
            data.setValue("%s;%f;%f;%d"
                    .formatted(key.asMinimalString(), object.volume(), object.pitch(), object.seed())
            );
            return;
        }
        data.setValue("%s;%f;%f"
                .formatted(key.asMinimalString(), object.volume(), object.pitch())
        );
    }

    @Override
    public SoundDefinition deserialize(DeserializationData data, GenericsDeclaration generics) {
        String string = data.getValue(String.class);
        if (string == null) {
            throw new IllegalArgumentException("Expected a present value for sound definition");
        }
        String[] split = string.split(";");
        NamespacedKey soundKey = NamespacedKey.fromString(split[0]);
        if (soundKey == null || !(Registry.SOUNDS.get(soundKey) instanceof Sound sound)) {
            throw new IllegalArgumentException("Invalid namespaced key for sound definition");
        }
        float volume = split.length > 1 ? Float.parseFloat(split[1]) : 15f;
        float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;
        Long seed = split.length > 3 ? Long.parseLong(split[3]) : null;
        return new SoundDefinition(sound, volume, pitch, seed);
    }
}
