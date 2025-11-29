package dev.jsinco.luma.lumaevents.configurable.sectors;

import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class Region extends OkaeriConfig {
    private Location loc1;
    private Location loc2;

    public WorldTiedBoundingBox toWorldTiedBoundingBox() {
        return WorldTiedBoundingBox.of(loc1, loc2);
    }
}
