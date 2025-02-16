package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRaceDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.enums.SerializableMinigame;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class Config extends OkaeriConfig {

    @Comment("Enable or disable job token payouts")
    private boolean jobTokenPayouts = false;

    @Comment("Enable or disable automatic minigames")
    private boolean automaticMinigames = false;

    @Comment("Automatic minigame cooldown in milliseconds")
    private long automaticMinigameCooldown = 7200000L;

    @Comment("Default location for /valentide")
    private Location eventSpawnLocation;

    @Comment("Would be /spawn location")
    private Location gameDropOffLocation;

    @Comment("Paintball")
    private MinigameDefinition paintball = new MinigameDefinition();
    @Comment("Envoys")
    private MinigameDefinition envoys = new MinigameDefinition();
    @Comment("BoatRace")
    private BoatRaceDefinition boatRace = new BoatRaceDefinition();


    @Comment("Don't touch me")
    private EventTeamType lastChosenTeam = EventTeamType.ROSETHORN;
    @Comment("Don't touch me")
    private long lastGameLaunchTime = System.currentTimeMillis();
    @Comment("Don't touch me")
    private SerializableMinigame lastMinigame = SerializableMinigame.ENVOYS;
}
