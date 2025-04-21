package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.List;

@Getter
@Setter
public class TheNabbitsMinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Region region = new Region();
    private List<Region> playAreas = List.of(new Region(), new Region());
}
