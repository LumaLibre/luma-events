package dev.lumas.events.configurable.sectors.game.towers;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@Setter
@ConfigSerializable
public class TowersDefinition {

    private Location spawnLocation;
    @MapTied
    private Region region = new Region();
    @MapTied
    private int maxRadius = 250;
    @MapTied
    private Location centerPoint;
    @MapTied
    private Location ivoryCenterPoint;
    @MapTied
    private Location scarletCenterPoint;
    private boolean escalation = false;
    private TowersItems towersItems = new TowersItems();
}
