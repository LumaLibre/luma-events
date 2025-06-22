package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class CylinderRegion extends OkaeriConfig {

    public Location center;
    public int radius;
    public int height;

}
