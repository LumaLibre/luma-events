package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class VectorSerializer implements ObjectSerializer<Vector> {

    private static final Pattern VECTOR_RE = Pattern.compile("([^,]+),([^,]+),([^,]+)");

    @Override
    public boolean supports(Class<? super Vector> type) {
        return Vector.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(Vector object, SerializationData data, GenericsDeclaration generics) {
        data.setValue("%f,%f,%f".formatted(
                object.getX(), object.getY(), object.getZ()
        ));
    }

    @Override
    public Vector deserialize(DeserializationData data, GenericsDeclaration generics) {
        String string = data.getValue(String.class);
        if (string == null) {
            throw new IllegalArgumentException("Expected a string for vector format");
        }
        Matcher matcher = VECTOR_RE.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid vector format, expected 'z,y,z' where n is any number");
        }
        double x = Double.parseDouble(matcher.group(1));
        double y = Double.parseDouble(matcher.group(2));
        double z = Double.parseDouble(matcher.group(3));
        return new Vector(x, y, z);
    }
}
