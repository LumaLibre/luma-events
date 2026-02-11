package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Map;

@Getter
public class MineBattleDefinition extends OkaeriConfig {

    private long timeLimitSeconds = 360;
    private long heartbeatTicks = 5;
    private long maxDistanceLOS = 10;
    private long minDistanceLOS = 2;

    private boolean doPeriodicReveal = false;
    private boolean useWorldBorder = false;

    private Location lobbyLocation;
    private Location arenaOrigin;

    private int arenaHeight = 32;
    private int minRadius = 10;
    private int maxRadius = 32;

    private Map<String, Double> innerPattern = Map.ofEntries(
            Map.entry("deepslate", 80.0),
            Map.entry("cobbled_deepslate", 3.92),
            Map.entry("deepslate_diamond_ore", 0.6),
            Map.entry("deepslate_emerald_ore", 0.6),
            Map.entry("deepslate_lapis_ore", 1.2),
            Map.entry("deepslate_gold_ore", 1.2),
            Map.entry("deepslate_copper_ore", 2.2),
            Map.entry("deepslate_redstone_ore", 2.2),
            Map.entry("deepslate_iron_ore", 3.0),
            Map.entry("deepslate_coal_ore", 5.0),
            Map.entry("ancient_debris", 0.04),
            Map.entry("raw_gold_block", 0.04)
    );

    private Map<String, Double> shellPattern = Map.of(
            "reinforced_deepslate", 100.0
    );

    private Map<String, Double> outerPattern = Map.of(
            "bedrock", 100.0
    );

    private double minPocketSpacing = 12.0;
    private int wallPadding = 3;

}
