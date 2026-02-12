package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Location;

import java.util.List;

@ToString
@Getter
public class BoatRace2Definition extends OkaeriConfig {

    private Location spawnLocation;
    private Location spectateLocation;
    private int maxLaps = 3;
    private Region region = new Region();
    private Region finishLine = new Region();
    private List<Region> checkpoints = List.of(new Region(), new Region());
    private Location overFlowPoint;
    private List<Location> spawnPoints = List.of();

}