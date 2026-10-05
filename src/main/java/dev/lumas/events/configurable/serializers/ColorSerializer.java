package dev.lumas.events.configurable.serializers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.bukkit.Color;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Predicate;

@NullMarked
public class ColorSerializer extends ScalarSerializer<Color> {

    private static final BiMap<String, Integer> PREDEFINED_COLORS = ImmutableBiMap.<String, Integer>builder()
            .put("black", 0x00000)
            .put("dark_blue", 0x0000AA)
            .put("dark_green", 0x00AA00)
            .put("dark_aqua", 0x00AAAA)
            .put("dark_red", 0xAA0000)
            .put("dark_purple", 0xAA00AA)
            .put("gold", 0xFFAA00)
            .put("gray", 0xAAAAAA)
            .put("dark_gray", 0x555555)
            .put("blue", 0x5555FF)
            .put("green", 0x55FF55)
            .put("aqua", 0x55FFFF)
            .put("red", 0xFF5555)
            .put("light_purple", 0xFF55FF)
            .put("yellow", 0xFFFF55)
            .put("white", 0xFFFFFF)
            .build();

    public ColorSerializer() {
        super(Color.class);
    }

    @Override
    public Color deserialize(Type type, Object obj) throws SerializationException {
        String string = obj.toString();
        Integer rgb = PREDEFINED_COLORS.get(string.toLowerCase(Locale.ROOT));
        if (rgb != null) {
            return Color.fromRGB(rgb);
        }
        try {
            return Color.fromRGB(Integer.parseInt(string, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid color definition: " + string);
        }
    }

    @Override
    protected Object serialize(Color item, Predicate<Class<?>> typeSupported) {
        int rgb = item.asRGB();
        if (PREDEFINED_COLORS.inverse().get(rgb) instanceof String string) {
            return string;
        }
        return Integer.toString(rgb, 16);
    }
}
