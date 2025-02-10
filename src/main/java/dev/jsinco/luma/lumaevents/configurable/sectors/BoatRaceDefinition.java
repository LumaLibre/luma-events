package dev.jsinco.luma.lumaevents.configurable.sectors;

import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class BoatRaceDefinition extends MinigameDefinition {

    private List<Region> checkpoints = List.of(new Region(), new Region());
    private Location startLocation;
}
