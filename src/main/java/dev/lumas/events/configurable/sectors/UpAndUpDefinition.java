package dev.lumas.events.configurable.sectors;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@Getter
@ConfigSerializable
public class UpAndUpDefinition {

    private Location spawnLocation;
    @MapTied
    private Region region = new Region();
    @MapTied
    private List<MicrogameMap> microgameMaps = List.of(
            new MicrogameMap("idk_whatever.schem", 50)
    );

}
