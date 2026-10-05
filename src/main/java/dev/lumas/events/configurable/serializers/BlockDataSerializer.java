package dev.lumas.events.configurable.serializers;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class BlockDataSerializer extends ScalarSerializer<BlockData> {

    public BlockDataSerializer() {
        super(BlockData.class);
    }

    @Override
    public BlockData deserialize(Type type, Object obj) throws SerializationException {
        try {
            return Bukkit.createBlockData(obj.toString());
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid block data definition: " + obj);
        }
    }

    @Override
    protected Object serialize(BlockData item, Predicate<Class<?>> typeSupported) {
        return item.getAsString(true);
    }
}
