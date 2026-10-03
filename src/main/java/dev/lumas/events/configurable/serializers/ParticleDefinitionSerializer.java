package dev.lumas.events.configurable.serializers;

import dev.lumas.events.utility.effects.ParticleDefinition;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ParticleDefinitionSerializer implements ObjectSerializer<ParticleDefinition> {
    @Override
    public boolean supports(Class<? super ParticleDefinition> type) {
        return ParticleDefinition.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(ParticleDefinition object, SerializationData data, GenericsDeclaration generics) {
        data.add("particle", object.particleKey());
        if (object.spawnProbability() != 100d) {
            data.add("probability", object.spawnProbability());
        }
        if (object.spawnArea() != null) {
            data.add("spawning-area", object.spawnArea());
        }
        if (object.count() != 1) {
            data.add("count", object.count());
        }
        switch (object.options()) {
            case Particle.DustOptions dustOptions -> data.add("dust-options", dustOptions);
            case BlockData blockData -> data.add("block-data", blockData);
            case null, default -> {
            }
        }

    }

    @Override
    public ParticleDefinition deserialize(DeserializationData data, GenericsDeclaration generics) {
        Particle particle = data.get("particle", Particle.class);
        if (particle == null) {
            throw new IllegalArgumentException("Expected particle type for particle definition");
        }
        Double probability = data.get("probability", Double.class);
        BoundingBox spawningArea = data.get("spawning-area", BoundingBox.class);
        Integer count = data.get("count", Integer.class);
        Object options;
        if (data.containsKey("dust-options")) {
            options = data.get("dust-options", Particle.DustOptions.class);
        } else if (data.containsKey("block-data")) {
            options = data.get("block-data", BlockData.class);
        } else {
            options = null;
        }
        return new ParticleDefinition(
                particle,
                probability == null ? 100d : probability,
                count == null ? 1 : count,
                spawningArea,
                options
        );
    }
}
