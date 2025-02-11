package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class Region extends OkaeriConfig {
    private Location loc1;
    private Location loc2;
}
