package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DustOptionsSerializer implements ObjectSerializer<Particle.DustOptions> {
    @Override
    public boolean supports(Class<? super Particle.DustOptions> type) {
        return Particle.DustOptions.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(Particle.DustOptions object, SerializationData data, GenericsDeclaration generics) {
        data.add("color", object.getColor());
        data.add("size", object.getSize());
    }

    @Override
    public Particle.DustOptions deserialize(DeserializationData data, GenericsDeclaration generics) {
        Color color = data.get("color", Color.class);
        if (color == null) {
            throw new IllegalArgumentException("Expected color to be specified for dust options definition");
        }
        Float size = data.get("size", Float.class);
        return new Particle.DustOptions(color, size == null ? 1f : size);
    }
}
