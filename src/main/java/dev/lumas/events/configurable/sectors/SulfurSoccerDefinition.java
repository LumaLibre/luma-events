package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class SulfurSoccerDefinition extends OkaeriConfig {

    private int punchRadius = 9;
    private Location spawnLocation;
    private Location team1StartLocation;
    private Location team2StartLocation;
    private Location soccerBallStartLocation;

    private Region bounds = new Region();
    private Region team1Goal = new Region();
    private Region team2Goal = new Region();
}
