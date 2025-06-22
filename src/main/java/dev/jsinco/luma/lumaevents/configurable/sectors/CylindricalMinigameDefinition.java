package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class CylindricalMinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private CylinderRegion region = new CylinderRegion();
}
