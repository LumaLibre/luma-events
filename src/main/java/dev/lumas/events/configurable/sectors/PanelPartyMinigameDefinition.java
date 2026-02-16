package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class PanelPartyMinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Location center;
    private Region region = new Region();
    private int reachableRadius = 32; // 64x64 blocks
    private String blankPanelSchematic = "panel_blank.schem";
    private List<String> schematics = List.of();
}
