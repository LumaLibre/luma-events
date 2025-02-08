package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.obj.EventTeamType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class Config extends OkaeriConfig {

    private PaintballConfig paintball = new PaintballConfig();
    private Region envoys = new Region();
    @Comment("Don't touch me")
    private EventTeamType lastChosenTeam = EventTeamType.ROSETHORN;


    @Getter
    public static class PaintballConfig extends OkaeriConfig {
        private Region region = new Region();
        private Location spawnPoint;
    }

    @Getter
    public static class Region extends OkaeriConfig {
        private Location loc1;
        private Location loc2;
    }
}
