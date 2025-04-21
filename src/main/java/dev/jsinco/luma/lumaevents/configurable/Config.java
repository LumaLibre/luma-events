package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BunnyArenaDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
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

    @Comment("Default location for /valentide")
    private Location eventSpawnLocation;

    @Comment("Would be /spawn location")
    private Location gameDropOffLocation;

    @Comment("Minigame definition for 'The Nabbits'")
    private TheNabbitsMinigameDefinition theNabbits = new TheNabbitsMinigameDefinition();

    @Comment("Bunny Arena where bunnies with different values spawn")
    private BunnyArenaDefinition bunnyArena = new BunnyArenaDefinition();

    @Comment("Don't touch me")
    private long lastGameLaunchTime = System.currentTimeMillis();
}

