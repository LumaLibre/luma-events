package dev.lumas.events.configurable.serializers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Color;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

@NullMarked
public class ColorSerializer implements ObjectSerializer<Color> {

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


    @Override
    public boolean supports(Class<? super Color> type) {
        return Color.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(Color object, SerializationData data, GenericsDeclaration generics) {
        int rgb = object.asRGB();
        if (PREDEFINED_COLORS.inverse().get(rgb) instanceof String string) {
            data.setValue(string);
            return;
        }
        data.setValue(Integer.toString(rgb, 16));
    }

    @Override
    public Color deserialize(DeserializationData data, GenericsDeclaration generics) {
        String string = data.getValue(String.class);
        if (string == null) {
            throw new IllegalArgumentException("Expected a present value for color definition");
        }
        Integer rgb = PREDEFINED_COLORS.get(string.toLowerCase(Locale.ROOT));
        if (rgb != null) {
            return Color.fromRGB(rgb);
        }
        return Color.fromRGB(Integer.parseInt(string, 16) & 0xFFFFFF);
    }
}
