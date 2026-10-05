package dev.lumas.events.configurable.serializers;

import dev.lumas.events.utility.effects.ParticleDefinition;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@NullMarked
public class ParticleDefinitionSerializer implements TypeSerializer<ParticleDefinition> {

    @Override
    public ParticleDefinition deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Particle particle = node.node("particle").get(Particle.class);
        if (particle == null) {
            throw new SerializationException("Expected particle type for particle definition");
        }
        Object options;
        if (!node.node("dust-options").virtual()) {
            options = node.node("dust-options").get(Particle.DustOptions.class);
        } else if (!node.node("block-data").virtual()) {
            options = node.node("block-data").get(BlockData.class);
        } else {
            options = null;
        }
        return new ParticleDefinition(
                particle,
                node.node("probability").getDouble(100d),
                node.node("count").getInt(1),
                node.node("spawning-area").get(BoundingBox.class),
                options
        );
    }

    @Override
    public void serialize(Type type, @Nullable ParticleDefinition obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }
        node.node("particle").set(Particle.class, obj.particleKey());
        if (obj.spawnProbability() != 100d) {
            node.node("probability").set(obj.spawnProbability());
        }
        if (obj.spawnArea() != null) {
            node.node("spawning-area").set(BoundingBox.class, obj.spawnArea());
        }
        if (obj.count() != 1) {
            node.node("count").set(obj.count());
        }
        switch (obj.options()) {
            case Particle.DustOptions dustOptions -> node.node("dust-options").set(Particle.DustOptions.class, dustOptions);
            case BlockData blockData -> node.node("block-data").set(BlockData.class, blockData);
            case null, default -> {
            }
        }
    }
}
