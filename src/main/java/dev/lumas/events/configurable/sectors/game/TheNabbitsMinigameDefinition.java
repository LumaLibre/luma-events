package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@Getter
@Setter
@ConfigSerializable
public class TheNabbitsMinigameDefinition {

    @MapTied
    private Location spawnLocation;
    @MapTied
    private Region region = new Region();
    @MapTied
    private List<Region> playAreas = List.of(new Region(), new Region());
}
