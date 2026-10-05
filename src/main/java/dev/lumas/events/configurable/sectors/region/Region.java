package dev.lumas.events.configurable.sectors.region;

import dev.lumas.events.model.WorldTiedBoundingBox;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class Region {
    private Location loc1;
    private Location loc2;

    public WorldTiedBoundingBox toWorldTiedBoundingBox() {
        return WorldTiedBoundingBox.of(loc1, loc2);
    }

    public WorldTiedBoundingBox toBlockBoundingBox() {
        return WorldTiedBoundingBox.ofBlocks(loc1, loc2);
    }
}
