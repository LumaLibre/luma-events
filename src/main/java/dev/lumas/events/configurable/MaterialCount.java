package dev.lumas.events.configurable;

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
}
