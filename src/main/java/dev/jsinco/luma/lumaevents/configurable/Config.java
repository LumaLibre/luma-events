package dev.jsinco.luma.lumaevents.configurable;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class Config extends OkaeriConfig {

    private PaintballConfig paintball = new PaintballConfig();
    private Region envoys = new Region();


    @Getter
    public static class PaintballConfig {
        private Region region = new Region();
        private Location spawnPoint;
    }

    @Getter
    public static class Region {
        private Location loc1;
        private Location loc2;
    }
}
