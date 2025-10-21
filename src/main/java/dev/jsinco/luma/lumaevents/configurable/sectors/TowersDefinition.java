package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class TowersDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Region region = new Region();
    private Region outerRegion = new Region();
    private TowersItems towersItems = new TowersItems();
}
