package dev.lumas.events.configurable.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
public class BoundingBoxSerializer implements ObjectSerializer<BoundingBox> {
    private static final Pattern BOUNDING_BOX_RE = Pattern.compile("([^,]+),([^,]+),([^,]+);([^,]+),([^,]+),([^,]+)");

    @Override
    public boolean supports(Class<? super BoundingBox> type) {
        return BoundingBox.class == type;
    }

    @Override
    public void serialize(BoundingBox object, SerializationData data, GenericsDeclaration generics) {
        Vector minPoint = object.getMin();
        Vector maxPoint = object.getMax();
        data.setValue("%f,%f,%f;%f,%f,%f".formatted(
                minPoint.getX(),
                minPoint.getY(),
                minPoint.getZ(),
                maxPoint.getX(),
                maxPoint.getY(),
                maxPoint.getZ()
        ));
    }

    @Override
    public BoundingBox deserialize(DeserializationData data, GenericsDeclaration generics) {
        String string = data.getValue(String.class);
        if (string == null) {
            throw new IllegalArgumentException("Expected a string for bounding box format");
        }
        Matcher matcher = BOUNDING_BOX_RE.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid bounding box format expected 'x,y,z;x,y,z'");
        }
        double x1 = Double.parseDouble(matcher.group(1));
        double y1 = Double.parseDouble(matcher.group(2));
        double z1 = Double.parseDouble(matcher.group(3));
        double x2 = Double.parseDouble(matcher.group(4));
        double y2 = Double.parseDouble(matcher.group(5));
        double z2 = Double.parseDouble(matcher.group(6));
        return new BoundingBox(x1, y1, z1, x2, y2, z2);
    }
}
