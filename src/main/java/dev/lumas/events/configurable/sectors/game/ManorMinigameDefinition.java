package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class ManorMinigameDefinition {

    private Location spawnLocation;
    @MapTied
    private Location startLocation;
    @MapTied
    private Region region = new Region();
}
