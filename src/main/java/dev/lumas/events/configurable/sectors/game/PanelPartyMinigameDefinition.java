package dev.lumas.events.configurable.sectors.game;

import dev.lumas.events.annotation.MapTied;
import dev.lumas.events.configurable.sectors.region.Region;
import lombok.Getter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@Getter
@ConfigSerializable
public class PanelPartyMinigameDefinition {

    private Location spawnLocation;
    @MapTied
    private Location center;
    @MapTied
    private Region region = new Region();
    @MapTied
    private int reachableRadius = 32; // 64x64 blocks
    private boolean superSpeed = true;
    private int superSpeedBoost = 255;
    private String blankPanelSchematic = "panel_blank.schem";
    private List<String> schematics = List.of();
}
