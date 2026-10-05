package dev.lumas.events.configurable.serializers;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@NullMarked
public class DustOptionsSerializer implements TypeSerializer<Particle.DustOptions> {

    @Override
    public Particle.DustOptions deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Color color = node.node("color").get(Color.class);
        if (color == null) {
            throw new SerializationException("Expected color to be specified for dust options definition");
        }
        return new Particle.DustOptions(color, node.node("size").getFloat(1f));
    }

    @Override
    public void serialize(Type type, Particle.@Nullable DustOptions obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }
        node.node("color").set(Color.class, obj.getColor());
        node.node("size").set(obj.getSize());
    }
}
