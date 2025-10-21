package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class ManorMinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Location startLocation;
    private Region region = new Region();
}
