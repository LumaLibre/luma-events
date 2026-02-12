package dev.lumas.events.configurable;

import dev.lumas.events.utility.Util;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class MaterialCount {
    private Material material;
    private int count;

    public static MaterialCount of(@NonNull Material material) {
        return new MaterialCount(material, 1);
    }

    public static MaterialCount of(@NonNull Material material, int count) {
        return new MaterialCount(material, count);
    }

    public static class Transformer extends BidirectionalTransformer<String, MaterialCount> {


        @Override
        public GenericsPair<String, MaterialCount> getPair() {
            return this.genericsPair(String.class, MaterialCount.class);
        }

        @Override
        public MaterialCount leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
            String[] parts = data.split(";");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid MaterialCount format: " + data);
            }
            Material material = Material.getMaterial(parts[0]);
            if (material == null) {
                throw new IllegalArgumentException("Invalid material: " + parts[0]);
            }
            int count = Util.getInt(parts[1], 1);
            return new MaterialCount(material, count);
        }

        @Override
        public String rightToLeft(@NonNull MaterialCount data, @NonNull SerdesContext serdesContext) {
            return data.getMaterial().name() + ";" + data.getCount();
        }
    }
}
