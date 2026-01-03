package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Map;

@Getter
public class MineBattleDefinition extends OkaeriConfig {

    private long maxDurationMillis;
    private Location lobbyLocation;
    private Location arenaOrigin;
    private int arenaHeight;
    private int minRadius;
    private int maxRadius;
    private Map<String, Double> innerPattern;
    private Map<String, Double> shellPattern;
    private Map<String, Double> outerPattern;
    private double minPocketSpacing;
    private int wallPadding;

}
