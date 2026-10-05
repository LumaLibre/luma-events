package dev.lumas.events.configurable.sectors.region;

import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class CylinderRegion {

    public Location center;
    public int radius;
    public int height;

}
