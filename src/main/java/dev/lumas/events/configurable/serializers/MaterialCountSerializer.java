package dev.lumas.events.configurable.serializers;

import dev.lumas.events.configurable.MaterialCount;
import dev.lumas.events.utility.Util;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

@NullMarked
public class MaterialCountSerializer extends ScalarSerializer<MaterialCount> {

    public MaterialCountSerializer() {
        super(MaterialCount.class);
    }

    @Override
    public MaterialCount deserialize(Type type, Object obj) throws SerializationException {
        String data = obj.toString();
        String[] parts = data.split(";");
        if (parts.length != 2) {
            throw new SerializationException("Invalid MaterialCount format: " + data);
        }
        Material material = Material.getMaterial(parts[0]);
        if (material == null) {
            throw new SerializationException("Invalid material: " + parts[0]);
        }
        return new MaterialCount(material, Util.getInt(parts[1], 1));
    }

    @Override
    protected Object serialize(MaterialCount item, Predicate<Class<?>> typeSupported) {
        return item.getMaterial().name() + ";" + item.getCount();
    }
}
