package dev.lumas.events.configurable.sectors;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import org.bukkit.Location;

import java.util.List;

@Getter
public class MobArenaDefinition extends OkaeriConfig {

    @Comment("Where participants are sent when they join. There is no lobby")
    private Location spawnLocation;
    private Location reviveLocation;


    @Comment("Spawn points used in rotation")
    private List<Location> hotspots = List.of();

    private Region region = new Region();

    private boolean allowEnderDragon = false;

    private int startCountdownSeconds = 10;
    private int intermissionSeconds = 15;
    private int spawnWindowSeconds = 120;
    private int spawnIntervalSeconds = 5;

    private List<Integer> baseQuotaPerWave = List.of(24, 30, 36);

    private int maxAliveMobs = 90;

    private double hotspotSpawnRadius = 8.0;

    private int staleMobMaxRemaining = 5;

    private double soulPickupRadius = 1.75;

    private double reviveRadius = 3.0;
}
