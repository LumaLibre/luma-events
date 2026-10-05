package dev.lumas.events.configurable.sectors;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ToString
@Getter
@Setter
@ConfigSerializable
public class MinigameDefinition {

    @MapTied
    private Location spawnLocation;
    @MapTied
    private Region region = new Region();

}
