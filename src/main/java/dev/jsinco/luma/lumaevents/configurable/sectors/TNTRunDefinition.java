package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class TNTRunDefinition extends OkaeriConfig {

    private Location lobbyLocation;
    private Location arenaOrigin;

    private int decayDelayTicks = 20;
    private int eliminationHeight = 0;

    private int powerupSpawnAttempts = 12;
    private int powerupMaxAlive = 36;

    private Map<String, Integer> powerupWeights = defaultPowerupWeights();

    private int slowFallingTicks = 140;
    private int jumpSpeedTicks = 140;
    private int platformTicks = 140;
    private double smallUpdraftY = 0.9;
    private double bigUpdraftY = 1.6;
    private int updraftCooldownTicks = 20;

    private String mapSchematic = "tntrun.schem";

    private static Map<String, Integer> defaultPowerupWeights() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("SLOW_FALL", 6);
        map.put("JUMP_SPEED", 5);
        map.put("UPDRAFT_SMALL", 4);
        map.put("PLATFORM", 2);
        map.put("UPDRAFT_BIG", 1);
        return map;
    }

}