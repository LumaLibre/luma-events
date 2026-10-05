package dev.lumas.events.configurable.serializers;

import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class VectorSerializer extends ScalarSerializer<Vector> {

    private static final Pattern VECTOR_RE = Pattern.compile("([^,]+),([^,]+),([^,]+)");

    public VectorSerializer() {
        super(Vector.class);
    }

    @Override
    public Vector deserialize(Type type, Object obj) throws SerializationException {
        Matcher matcher = VECTOR_RE.matcher(obj.toString());
        if (!matcher.matches()) {
            throw new SerializationException("Invalid vector format, expected 'x,y,z' where n is any number");
        }
        try {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3));
            return new Vector(x, y, z);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid vector format, expected 'x,y,z' where n is any number");
        }
    }

    @Override
    protected Object serialize(Vector item, Predicate<Class<?>> typeSupported) {
        return "%f,%f,%f".formatted(item.getX(), item.getY(), item.getZ());
    }
}
