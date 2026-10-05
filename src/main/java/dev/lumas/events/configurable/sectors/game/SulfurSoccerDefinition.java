package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class SulfurSoccerDefinition {

    private int punchRadius = 9;
    private Location spawnLocation;
    @MapTied
    private Location team1StartLocation;
    @MapTied
    private Location team2StartLocation;
    @MapTied
    private Location soccerBallStartLocation;

    @MapTied
    private Region bounds = new Region();
    @MapTied
    private Region team1Goal = new Region();
    @MapTied
    private Region team2Goal = new Region();
}
