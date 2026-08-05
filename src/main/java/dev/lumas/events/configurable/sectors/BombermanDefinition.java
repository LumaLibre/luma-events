package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class BombermanDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Location arenaOrigin;

}
