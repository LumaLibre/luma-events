package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlockDataSerializer implements ObjectSerializer<BlockData> {
    @Override
    public boolean supports(Class<? super BlockData> type) {
        return BlockData.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(BlockData object, SerializationData data, GenericsDeclaration generics) {
        data.setValue(object.getAsString(true));
    }

    @Override
    public BlockData deserialize(DeserializationData data, GenericsDeclaration generics) {
        String blockDataString = data.getValue(String.class);
        if (blockDataString == null) {
            throw new IllegalArgumentException("Expected string for block data definition");
        }
        return Bukkit.createBlockData(blockDataString);
    }
}
