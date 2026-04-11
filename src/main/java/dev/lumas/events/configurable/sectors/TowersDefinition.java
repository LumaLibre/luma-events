package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class TowersDefinition extends OkaeriConfig {

    private Location spawnLocation;
    private Region region = new Region();
    private int maxRadius = 250;
    private Location centerPoint;
    private Location ivoryCenterPoint;
    private Location scarletCenterPoint;
    private boolean escalation = false;
    private TowersItems towersItems = new TowersItems();
}
