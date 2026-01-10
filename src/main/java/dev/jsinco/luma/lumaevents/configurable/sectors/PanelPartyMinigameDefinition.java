package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class PanelPartyMinigameDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Location center;
    private Region region = new Region();
    private List<String> schematics = List.of();
}
