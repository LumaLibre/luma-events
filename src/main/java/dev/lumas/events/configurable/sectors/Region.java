package dev.lumas.events.configurable.sectors;

import dev.lumas.events.model.WorldTiedBoundingBox;
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

    public WorldTiedBoundingBox toBlockBoundingBox() {
        return WorldTiedBoundingBox.ofBlocks(loc1, loc2);
    }
}
