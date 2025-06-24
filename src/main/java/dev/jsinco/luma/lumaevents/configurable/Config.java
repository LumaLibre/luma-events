package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.BunnyArenaDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Region;
import dev.jsinco.luma.lumaevents.configurable.sectors.TheNabbitsMinigameDefinition;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class Config extends OkaeriConfig {

    @Comment("Enable or disable automatic minigames")
    private boolean automaticMinigames = false;

    @Comment("Automatic minigame cooldown in milliseconds")
    private long automaticMinigameCooldown = 7200000L;

    @Comment("Default location for /easter")
    private Location eventSpawnLocation;

    @Comment("Would be /spawn location")
    private Location gameDropOffLocation;

    @Comment("Minigame definition for 'Paintball 2.1'")
    private Paintball2_1Definition paintball = new Paintball2_1Definition();

    @Comment("Minigame definition for 'TnT Tag'")
    private MinigameDefinition tntTag = new MinigameDefinition();

    @Comment("Minigame definition for 'Boatrace 2'")
    private BoatRaceDefinition boatRace = new BoatRaceDefinition();


    // TODO: Should use a separate file
    @Comment("Don't touch me")
    private long lastGameLaunchTime = System.currentTimeMillis();
}

