package dev.lumas.events.configurable.serializers;

import dev.lumas.events.model.WorldTiedBoundingBox;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class WorldTiedBoundingBoxSerializer extends ScalarSerializer<WorldTiedBoundingBox> {

    public WorldTiedBoundingBoxSerializer() {
        super(WorldTiedBoundingBox.class);
    }

    @Override
    public WorldTiedBoundingBox deserialize(Type type, Object obj) throws SerializationException {
        String data = obj.toString();
        String[] corners = data.split("/");
        if (corners.length != 2) {
            throw new SerializationException("Invalid box format (expected two corners separated by a /): " + data);
        }
        return WorldTiedBoundingBox.ofBlocks(
                LocationSerializer.parse(corners[0].trim()),
                LocationSerializer.parse(corners[1].trim())
        );
    }

    @Override
    protected Object serialize(WorldTiedBoundingBox item, Predicate<Class<?>> typeSupported) {
        World world = item.getWorld();
        String name = world != null ? world.getName() : "world";
        return name + "," + (int) item.getMinX() + "," + (int) item.getMinY() + "," + (int) item.getMinZ()
                + "/" + name + "," + ((int) item.getMaxX() - 1) + "," + ((int) item.getMaxY() - 1) + "," + ((int) item.getMaxZ() - 1);
    }
}
