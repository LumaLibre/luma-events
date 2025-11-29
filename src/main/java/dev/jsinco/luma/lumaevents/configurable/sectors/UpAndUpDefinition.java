package dev.jsinco.luma.lumaevents.configurable.sectors;

import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class UpAndUpDefinition {

    private Location spawnLocation;
    private Region region = new Region();
    private List<MicrogameMap> microgameMaps = List.of(
            new MicrogameMap("idk_whatever.schem", 50)
    );

}
