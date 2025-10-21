package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.configurable.sectors.BoatRace2Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.MinigameDefinition;
import dev.jsinco.luma.lumaevents.configurable.sectors.Paintball2_1Definition;
import dev.jsinco.luma.lumaevents.configurable.sectors.TowersDefinition;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.Map;

@Getter
@Setter
public class Config extends OkaeriConfig {

    @Comment("Enable or disable automatic minigames")
    private boolean automaticMinigames = false;

    @Comment("Enable or disable job token payouts")
    private boolean jobTokenPayouts = false;

    @Comment("Automatic minigame cooldown in milliseconds")
    private long automaticMinigameCooldown = 7200000L;

    @Comment("Location for /event")
    private Location eventSpawnLocation;

    @Comment("Game drop-off location for minigames")
    private Location gameDropOffLocation;

    @Comment("Minigame definition for 'Paintball 2.1'")
    private Map<String, Paintball2_1Definition> paintballMaps = Map.of(
            "default", new Paintball2_1Definition()
    );

    @Comment("Minigame definition for 'TNT Tag'")
    private Map<String, MinigameDefinition> tntTagMaps = Map.of(
            "default", new MinigameDefinition()
    );

    @Comment("Minigame definition for 'Boatrace 2'")
    private Map<String, BoatRace2Definition> boatRaceMaps = Map.of(
            "default", new BoatRace2Definition()
    );

    private Map<String, TowersDefinition> towersMaps = Map.of(
            "default", new TowersDefinition()
    );
}

