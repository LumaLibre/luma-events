package dev.lumas.events.configurable.sectors;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.CylinderRegion;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class CylindricalMinigameDefinition {

    private Location spawnLocation;
    @MapTied
    private CylinderRegion region = new CylinderRegion();
}
