package dev.lumas.events.configurable.sectors.game.skywars;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@Getter
@ConfigSerializable
public class SkyWarsDefinition {

    private Location lobby;
    @MapTied
    private String map;
    @MapTied
    private Location origin;
    @MapTied
    @Comment("How far (horizontally) a chest can be from a spawn point and still count as that spawn's island")
    private double islandRadius = 12;
    @MapTied
    private Region region = new Region();
    private SkyWarsLoot loot = new SkyWarsLoot();

}
