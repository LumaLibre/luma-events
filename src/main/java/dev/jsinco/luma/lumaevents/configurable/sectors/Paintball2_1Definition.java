package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class Paintball2_1Definition extends OkaeriConfig {

    private Location spawnLocation;
    private Location team1SpawnLocation;
    private Location team2SpawnLocation;
    private Region region = new Region();
}
