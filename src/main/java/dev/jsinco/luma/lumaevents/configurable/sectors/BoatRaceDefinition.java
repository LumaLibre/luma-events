package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Location;

import java.util.List;

@ToString
@Getter
public class BoatRaceDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Location startLocation;
    private Region region = new Region();
    private List<Region> checkpoints = List.of(new Region(), new Region());

}
