package dev.lumas.events.configurable.serializers;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class BoundingBoxSerializer extends ScalarSerializer<BoundingBox> {
    private static final Pattern BOUNDING_BOX_REGEX = Pattern.compile("([^,]+),([^,]+),([^,]+);([^,]+),([^,]+),([^,]+)");

    public BoundingBoxSerializer() {
        super(BoundingBox.class);
    }

    @Override
    public BoundingBox deserialize(Type type, Object obj) throws SerializationException {
        Matcher matcher = BOUNDING_BOX_REGEX.matcher(obj.toString());
        if (!matcher.matches()) {
            throw new SerializationException("Invalid bounding box format expected 'x,y,z;x,y,z'");
        }
        try {
            double x1 = Double.parseDouble(matcher.group(1));
            double y1 = Double.parseDouble(matcher.group(2));
            double z1 = Double.parseDouble(matcher.group(3));
            double x2 = Double.parseDouble(matcher.group(4));
            double y2 = Double.parseDouble(matcher.group(5));
            double z2 = Double.parseDouble(matcher.group(6));
            return new BoundingBox(x1, y1, z1, x2, y2, z2);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid bounding box format expected 'x,y,z;x,y,z'");
        }
    }

    @Override
    protected Object serialize(BoundingBox item, Predicate<Class<?>> typeSupported) {
        Vector minPoint = item.getMin();
        Vector maxPoint = item.getMax();
        return "%f,%f,%f;%f,%f,%f".formatted(
                minPoint.getX(),
                minPoint.getY(),
                minPoint.getZ(),
                maxPoint.getX(),
                maxPoint.getY(),
                maxPoint.getZ()
        );
    }
}
