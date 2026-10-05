package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@ToString
@Getter
@ConfigSerializable
public class BoatRace2Definition {

    private Location spawnLocation;
    @MapTied
    private Location spectateLocation;
    private int maxLaps = 3;
    @MapTied
    private Region region = new Region();
    @MapTied
    private Region finishLine = new Region();
    @MapTied
    private List<Region> checkpoints = List.of(new Region(), new Region());
    @MapTied
    private Location overFlowPoint;
    @MapTied
    private List<Location> spawnPoints = List.of();

}