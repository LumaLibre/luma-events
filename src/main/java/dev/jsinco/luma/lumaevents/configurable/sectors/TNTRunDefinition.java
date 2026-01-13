package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class TNTRunDefinition extends OkaeriConfig {

    private long timeLimitSeconds = 360;
    private long heartbeatTicks = 5;

    private Location lobbyLocation;
    private Location arenaOrigin;

    private int decayDelayTicks = 20;
    private int eliminationHeight = 0;

    private String mapSchematic;

}