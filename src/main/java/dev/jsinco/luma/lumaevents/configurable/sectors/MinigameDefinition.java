package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;

@ToString
@Getter
@Setter
public class MinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Region region = new Region();

}
