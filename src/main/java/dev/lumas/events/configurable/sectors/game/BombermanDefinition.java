package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public class BombermanDefinition {

    private Location spawnLocation;
    @MapTied
    private Location arenaOrigin;
    // TODO: needs region for preventing spectators from flying too far out
}
