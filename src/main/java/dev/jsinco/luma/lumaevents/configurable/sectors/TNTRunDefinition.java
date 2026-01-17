package dev.jsinco.luma.lumaevents.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import org.bukkit.Location;

@Getter
public class TNTRunDefinition extends OkaeriConfig {

    private long timeLimitSeconds = 360;

    private Location lobbyLocation;
    private Location arenaOrigin;

    private int decayDelayTicks = 20;
    private int eliminationHeight = 0;

    private boolean powerupsEnabled = true;
    private int powerupSpawnPeriodTicks = 5;
    private int powerupSpawnAttempts = 65;
    private int powerupMaxAlive = 55;

    private int slowFallingTicks = 140;
    private int jumpSpeedTicks = 140;
    private int platformTicks = 140;
    private double smallUpdraftY = 0.9;
    private double bigUpdraftY = 1.6;
    private int updraftCooldownTicks = 20;

    private String mapSchematic = "tntrun.schem";

}