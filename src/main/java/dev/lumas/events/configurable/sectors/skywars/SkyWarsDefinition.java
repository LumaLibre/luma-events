package dev.lumas.events.configurable.sectors.skywars;

import dev.lumas.events.configurable.sectors.Region;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class SkyWarsDefinition extends OkaeriConfig {

    private Location lobby;
    private String map;
    private Location origin;
    @Comment("How far (horizontally) a chest can be from a spawn point and still count as that spawn's island")
    private double islandRadius = 12;
    private Region region = new Region();
    private SkyWarsLoot loot = new SkyWarsLoot();

}
