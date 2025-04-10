package dev.jsinco.luma.lumaevents.configurable.sectors;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class TheNabbitsMinigameDefinition {

    private Location spawnLocation;
    private Region region = new Region();
    private Region playArea = new Region();
}
