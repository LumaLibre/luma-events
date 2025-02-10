package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class MinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Region region = new Region();


    @Getter
    public static class Region extends OkaeriConfig {
        private Location loc1;
        private Location loc2;
    }
}
