package dev.lumas.events.configurable.sectors.game.skywars;

import dev.lumas.events.annotation.MapTied;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@Getter
@ConfigSerializable
public class SkyWarsDefinition {

    private Location lobby;
    @MapTied
    private String schematic;
    private Location origin;
    @MapTied
    @Comment("How far (horizontally) a chest can be from a spawn point and still count as that spawn's island")
    private double islandRadius = 15;
    @Comment("Spectators further than this from the origin are teleported back to their island")
    private double maxSpectatorDistance = 700;
    private SkyWarsLoot loot = new SkyWarsLoot();

}
